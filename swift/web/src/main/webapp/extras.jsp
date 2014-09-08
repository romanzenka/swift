<%@ page import="edu.mayo.mprc.ReleaseInfoCore" %>
<%@ page import="edu.mayo.mprc.swift.SwiftWebContext" %>
<%@ page import="edu.mayo.mprc.utilities.StringUtilities" %>
<%@ page import="org.joda.time.DateTime" %>
<%@ page import="org.joda.time.format.DateTimeFormat" %>
<!DOCTYPE HTML>
<html lang="en">
<head>
    <meta charset="utf-8">
    <title>Swift Extras</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="common/bootstrap/css/bootstrap.min.css" rel="stylesheet" media="screen">
    <!-- HTML5 shim, for IE6-8 support of HTML5 elements -->
    <!--[if lt IE 9]>
    <script src="common/bootstrap/js/html5shiv.js"></script>
    <![endif]-->
    <style>
        body {
            padding-top: 60px; /* 60px to make the container go all the way to the bottom of the topbar */
        }
    </style>
</head>
<body>
<div class="navbar navbar-inverse navbar-fixed-top">
    <div class="navbar-inner">
        <div class="container">
            <button type="button" class="btn btn-navbar" data-toggle="collapse" data-target=".nav-collapse">
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
            </button>
            <a class="brand" href="/"><%=SwiftWebContext.getWebUi().getTitle()%>
            </a>

            <div class="nav-collapse collapse">
                <ul class="nav">
                    <li><a href="/start">New Search</a></li>
                    <li><a href="/report/report.jsp">Existing Searches</a></li>
                    <li><a href="/">About Swift</a></li>
                    <li><a href="/quameter">QuaMeter</a></li> <!-- TODO - make optional -->
                    <li class="active"><a href="/extras">Extras</a></li>
                </ul>
            </div>
            <!--/.nav-collapse -->
        </div>
    </div>
</div>
<div class="container">
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

</div>
<script src="common/bootstrap/js/jquery_1.9.0.min.js"></script>
<script src="common/bootstrap/js/bootstrap.min.js"></script>
</body>
</html>
