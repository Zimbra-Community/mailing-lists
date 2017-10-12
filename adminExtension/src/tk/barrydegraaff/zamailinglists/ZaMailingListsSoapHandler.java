/*

Copyright (C) 2014-2017 Barry de Graaff

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

*/
package tk.barrydegraaff.zamailinglists;

import java.util.Iterator;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.ZimbraSoapContext;
import org.json.JSONArray;
import org.json.JSONArray;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.io.FileInputStream;
import java.util.Properties;
import java.io.IOException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;


public class ZaMailingListsSoapHandler extends DocumentHandler {
    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException {
        try {

            ZimbraSoapContext zsc = getZimbraSoapContext(context);
            Element response = zsc.createElement(
                    "ZaMailingListsResponse"
            );
            final String db_connect_string = this.getDbConnectionString();

            switch (request.getAttribute("action")) {
                case "getLists":
                    return getLists(response, db_connect_string);
                case "saveLists":
                    return saveLists(request, response, db_connect_string);
                case "getPending":
                    return getPending(response, db_connect_string);
                case "savePending":
                    return savePending(request, response, db_connect_string);
            }
            return response;

        } catch (
                Exception e)

        {
            throw ServiceException.FAILURE("ZaMailingListsSoapHandler ServiceException ", e);
        }

    }

    public static final Pattern VALID_EMAIL_ADDRESS_REGEX =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

    public static boolean validate(String emailStr) {
        Matcher matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(emailStr);
        return matcher.find();
    }

    private Element getLists(Element response, String db_connect_string) throws IOException {
        Element mailinglistsResult = response.addUniqueElement("mailinglistsResult");
        try {
            Connection connection = DriverManager.getConnection(db_connect_string);
            PreparedStatement queryApp = null;
            ResultSet sqlresult = null;

            if (!connection.isClosed()) {
                queryApp = connection.prepareStatement("select * from list_properties");
                sqlresult = queryApp.executeQuery();
                while (sqlresult.next()) {
                    Element list = mailinglistsResult.addNonUniqueElement("list");
                    list.addAttribute("list_email", sqlresult.getString("list_email"));
                    list.addAttribute("enabled", sqlresult.getString("enabled"));
                    list.addAttribute("approval", sqlresult.getString("approval"));
                    list.addAttribute("description", sqlresult.getString("description"));
                }
                queryApp = connection.prepareStatement("select * from page");
                sqlresult = queryApp.executeQuery();
                while (sqlresult.next()) {
                    Element list = mailinglistsResult.addUniqueElement("page");
                    list.addAttribute("title", sqlresult.getString("title"));
                    list.addAttribute("style", sqlresult.getString("style"));
                    list.addAttribute("body", sqlresult.getString("body"));
                }

                queryApp = connection.prepareStatement("select * from template");
                sqlresult = queryApp.executeQuery();
                while (sqlresult.next()) {
                    Element list = mailinglistsResult.addUniqueElement("template");
                    list.addAttribute("from", sqlresult.getString("fromEmail"));
                    list.addAttribute("subject", sqlresult.getString("subject"));
                    list.addAttribute("body", sqlresult.getString("body"));
                }

                sqlresult.close();
                connection.close();
            }
        } catch (Exception ex) {
            mailinglistsResult.setText("tk.barrydegraaff.mailinglists SQL exception in getLists" + ex.toString());

        }
        return mailinglistsResult;
    }

    private Element saveLists(Element request, Element response, String db_connect_string) throws IOException {
        Element mailinglistsResult = response.addUniqueElement("mailinglistsResult");
        try {
            JSONArray listsData = new JSONArray(request.getAttribute("listsData"));
            Integer paramCount = 1;

            Connection connection = DriverManager.getConnection(db_connect_string);
            PreparedStatement stmt = connection.prepareStatement("REPLACE INTO list_properties VALUES (?, ?, ?, ?)");
            if (!connection.isClosed()) {
                for (int i = 0; i < listsData.length(); i++) {
                    stmt.setString(paramCount, listsData.getString(i));
                    if (paramCount.equals(4)) {
                        stmt.executeQuery();
                        paramCount = 1;
                    } else {
                        paramCount++;
                    }
                }
            }

            JSONArray pageData = new JSONArray(request.getAttribute("pageData"));
            paramCount = 1;

            stmt = connection.prepareStatement("UPDATE page SET title=?, style=?, body=?");
            if (!connection.isClosed()) {
                for (int i = 0; i < pageData.length(); i++) {
                    stmt.setString(paramCount, pageData.getString(i));
                    if (paramCount.equals(3)) {
                        stmt.executeQuery();
                        paramCount = 1;
                    } else {
                        paramCount++;
                    }
                }
            }

            JSONArray templateData = new JSONArray(request.getAttribute("templateData"));
            paramCount = 1;

            stmt = connection.prepareStatement("UPDATE template SET fromEmail=?, subject=?, body=?");
            if (!connection.isClosed()) {
                for (int i = 0; i < templateData.length(); i++) {
                    stmt.setString(paramCount, templateData.getString(i));
                    if (paramCount.equals(3)) {
                        stmt.executeQuery();
                        paramCount = 1;
                    } else {
                        paramCount++;
                    }
                }
            }

            connection.close();

            mailinglistsResult.setText("Saved");
        } catch (Exception ex) {
            mailinglistsResult.setText("tk.barrydegraaff.mailinglists exception in saveLists" + ex.toString());

        }
        return mailinglistsResult;
    }

    private Element getPending(Element response, String db_connect_string) throws IOException {
        Element mailinglistsResult = response.addUniqueElement("mailinglistsResult");
        try {
            Connection connection = DriverManager.getConnection(db_connect_string);
            PreparedStatement queryApp = null;
            ResultSet sqlresult = null;

            if (!connection.isClosed()) {
                queryApp = connection.prepareStatement("select list_actions.email, list_actions.list_email, list_actions.action, list_actions.approved, list_actions.reject, list_confirmations.confirmation from list_actions inner join list_confirmations on list_actions.email = list_confirmations.email and list_actions.list_email = list_confirmations.list_email;");
                sqlresult = queryApp.executeQuery();
                while (sqlresult.next()) {
                    Element list = mailinglistsResult.addNonUniqueElement("action");
                    list.addAttribute("email", sqlresult.getString("email"));
                    list.addAttribute("list_email", sqlresult.getString("list_email"));
                    list.addAttribute("action", sqlresult.getString("action"));
                    list.addAttribute("confirmation", sqlresult.getString("confirmation"));
                    list.addAttribute("approved", sqlresult.getString("approved"));
                    list.addAttribute("reject", sqlresult.getString("reject"));
                }
                sqlresult.close();
                connection.close();
            }
        } catch (Exception ex) {
            mailinglistsResult.setText("tk.barrydegraaff.mailinglists SQL exception in getLists" + ex.toString());

        }
        return mailinglistsResult;
    }

    private Element savePending(Element request, Element response, String db_connect_string) throws IOException {
        Element mailinglistsResult = response.addUniqueElement("mailinglistsResult");
        try {
            JSONArray listsData = new JSONArray(request.getAttribute("listsData"));
            Integer paramCount = 1;

            Connection connection = DriverManager.getConnection(db_connect_string);
            PreparedStatement stmt = connection.prepareStatement("UPDATE list_actions SET email=?, list_email=?, action=?, approved=?, reject=?, time=NOW() WHERE email=? AND list_email=?");
            if (!connection.isClosed()) {
                String param1 = "", param2 = "";
                for (int i = 0; i < listsData.length(); i++) {
                    stmt.setString(paramCount, listsData.getString(i));
                    if(paramCount==1) {
                        param1 = listsData.getString(i);
                    }
                    if(paramCount==2) {
                        param2 = listsData.getString(i);
                    }
                    if (paramCount.equals(5)) {
                        stmt.setString(6, param1);
                        stmt.setString(7, param2);
                        stmt.executeQuery();
                        paramCount = 1;
                    } else {
                        paramCount++;
                    }
                }
            }
            connection.close();

            mailinglistsResult.setText("Saved");
        } catch (Exception ex) {
            mailinglistsResult.setText("tk.barrydegraaff.mailinglists exception in saveLists" + ex.toString());

        }
        return mailinglistsResult;
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

}
