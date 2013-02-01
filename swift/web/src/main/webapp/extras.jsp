<%@ page import="org.joda.time.DateTime" %>
<%@ page import="org.joda.time.format.DateTimeFormat" %>
<!DOCTYPE HTML>
<html lang="en">
<head>
    <title>Swift Extras</title>
    <link href="common/bootstrap/css/bootstrap.min.css" rel="stylesheet" media="screen">
</head>
<body>
<div class="container">
    <h1>Swift Extras</h1>

    <h3>Configuration</h3>

    <p>
        Change Swift settings. Warning - when saving the configuration, Swift restarts.
    </p>

    <p>
        <a class="btn" href="/configuration">Configuration</a>
    </p>

    <p>
        Preview the settings Swift uses internall when sending searches to search engines.
    </p>

    <p>
        <a class="btn" href="/amino-acids">Defined Amino Acids</a>
        <a class="btn" href="/mods">Defined Modifications</a>
    </p>


    <h3>Monitoring</h3>

    <p>
        To be notified of successfully finished searches or errors, subscribe to Swift's RSS feeds:
    </p>

    <p>
        <a class="btn" href="/feed">RSS Feed</a>
        <a class="btn" href="/feed?showSuccess=false">RSS Feed (Errors Only)</a>
        <a class="btn" href="/iphone">iPhone interface</a>
    </p>

    <h3>Queries</h3>

    <p>
        Search for all searches that contain a given protein accession number.
    </p>

    <p>
        <a class="btn" href="/find-protein"><i class="icon-search"></i> Find proteins</a>
    </p>

    <p>
        Report time that was spent by Swift over a particular time period.
    </p>
    <%
        DateTime end = new DateTime();
        DateTime start = end.minusMonths(1);
    %>

    <p>

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
    </p>

</div>
<script src="common/bootstrap/js/jquery_1.9.0.min.js"></script>
<script src="common/bootstrap/js/bootstrap.min.js"></script>
</body>
</html>
