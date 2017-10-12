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

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.extension.ZimbraExtension;
import com.zimbra.soap.SoapServlet;


public class ZaMailingListsExt implements ZimbraExtension {
    public void destroy() {
    }

    public String getName() {
        return "ZaMailingListsExt";
    }

    public void init() throws ServiceException {
        SoapServlet.addService("AdminServlet", new ZaMailingListsService());
    }

}


