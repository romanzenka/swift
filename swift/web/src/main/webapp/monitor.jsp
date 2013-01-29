<%@ page import="edu.mayo.mprc.ServletIntialization" %>
<%@ page import="edu.mayo.mprc.daemon.DaemonConnection" %>
<%@ page import="edu.mayo.mprc.daemon.DaemonStatus" %>
<%@ page import="edu.mayo.mprc.swift.SwiftMonitor" %>
<%@ page import="edu.mayo.mprc.swift.SwiftWebContext" %>
<%@ page import="java.util.Date" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
    <% if (ServletIntialization.redirectToConfig(getServletConfig(), response)) {
        return;
    } %>
    <title>Swift Monitor</title>
</head>
<body>
<h1>Swift Monitor</h1>
<table>
    <tr>
        <th>Ok</th>
        <th>Connection</th>
        <th>Last heard from</th>
        <th>Message</th>
    </tr>
    <%
        for (final DaemonConnection connection : SwiftWebContext.getServletConfig().getSwiftMonitor().getMonitoredConnections()) {
            final DaemonStatus status = connection.getStatus();
            out.print("<tr><td>");
            out.print(status != null ? (status.isOk() && !status.isTooOld(SwiftMonitor.MONITOR_PERIOD_SECONDS)) : "?");
            out.print("</td><td>");
            out.print(connection.getConnectionName());
            out.print("</td><td>");
            out.print(status != null ? new Date(status.getLastResponse()).toString() : "?");
            out.print("</td><td>");
            out.print(status != null ? status.getMessage() : "?");
            out.print("</td>");
            out.print("</tr>");
        }
    %>
</table>
</body>
</html>
