package tk.barrydegraaff.processor;


import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.sun.mail.smtp.*;

import java.io.*;
import java.util.Date;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.util.Properties;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Main {

    public static void main(String[] args) {

        try {
            final String db_connect_string = getDbConnectionString();
            Connection connection = DriverManager.getConnection(db_connect_string);
            if (!connection.isClosed()) {
                //run clean-up
                //remove requests rejected by admin
                PreparedStatement stmt = connection.prepareStatement("DELETE FROM list_actions WHERE `reject` = '1';");
                stmt.executeQuery();
                //remove requests for lists that are no longer enabled
                stmt = connection.prepareStatement("DELETE FROM list_actions WHERE `list_email` NOT IN (SELECT list_email FROM list_properties where `enabled` = '1');");
                stmt.executeQuery();
                //auto-approve requests where admin approval is turned off
                stmt = connection.prepareStatement("UPDATE list_actions SET approved = '1' WHERE `list_email` IN (SELECT list_email FROM list_properties where `enabled` = '1' AND `approval` = '0');");
                stmt.executeQuery();

                //here we add/remove from dl's
                stmt = connection.prepareStatement("select list_actions.email, list_actions.list_email, list_actions.action, list_actions.approved, list_actions.reject, list_confirmations.confirmation from list_actions inner join list_confirmations on list_actions.email = list_confirmations.email and list_actions.list_email = list_confirmations.list_email where list_actions.approved = '1' AND list_confirmations.confirmation = '1';");
                ResultSet sqlresult = null;
                sqlresult = stmt.executeQuery();
                PrintWriter out = new PrintWriter("/usr/local/sbin/mailinglistrun.txt");
                while (sqlresult.next()) {
                    if (validate(sqlresult.getString("email")) && validate(sqlresult.getString("list_email")) &&
                            ("subscribe".equals(sqlresult.getString("action")) || "unsubscribe".equals(sqlresult.getString("action")))) {
                        if ("subscribe".equals(sqlresult.getString("action"))) {
                            out.println("adlm " + sqlresult.getString("list_email") + " " + sqlresult.getString("email"));
                            System.out.println("adlm " + sqlresult.getString("list_email") + " " + sqlresult.getString("email"));
                        } else {
                            out.println("rdlm " + sqlresult.getString("list_email") + " " + sqlresult.getString("email"));
                            System.out.println("rdlm " + sqlresult.getString("list_email") + " " + sqlresult.getString("email"));
                        }

                        //remove processed
                        PreparedStatement removeProcessed = connection.prepareStatement("delete from list_actions where email=? and list_email=?");
                        removeProcessed.setString(1, sqlresult.getString("email"));
                        removeProcessed.setString(2, sqlresult.getString("list_email"));
                        removeProcessed.executeQuery();
                    }
                    out.close();
                    sqlresult.close();
                }

                //remove orphaned confirmation codes
                stmt = connection.prepareStatement("delete from list_confirmations where (email,list_email) not in (select email,list_email from list_actions);");
                stmt.executeQuery();

                stmt = connection.prepareStatement("select list_actions.email, list_actions.list_email, list_actions.action, list_actions.approved, list_actions.reject, list_confirmations.confirmation, template.fromEmail, template.subject, template.body from list_actions inner join list_confirmations on list_actions.email = list_confirmations.email and list_actions.list_email = list_confirmations.list_email left outer join template on 1=1 where list_actions.approved = '1' AND list_confirmations.confirmation <> '1' AND list_actions.email IN (select * from mailer);");
                sqlresult = null;
                sqlresult = stmt.executeQuery();
                while (sqlresult.next()) {
                    if (validate(sqlresult.getString("email")) && validate(sqlresult.getString("list_email")) && validate(sqlresult.getString("fromEmail")) &&
                            ("subscribe".equals(sqlresult.getString("action")) || "unsubscribe".equals(sqlresult.getString("action")))) {

                        //See: https://javaee.github.io/javamail/

                        Properties props = System.getProperties();
                        //props.put("mail.smtps.host","smtp.gmail.com");
                        //props.put("mail.smtps.auth","true");
                        Session session = Session.getInstance(props, null);
                        Message msg = new MimeMessage(session);
                        msg.setFrom(new InternetAddress(sqlresult.getString("fromEmail")));
                        ;
                        msg.setRecipients(Message.RecipientType.TO,
                                InternetAddress.parse(sqlresult.getString("email"), false));
                        msg.setSubject(sqlresult.getString("subject"));
                        String body = sqlresult.getString("body");
                        body = body.replace("{user-email}", sqlresult.getString("email"));
                        body = body.replace("{list-email}", sqlresult.getString("list_email"));
                        body = body.replace("{from-email}", sqlresult.getString("fromEmail"));
                        body = body.replace("{confirmation-link}", sqlresult.getString("confirmation"));
                        body = body.replace("{unsubscription}", sqlresult.getString("action"));
                        msg.setText(body);
                        msg.setHeader("X-Mailer", "Zimbra Mailinglists Extension Processor");
                        msg.setSentDate(new Date());
                        //SMTPTransport t = (SMTPTransport)session.getTransport("smtps");
                        SMTPTransport t = (SMTPTransport) session.getTransport("smtp");
                        //t.connect("smtp.gmail.com", "admin@tovare.com", "<insert password here>");
                        t.connect("localhost", "", "");
                        t.sendMessage(msg, msg.getAllRecipients());
                        System.out.println("Response: " + t.getLastServerResponse());
                        t.close();
                        //remove processed
                        PreparedStatement removeProcessed = connection.prepareStatement("delete from mailer where email=?");
                        removeProcessed.setString(1, sqlresult.getString("email"));
                        removeProcessed.executeQuery();
                    }
                }
                sqlresult.close();
                connection.close();
            }
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    private static String getDbConnectionString() {
        Properties prop = new Properties();
        try {
            FileInputStream input = new FileInputStream("/opt/zimbra/lib/ext/mailinglists/db.properties");
            prop.load(input);
            input.close();
            return prop.getProperty("db_connect_string");
        } catch (IOException ex) {
            ex.printStackTrace();
            return "";
        }

    }

    public static final Pattern VALID_EMAIL_ADDRESS_REGEX =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

    public static boolean validate(String emailStr) {
        Matcher matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(emailStr);
        return matcher.find();
    }
}
