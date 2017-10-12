/*
Copyright (C) 2014-2017  Barry de Graaff

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
ZaMailinglistsTab = function(parent, entry) {
    if (arguments.length == 0) return;
    ZaTabView.call(this, parent,"ZaMailinglistsTab");
    ZaTabView.call(this, {
        parent:parent,
        iKeyName:"ZaMailinglistsTab",
        contextId:"MAILINGLISTS"
    });
    this.setScrollStyle(Dwt.SCROLL);

    var soapDoc = AjxSoapDoc.create("ZaMailingLists", "urn:ZaMailingLists", null);
    soapDoc.getMethod().setAttribute("action", "getLists");
    var csfeParams = new Object();
    csfeParams.soapDoc = soapDoc;
    csfeParams.asyncMode = true;
    csfeParams.callback = new AjxCallback(ZaMailinglistsTab.prototype.getListsCallback);
    var reqMgrParams = {} ;
    resp = ZaRequestMgr.invoke(csfeParams, reqMgrParams);

    var soapDoc = AjxSoapDoc.create("ZaMailingLists", "urn:ZaMailingLists", null);
    soapDoc.getMethod().setAttribute("action", "getPending");
    var csfeParams = new Object();
    csfeParams.soapDoc = soapDoc;
    csfeParams.asyncMode = true;
    csfeParams.callback = new AjxCallback(ZaMailinglistsTab.prototype.getPendingCallback);
    var reqMgrParams = {} ;
    resp = ZaRequestMgr.invoke(csfeParams, reqMgrParams);

    document.getElementById('ztab__MAILINGLISTS').innerHTML = '<div style="padding-left:10px"><h1>Mailing Lists</h1>' +    
    '<h2>Manage Lists</h2>Here you can configure Distribution Lists to act as Mailing Lists.<br><br><div id="ml_DistLists"></div><br><button id="Mailinglists-btnSavePrefs">Save</button>' +
    '<br><br><hr>' +
    '<h2>Manage Requests &amp; Admin Approval</h2>Here you can manage requests for (un)subscriptions. Requests that have been processed by the server, will be automatically removed from this list.<br><br><div id="ml_pending"></div><br><button id="Mailinglists-btnSavePending">Save / Refresh</button>' +
    '<br><br><hr>' +
    '<h2>Subscription Management Page</h2>Here you can set the title, style and message to show on the <a href="/service/extension/mailinglists" target="_blank">subscription management page</a>.<br><br><div id="ml_page"></div><br><button id="Mailinglists-btnSavePagePrefs">Save</button><br><br><hr>'+
    '<h2>Email Template</h2>Here you can configure the email that is send to users with the confirmation link.<br><br><div id="ml_template"></div><br><button id="Mailinglists-btnSaveTemplate">Save</button><br><br>';

    var btnSavePagePrefs = document.getElementById('Mailinglists-btnSavePagePrefs');
    btnSavePagePrefs.onclick = AjxCallback.simpleClosure(this.btnSavePrefs);
    
    var btnSavePrefs = document.getElementById('Mailinglists-btnSavePrefs');
    btnSavePrefs.onclick = AjxCallback.simpleClosure(this.btnSavePrefs);
    
    var btnSavePending = document.getElementById('Mailinglists-btnSavePending');
    btnSavePending.onclick = AjxCallback.simpleClosure(this.btnSavePending);

    var btnSaveTemplate = document.getElementById('Mailinglists-btnSaveTemplate');
    btnSaveTemplate.onclick = AjxCallback.simpleClosure(this.btnSavePrefs);    
}


ZaMailinglistsTab.prototype = new ZaTabView();
ZaMailinglistsTab.prototype.constructor = ZaMailinglistsTab;

ZaMailinglistsTab.prototype.getTabIcon =
    function () {
        return "ClientUpload" ;
    }

ZaMailinglistsTab.prototype.getTabTitle =
    function () {
        return "Mailing Lists";
    }

ZaMailinglistsTab.prototype.getListsCallback = function (res) {
   result = res.getResponse().Body.mailinglistsResult.list;
   var form = "";
   if(result)
   {
      form = "<form id='ml_listsFrm'><table><tr><td>Distribution list</td><td>Enable&nbsp;</td><td>Admin approval&nbsp;</td><td>Description</td></tr>";
      for(var x=0; x < result.length; x++){
         form += '<tr><td><input readonly type="text" value="'+result[x].list_email+'"></td>';
         form += '<td><input type="checkbox" '+(result[x].enabled == 1 ? 'checked' : '')+'></td>';
         form += '<td><input type="checkbox" '+(result[x].approval == 1 ? 'checked' : '')+'></td>';
         form += '<td><input type="text" placeholder="description" value="'+(result[x].description ? result[x].description : "")+'"></td>';
         form += '</tr>';
      }
      form +=  '</table></form>';
      document.getElementById('ml_DistLists').innerHTML = form;
   }

   result = res.getResponse().Body.mailinglistsResult.page;
   form = "<form id='ml_pageFrm'><table>";
      form += '<tr><td>Title:</td><td><input id="ml_pageFrm_title" type="text" value="'+result.title+'"></td></tr>';
      form += '<tr><td>Style:</td><td><textarea id="ml_pageFrm_style" rows="10" cols="100">'+result.style+'</textarea></td></tr>';
      form += '<tr><td>Body:</td><td><textarea id="ml_pageFrm_body" rows="10" cols="100">'+result.body+'</textarea></td></tr>';
      form += '</table>';
   
   form +=  '</form>';
   document.getElementById('ml_page').innerHTML = form;

   result = res.getResponse().Body.mailinglistsResult.template;
   form = "<form id='ml_templateFrm'><table>";
      form += '<tr><td>From address:</td><td><input id="ml_templateFrm_from" type="text" value="'+result.from+'"></td></tr>';
      form += '<tr><td>Subject:</td><td><input id="ml_templateFrm_subject" type="text" value="'+result.subject+'"></td></tr>';
      form += '<tr><td>Body:</td><td><textarea id="ml_templateFrm_body" rows="10" cols="100">'+result.body+'</textarea></td></tr>';
      form += '</table>';
   
   form +=  '</form>';
   document.getElementById('ml_template').innerHTML = form;
   return;
}

ZaMailinglistsTab.prototype.btnSavePrefs = function () {
    if(document.getElementById('ml_listsFrm'))
    {
       var elements = document.getElementById('ml_listsFrm').elements;
       var form = [];
       for(var x=0; x < elements.length; x++)
       {
          if(elements[x].type == "checkbox")
          {
             if(elements[x].checked === false)
             {
                form[x] = "0";
             }
             else
             {
                form[x] = "1";
             }       
          }
          else
          {
             form[x] = elements[x].value;
          }
       }
       var soapDoc = AjxSoapDoc.create("ZaMailingLists", "urn:ZaMailingLists", null);
       soapDoc.getMethod().setAttribute("action", "saveLists");
       soapDoc.getMethod().setAttribute("listsData", JSON.stringify(form));
   
       form = [];
       form[0] = document.getElementById('ml_pageFrm_title').value;
       form[1] = document.getElementById('ml_pageFrm_style').value;
       form[2] = document.getElementById('ml_pageFrm_body').value;
       soapDoc.getMethod().setAttribute("pageData", JSON.stringify(form));    

       form = [];
       form[0] = document.getElementById('ml_templateFrm_from').value;
       form[1] = document.getElementById('ml_templateFrm_subject').value;
       form[2] = document.getElementById('ml_templateFrm_body').value;
       soapDoc.getMethod().setAttribute("templateData", JSON.stringify(form));    
       
       var csfeParams = new Object();
       csfeParams.soapDoc = soapDoc;
       csfeParams.asyncMode = true;
       csfeParams.callback = new AjxCallback(ZaMailinglistsTab.prototype.mailinglistsDefaultCallback);
       var reqMgrParams = {} ;
       resp = ZaRequestMgr.invoke(csfeParams, reqMgrParams);
    }
}    

ZaMailinglistsTab.prototype.getPendingCallback = function (res) {
   result = res.getResponse().Body.mailinglistsResult.action;
   if(!result)
   {
      document.getElementById('ml_pending').innerHTML = "";
      return;
   }
   var form = "<form id='ml_pendingFrm'><table><tr><td>User Email&nbsp;</td><td>List&nbsp;</td><td>(Un)Subscribe&nbsp;</td><td>User Confirmed&nbsp;</td><td>Admin Approved&nbsp;</td><td>Reject</td></tr>";
   for(var x=0; x < result.length; x++){
      form += '<tr><td><input readonly type="text" value="'+result[x].email+'">&nbsp;</td>';
      form += '<td><input readonly type="text" value="'+result[x].list_email+'">&nbsp;</td>';
      form += '<td><input readonly type="text" value="'+result[x].action+'">&nbsp;</td>';      
      form += '<td>';
      if(result[x].confirmation == 1)
      {
          form += 'confirmed';
      }
      else
      {
         form += '<a href="/service/extension/mailinglists?confirm='+result[x].confirmation+'" target="_blank" onclick="document.getElementById(\'ml_confirmed\').style.display=\'inline\';this.remove()">confirm</a><div id="ml_confirmed" style="display:none">confirmed</div>';
      }
      form += '&nbsp;</td>';
      form += '<td><input type="checkbox"'+(result[x].approved == 1 ? 'checked' : '')+'></td>';
      form += '<td><input type="checkbox"'+(result[x].reject == 1 ? 'checked' : '')+'></td>';
      form += '</tr>';
   }
   form +=  '</table></form>';
   document.getElementById('ml_pending').innerHTML = form;
   return;
}

ZaMailinglistsTab.prototype.btnSavePending = function () {
   if(!document.getElementById('ml_pendingFrm'))
   {
       var soapDoc = AjxSoapDoc.create("ZaMailingLists", "urn:ZaMailingLists", null);
       soapDoc.getMethod().setAttribute("action", "getPending");
       var csfeParams = new Object();
       csfeParams.soapDoc = soapDoc;
       csfeParams.asyncMode = true;
       csfeParams.callback = new AjxCallback(ZaMailinglistsTab.prototype.getPendingCallback);
       var reqMgrParams = {} ;
       resp = ZaRequestMgr.invoke(csfeParams, reqMgrParams);
       return;
    }
    var elements = document.getElementById('ml_pendingFrm').elements;
    var form = [];
    for(var x=0; x < elements.length; x++)
    {
       if(elements[x].type == "checkbox")
       {
          if(elements[x].checked === false)
          {
             form[x] = "0";
          }
          else
          {
             form[x] = "1";
          }       
       }
       else
       {
          form[x] = elements[x].value;
       }
    }
    var soapDoc = AjxSoapDoc.create("ZaMailingLists", "urn:ZaMailingLists", null);
    soapDoc.getMethod().setAttribute("action", "savePending");
    soapDoc.getMethod().setAttribute("listsData", JSON.stringify(form));
    
    var csfeParams = new Object();
    csfeParams.soapDoc = soapDoc;
    csfeParams.asyncMode = true;
    csfeParams.callback = new AjxCallback(ZaMailinglistsTab.prototype.mailinglistsDefaultCallback);
    var reqMgrParams = {} ;
    resp = ZaRequestMgr.invoke(csfeParams, reqMgrParams);
} 
  
ZaMailinglistsTab.prototype.mailinglistsDefaultCallback = function (result) {
   ZaMailinglistsTab.prototype.status('Ready. '+ result.getResponse().Body.mailinglistsResult._content);

   var soapDoc = AjxSoapDoc.create("ZaMailingLists", "urn:ZaMailingLists", null);
   soapDoc.getMethod().setAttribute("action", "getLists");
   var csfeParams = new Object();
   csfeParams.soapDoc = soapDoc;
   csfeParams.asyncMode = true;
   csfeParams.callback = new AjxCallback(ZaMailinglistsTab.prototype.getListsCallback);
   var reqMgrParams = {} ;
   resp = ZaRequestMgr.invoke(csfeParams, reqMgrParams);
   
   var soapDoc = AjxSoapDoc.create("ZaMailingLists", "urn:ZaMailingLists", null);
   soapDoc.getMethod().setAttribute("action", "getPending");
   var csfeParams = new Object();
   csfeParams.soapDoc = soapDoc;
   csfeParams.asyncMode = true;
   csfeParams.callback = new AjxCallback(ZaMailinglistsTab.prototype.getPendingCallback);
   var reqMgrParams = {} ;
   resp = ZaRequestMgr.invoke(csfeParams, reqMgrParams);
}  

ZaMailinglistsTab.prototype.status = function (statusText) {
   ZaApp.getInstance().getAppCtxt().getAppController().setActionStatusMsg(statusText);
}
