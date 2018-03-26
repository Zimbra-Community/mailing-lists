/*

Copyright (C) 2017  Barry de Graaff

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see http://www.gnu.org/licenses/.

https://myzimbra.com/service/extension/mailinglists

*/

package tk.barrydegraaff.mailinglists;

import com.zimbra.cs.extension.ExtensionHttpHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.FileInputStream;
import java.io.IOException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class MailingLists extends ExtensionHttpHandler {

    /**
     * The path under which the handler is registered for an extension.
     *
     * @return path
     */
    @Override
    public String getPath() {
        return "/mailinglists";
    }

    /**
     * Processes HTTP GET requests.
     *
     * @param req  request message
     * @param resp response message
     * @throws java.io.IOException
     * @throws javax.servlet.ServletException
     */
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        final Map<String, String> paramsMap = new HashMap<String, String>();
        final String db_connect_string = this.getDbConnectionString();
        if (req.getQueryString() != null) {
            String[] params = req.getQueryString().split("&");
            for (String param : params) {
                String[] subParam = param.split("=");
                try {
                    paramsMap.put(subParam[0], uriDecode(subParam[1]));
                } catch (Exception e) {
                    paramsMap.put(subParam[0], "");
                }
            }
        }
        if (paramsMap.get("action") != null) {
            handleAction(resp, paramsMap, db_connect_string);
        } else if (paramsMap.get("confirm") != null) {
            handleConfirmation(resp, paramsMap, db_connect_string);
        } else {
            showPage(resp, db_connect_string, "");
        }
    }

    /**
     * Processes HTTP POST requests.
     *
     * @param req  request message
     * @param resp response message
     * @throws java.io.IOException
     * @throws javax.servlet.ServletException
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        resp.getOutputStream().print("tk.barrydegraaff.mailinglists is installed. HTTP POST method is not supported");
    }

    private String getDbConnectionString() {
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

    private void showPage(HttpServletResponse resp, String db_connect_string, String message) throws IOException, ServletException {
        try {
            Connection connection = DriverManager.getConnection(db_connect_string);
            PreparedStatement queryApp = null;
            ResultSet sqlresult = null;
            if (!connection.isClosed()) {
                queryApp = connection.prepareStatement("select * from page");
                sqlresult = queryApp.executeQuery();
                while (sqlresult.next()) {
                    resp.getOutputStream().print("<!DOCTYPE HTML>\r\n<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"><style>");
                    resp.getOutputStream().print(sqlresult.getString("style"));
                    resp.getOutputStream().print("</style><title>" + sqlresult.getString("title") + "</title>");
                    if (!"".equals(message)) {
                        message = message.concat("<br><br>");
                    }
                    resp.getOutputStream().print("</head><body><div class=\"main\"><div class=\"logo\"></div><h1>" + sqlresult.getString("title") + "</h1><p><b>" + message + "</b>" + sqlresult.getString("body") + "</p>");
                }
                sqlresult.close();
                queryApp = connection.prepareStatement("select * from list_properties where enabled = '1';");
                sqlresult = queryApp.executeQuery();
                while (sqlresult.next()) {
                    resp.getOutputStream().print("<h2>" + sqlresult.getString("list_email") + "</h2>");
                    resp.getOutputStream().print("<span>" + sqlresult.getString("description") + "</span>");
                    resp.getOutputStream().print("<form method=\"GET\" enctype=\"application/x-www-form-urlencoded\" action=\"?\"><input name=\"list_email\" value=\"" + sqlresult.getString("list_email") + "\" type=\"hidden\">\n" +
                            "<input name=\"email\" value=\"\" placeholder=\"your email address\"><select name=\"action\">\n" +
                            "<option value=\"subscribe\">subscribe</option><option value=\"unsubscribe\">unsubscribe</option></select> <input type=\"submit\" class=\"button\"></form>\n<hr>");
                }
                sqlresult.close();
                connection.close();
            }
        } catch (Exception ex) {
            resp.getOutputStream().print("tk.barrydegraaff.mailinglists SQL exception in showPage" + ex.toString());
        }
    }

    private void handleAction(HttpServletResponse resp, Map<String, String> paramsMap, String db_connect_string) throws IOException, ServletException {
        try {
            if (validateEmail(paramsMap.get("list_email")) && validateEmail(paramsMap.get("email")) &&
                    ("subscribe".equals(paramsMap.get("action")) || "unsubscribe".equals(paramsMap.get("action")))) {
                Connection connection = DriverManager.getConnection(db_connect_string);
                if (!connection.isClosed()) {
                    PreparedStatement stmt = connection.prepareStatement("SELECT count(*) from list_properties WHERE (list_email = ? AND enabled = '1')");
                    stmt.setString(1, paramsMap.get("list_email"));
                    final ResultSet resultSet = stmt.executeQuery();
                    int dlEnabled = 0;
                    if (resultSet.next()) {
                        dlEnabled = resultSet.getInt(1);
                    }
                    if (dlEnabled == 1) {
                        stmt = connection.prepareStatement("REPLACE INTO list_actions VALUES (?, ?, ?, false, false, NOW())");
                        stmt.setString(1, paramsMap.get("email"));
                        stmt.setString(2, paramsMap.get("list_email"));
                        stmt.setString(3, paramsMap.get("action"));
                        stmt.executeQuery();

                        stmt = connection.prepareStatement("REPLACE INTO list_confirmations VALUES (?, ?, ?)");
                        stmt.setString(1, paramsMap.get("email"));
                        stmt.setString(2, paramsMap.get("list_email"));
                        stmt.setString(3, UUID.randomUUID().toString());
                        stmt.executeQuery();

                        stmt = connection.prepareStatement("REPLACE INTO mailer VALUES (?)");
                        stmt.setString(1, paramsMap.get("email"));
                        stmt.executeQuery();
                    }
                    connection.close();
                }
                showPage(resp, db_connect_string, "Thanks, a confirmation email is on its way to your inbox. Please be patient.");
            } else {
                showPage(resp, db_connect_string, "Could not understand your request, please try again.");
            }
        } catch (Exception ex) {
            return;
        }
    }

    private void handleConfirmation(HttpServletResponse resp, Map<String, String> paramsMap, String db_connect_string) throws IOException, ServletException {
        try {
            if (validateUUID(paramsMap.get("confirm"))) {
                Connection connection = DriverManager.getConnection(db_connect_string);
                if (!connection.isClosed()) {
                    PreparedStatement stmt = connection.prepareStatement("UPDATE list_confirmations SET confirmation = '1' WHERE confirmation = ?");
                    stmt.setString(1, paramsMap.get("confirm"));
                    stmt.executeQuery();
                    connection.close();
                }
                showPage(resp, db_connect_string, "Thanks for your confirmation.");
            } else {
                showPage(resp, db_connect_string, "Could not understand your request, please try again.");
            }
        } catch (Exception ex) {
            return;
        }
    }

    public static final Pattern VALID_EMAIL_ADDRESS_REGEX =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    public static final Pattern VALID_UUID_REGEX =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", Pattern.CASE_INSENSITIVE);

    public static boolean validateEmail(String str) {
        Matcher matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(str);
        return matcher.find();
    }

    public static boolean validateUUID(String str) {
        Matcher matcher = VALID_UUID_REGEX.matcher(str);
        return matcher.find();
    }

    private String uriDecode(String dirty) {
        try {
            String clean = java.net.URLDecoder.decode(dirty, "UTF-8");
            return clean;
        } catch (Exception ex) {
            return ex.toString();
        }
    }
}
