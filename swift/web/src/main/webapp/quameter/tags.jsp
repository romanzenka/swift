<%@ page import="edu.mayo.mprc.MprcException" %>
<%@ page import="edu.mayo.mprc.config.ResourceConfig" %>
<%@ page import="edu.mayo.mprc.quameterdb.QuameterUi" %>
<%@ page import="edu.mayo.mprc.quameterdb.dao.QuameterAnnotation" %>
<%@ page import="edu.mayo.mprc.swift.MainFactoryContext" %>
<%@ page import="edu.mayo.mprc.swift.SwiftWebContext" %>
<%@ page import="java.io.File" %>
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
    <title>QuaMeter Tags | <%=SwiftWebContext.getWebUi().getTitle()%>
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

    <h3>Tags</h3>
    <table class="table">
        <thead>
        <tr>
            <th>Path</th>
            <th>File name</th>
            <th>Metric</th>
            <th>Tag</th>
        </tr>
        </thead>
        <%
            if (quameterUiConfig != null) {
                final List<QuameterAnnotation> myList;
                final QuameterUi quameterUi = (QuameterUi) MainFactoryContext.getSwiftEnvironment().createResource(quameterUiConfig);
                quameterUi.begin();
                try {
                    myList = quameterUi.getQuameterDao().listAnnotations();

                    for (final QuameterAnnotation qa : myList) {
                        final String metricName = quameterUi.getMetricName(qa.getMetricCode());
                        if (metricName != null) {
                            final File file = qa.getQuameterResult().getSearchResult().getMassSpecSample().getFile();
        %>
        <tr>
            <td><%=file.getParent().replaceAll("/", "/&#8203;").replaceAll("-", "&#8209;")%>
            </td>
            <td><%=file.getName()%>
            </td>
            <td><%=metricName%>
            </td>
            <td><%=qa.getText()%>
            </td>
        </tr>
        <%
                        }
                    }
                    quameterUi.commit();
                } catch (final Exception e) {
                    quameterUi.rollback();
                    throw new MprcException(e);
                }
            }
        %>
    </table>
</div>

</body>
</html>