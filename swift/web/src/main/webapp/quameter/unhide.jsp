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
    <title>QuaMeter Results | <%=SwiftWebContext.getWebUi().getTitle()%>
    </title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="/common/bootstrap/css/bootstrap.min.css" rel="stylesheet" media="screen">
    <link href="css/quameter.css" rel="stylesheet" media="screen">
</head>
<body>
<div class="container-fluid">
    <div class="navbar navbar-fixed-top navbar-inverse">
        <div class="navbar-inner">
            <a href="/quameter" class="brand">QuaMeter Results</a>
        </div>
    </div>

    <h3>Hidden Data Points</h3>
    <ul class="list-unstyled">
        <%
            if (quameterUiConfig != null) {
                List<QuameterResult> myList;
                final QuameterUi quameterUi = (QuameterUi) MainFactoryContext.getSwiftEnvironment().createResource(quameterUiConfig);
                quameterUi.begin();
                try {
                    myList = quameterUi.getQuameterDao().listHiddenResults();

                    for (QuameterResult qr : myList) {
                        out.print("<li>" + qr.getSearchResult().getMassSpecSample().getFile().getAbsolutePath() + " <a href=\"/quameter-unhide/" + qr.getTransaction() + "\">Unhide This</></li>");
                    }

                    quameterUi.commit();

                } catch (Exception e) {
                    quameterUi.rollback();
                    throw new MprcException(e);
                }
            }
        %>
    </ul>
</div>

</body>
</html>