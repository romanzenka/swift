<%@ page import="edu.mayo.mprc.ReleaseInfoCore" %>
<%@ page import="edu.mayo.mprc.swift.SwiftWebContext" %>
<%@ page import="edu.mayo.mprc.utilities.StringUtilities" %>
<%@ page import="org.joda.time.DateTime" %>
<%@ page import="org.joda.time.format.DateTimeFormat" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <title>Swift Extras</title>
</head>
<body>
    <h1>Swift Extras</h1>

    <h3>Queries</h3>

    <p>
        Search for all searches that contain a given protein accession number.
    </p>

    <p>
        <a class="btn" href="/find-protein"><i class="icon-search"></i> Find proteins</a>
    </p>

    <h3>Time Report</h3>

    <p>
        Report time that was spent by Swift over a particular time period.
    </p>
    <%
        DateTime end = new DateTime();
        DateTime start = end.minusMonths(1);
    %>

    <form action="/time-report" class="form-inline">
        <label class="control-label" for="start">Start</label>

        <input type="text" name="start" id="start" class="input-small"
               value="<%=DateTimeFormat.forPattern("yyyy-MM-dd").print(start) %>"/>
        <label class="control-label" for="end">&nbsp;End</label>

        <input type="text" name="end" id="end" class="input-small"
               value="<%=DateTimeFormat.forPattern("yyyy-MM-dd").print(end) %>"/>
        <label class="checkbox">
            <input type="checkbox" name="screen" id="screen" value="1" checked="true"/> Display on screen
        </label>
        <button type="submit" class="btn btn-primary" name="submit">Get Report</button>
    </form>

    <h3>Configuration</h3>

    <p>
        Change Swift settings. Warning - when saving the configuration, Swift restarts.
    </p>

    <p>
        <a class="btn" href="/configuration"><i class="icon-pencil"></i> Configuration</a>
    </p>

    <p>
        Preview the settings Swift uses internally when sending searches to search engines.
    </p>

    <p>
        <a class="btn" href="/amino-acids">Defined Amino Acids</a>
        <a class="btn" href="/mods">Defined Modifications</a>
    </p>


    <h3>Monitoring</h3>

    <p>
        To be notified of successfully finished searches or errors, subscribe to Swift's RSS feeds.
        The swift monitor periodically pings Swift daemons to make sure all components are ok.
    </p>

    <p>
        <a class="btn" href="/feed">RSS Feed</a>
        <a class="btn" href="/feed?showSuccess=false">RSS Feed (Errors Only)</a>
    </p>

    <p>
        <a class="btn" href="/monitor.jsp">Component Monitor</a>
    </p>

    <h3>About</h3>

    <table class="table">
        <tr>
            <th>User-set Title</th>
            <td><%=StringUtilities.escapeHtml(SwiftWebContext.getWebUi().getTitle())%>
            </td>
        </tr>
        <tr>
            <th>Build Version</th>
            <td><%=StringUtilities.escapeHtml(ReleaseInfoCore.buildVersion())%>
            </td>
        </tr>
        <tr>
            <th>Build Revision</th>
            <td><%= StringUtilities.escapeHtml(ReleaseInfoCore.buildRevision())%>
            </td>
        </tr>
        <tr>
            <th>Build Timestamp</th>
            <td><%= StringUtilities.escapeHtml(ReleaseInfoCore.buildTimestamp())%>
            </td>
        </tr>
        <tr>
            <th>Build Link</th>
            <td>
                <a href="<%= StringUtilities.escapeHtml(ReleaseInfoCore.buildLink())%>"><%= StringUtilities.escapeHtml(ReleaseInfoCore.buildLink())%>
                </a></td>
        </tr>
    </table>
</body>
</html>
