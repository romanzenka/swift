<%@ page import="edu.mayo.mprc.MprcException" %>
<%@ page import="edu.mayo.mprc.swift.ReportUtils" %>
<%@ page import="edu.mayo.mprc.swift.SwiftWebContext" %>
<%@ page import="edu.mayo.mprc.swift.db.DatabaseFileTokenFactory" %>
<%@ page import="edu.mayo.mprc.swift.dbmapping.SearchRun" %>
<%@ page import="edu.mayo.mprc.swift.dbmapping.TaskData" %>
<%@ page import="edu.mayo.mprc.utilities.StringUtilities" %>
<%@ page import="java.io.File" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head><title>Swift Task Error Report</title></head>
<body>
<%
    final String browseWebRoot = SwiftWebContext.getWebUi().getBrowseWebRoot();
    final File browseRoot = SwiftWebContext.getWebUi().getBrowseRoot();
    final String taskIdString = request.getParameter("id");
    final String searchRunIdString = request.getParameter("tid");
    final DatabaseFileTokenFactory tokenFactory = SwiftWebContext.getWebUi().getFileTokenFactory();
    if (taskIdString == null && searchRunIdString == null) {
%>
<h1>Error</h1>
Neither task nor transaction ids were specified
<%
        return;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // TASK page
    if (taskIdString != null) {
        int taskid = 0;
        try {
            taskid = Integer.valueOf(taskIdString);
        } catch (NumberFormatException e) {
%>
<h1>Error</h1>
Task id is not valid: <%=taskIdString%><br/>

<h2>Exception thrown</h2>
<% final String message = StringUtilities.escapeHtml(e.getMessage()); %>
<%
        return;
    }

    SwiftWebContext.getWebUi().getSwiftDao().begin();
    try {
        final TaskData data = SwiftWebContext.getWebUi().getSwiftDao().getTaskData(taskid);
%>
<h1>Task Error</h1>
<table border="2" cellspacing="1" cellpadding="5">
    <tr>
        <th valign="top">Task name</th>
        <td><%=ReportUtils.replaceTokensWithHyperlinks(data.getTaskName(), browseRoot, browseWebRoot, tokenFactory)%>
        </td>
    </tr>
    <tr>
        <th valign="top">Task description</th>
        <td><%=ReportUtils.replaceTokensWithHyperlinks(data.getDescriptionLong(), browseRoot, browseWebRoot, tokenFactory)%>
        </td>
    </tr>
    <tr>
        <th valign="top">Error message</th>
        <td><%=ReportUtils.newlineToBr(ReportUtils.replaceTokensWithHyperlinks(data.getErrorMessage(), browseRoot, browseWebRoot, tokenFactory))%>
        </td>
    </tr>
    <tr>
        <th valign="top">Exception</th>
        <td>
            <pre><%=StringUtilities.escapeHtml(data.getExceptionString())%></pre>
        </td>
    </tr>
</table>
<%
        SwiftWebContext.getWebUi().getSwiftDao().commit();
    } catch (Exception e) {
        SwiftWebContext.getWebUi().getSwiftDao().rollback();
        throw new MprcException(e);
    }
} else {
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // TRANSACTION PAGE
    int transactionid = 0;
    try {
        transactionid = Integer.parseInt(searchRunIdString);
    } catch (NumberFormatException e) {
%>
<h1>Error</h1>
Transaction id is not valid: <%=searchRunIdString%><br/>

<h2>Exception thrown</h2>
<%=StringUtilities.escapeHtml(e.getMessage())%>
<%
        return;
    }

    SwiftWebContext.getWebUi().getSwiftDao().begin();
    try {
        final SearchRun data = SwiftWebContext.getWebUi().getSwiftDao().getSearchRunForId(transactionid);
%>
<h1>Transaction Error</h1>
<table border="2" cellspacing="1" cellpadding="5">
    <tr>
        <th valign="top">Transaction name</th>
        <td><%=StringUtilities.escapeHtml(data.getTitle())%>
        </td>
    </tr>
    <tr>
        <th valign="top">Exception</th>
        <td><%=ReportUtils.replaceTokensWithHyperlinks(StringUtilities.escapeHtml(data.getErrorMessage()), browseRoot, browseWebRoot, tokenFactory)%>
        </td>
    </tr>
</table>
<%
            SwiftWebContext.getWebUi().getSwiftDao().commit();
        } catch (Exception e) {
            SwiftWebContext.getWebUi().getSwiftDao().rollback();
            throw new MprcException(e);
        }
    } %>
</body>
</html>
