<%@ page import="edu.mayo.mprc.MprcException" %>
<%@ page import="edu.mayo.mprc.config.ResourceConfig" %>
<%@ page import="edu.mayo.mprc.quameterdb.QuameterUi" %>
<%@ page import="edu.mayo.mprc.quameterdb.dao.QuameterResult" %>
<%@ page import="edu.mayo.mprc.swift.MainFactoryContext" %>
<%@ page import="edu.mayo.mprc.swift.SwiftWebContext" %>
<%@ page import="java.util.List" %>
<%--
  Created by IntelliJ IDEA.
  User: m088378
  Date: 8/22/14
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<% final ResourceConfig quameterUiConfig = MainFactoryContext.getSwiftEnvironment().getSingletonConfig(QuameterUi.Config.class); %>
<html>
<head>
    <title>QuaMeter Unhide | <%=SwiftWebContext.getWebUi().getTitle()%>
    </title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="/common/bootstrap/css/bootstrap.min.css" rel="stylesheet" media="screen">
    <link href="css/quameter.css" rel="stylesheet" media="screen">
</head>
<body>
<div class="container-fluid">
    <div class="navbar navbar-fixed-top navbar-inverse">
        <div class="navbar-inner">
            <a href="/quameter" class="brand">QuaMeter</a>
        </div>
    </div>

    <h3>Hidden Data Points</h3>
    <table class="table">
        <thead>
        <tr>
            <th>Path</th>
            <th>File name</th>
            <th>Reason</th>
        </tr>
        </thead>
        <%
            if (quameterUiConfig != null) {
                List<QuameterResult> myList;
                final QuameterUi quameterUi = (QuameterUi) MainFactoryContext.getSwiftEnvironment().createResource(quameterUiConfig);
                quameterUi.begin();
                try {
                    myList = quameterUi.getQuameterDao().listHiddenResults();

                    for (QuameterResult qr : myList) {
        %>
        <tr>
            <form action="/service/quameter-unhide/<%= qr.getId() %>" method="post">
                <td class="path-small"><%= qr.getSearchResult().getMassSpecSample().getFile().getParentFile().getAbsolutePath()
                        .replaceAll("/", "/&#8203;").replaceAll("-", "&#8209;") %>
                </td>
                <td><%= qr.getSearchResult().getMassSpecSample().getFile().getName().replaceAll("_", "_&#8203;").replaceAll("-", "&#8209;") %>
                </td>
                <td><input type="text" id="reason" name="reason" placeholder="Reason for unhiding"
                           value="<%= qr.getHiddenReason() %>"></td>
                <td><input type="submit" value="Unhide This"></td>
            </form>
        </tr>
        <%
                    }
                    quameterUi.commit();
                } catch (Exception e) {
                    quameterUi.rollback();
                    throw new MprcException(e);
                }
            }
        %>
    </table>
</div>

</body>
</html>