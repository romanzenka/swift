<%@ page import="edu.mayo.mprc.daemon.DaemonConnection" %>
<%@ page import="edu.mayo.mprc.daemon.monitor.DaemonStatus" %>
<%@ page import="edu.mayo.mprc.swift.ServletInitialization" %>
<%@ page import="edu.mayo.mprc.swift.SwiftWebContext" %>
<%@ page import="edu.mayo.mprc.swift.resources.SwiftMonitor" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.Map" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <% if (ServletInitialization.redirectToConfig(getServletConfig().getServletContext(), response)) {
        return;
    } %>
    <title>Monitor | <%=SwiftWebContext.getWebUi().getTitle()%>
    </title>
    <link href="common/bootstrap/css/bootstrap.min.css" rel="stylesheet" media="screen">
</head>
<body>
<div class="container">
    <h1>Swift Monitor</h1>
    <table class="table">
        <tr>
            <th>Ok</th>
            <th>Connection</th>
            <th>Last heard from</th>
            <th>Message</th>
        </tr>
        <%
            for (final Map.Entry<DaemonConnection, DaemonStatus> entry : SwiftWebContext.getWebUi().getSwiftMonitor().getMonitoredConnections().entrySet()) {
                final DaemonStatus status = entry.getValue();
                out.print("<tr><td>");
                out.print(status != null ? (status.isOk() && !status.isTooOld(SwiftMonitor.MONITOR_PERIOD_SECONDS)) : "?");
                out.print("</td><td>");
                out.print(entry.getKey().getConnectionName());
                out.print("</td><td>");
                out.print(status != null ? new Date(status.getLastResponse()).toString() : "?");
                out.print("</td><td>");
                out.print(status != null ? status.getMessage() : "?");
                out.print("</td>");
                out.print("</tr>");
            }
        %>
    </table>
</div>
<script src="common/bootstrap/js/jquery_1.9.0.min.js"></script>
<script src="common/bootstrap/js/bootstrap.min.js"></script>
</body>
</html>
