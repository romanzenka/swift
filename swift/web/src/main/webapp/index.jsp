<%@ page import="edu.mayo.mprc.ReleaseInfoCore" %>
<%@ page import="edu.mayo.mprc.ServletIntialization" %>
<%@ page import="edu.mayo.mprc.swift.SwiftWebContext" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
    <% if (ServletIntialization.redirectToConfig(getServletConfig(), response)) {
        return;
    } %>
    <title>Swift - search using multiple engines</title>
    <!--
    <%=ReleaseInfoCore.buildVersion()%>
    -->
    <link rel="stylesheet" href="/common/topbar.css" media="all">
    <style type="text/css">
        body, tr, th, td {
            font-family: arial, sans-serif;
            font-size: 16px;
        }

        body {
            text-align: center;
            background-color: #fff;
            border: 0;
            margin: 0;
        }

        #content {
            text-align: left;
            width: 780px;
            margin: 0 auto;
        }

        a.button {
            display: block;
            width: 320px;
            height: 80px;
            background-image: url(button.gif);
            text-align: center;
            line-height: 80px;
            float: left;
            margin-right: 10px;
            border: 1px solid black;
            text-decoration: none;
            font-size: 20px;
        }

        a:hover.button {
            background-image: url(button_pressed.gif);
        }

            /* Rewrite the default blue tab background */
        ul.locations li.active-tab {
            background-color: #fff;
        }

        ul.locations li.active-tab a {
            border-bottom-color: #fff;
        }
    </style>
</head>
<body>
<div class="topbar">
    <ul class="locations">
        <li><a href="/start">New search</a></li>
        <li><a href="/report/report.jsp">Existing searches</a></li>
        <li class="active-tab"><a href="/">About Swift</a></li>
        <li><a href="/extras">Extras</a></li>
    </ul>
</div>
<div id="content">
    <% ServletIntialization.initServletConfiguration(getServletConfig()); %>
    <% if (SwiftWebContext.getServletConfig().getUserMessage().messageDefined()) { %>
    <div class="user-message">
        <%=SwiftWebContext.getServletConfig().getUserMessage().getMessage()%>
    </div>
    <% } %>
    <div style="height: 120px; overflow: hidden">
        <h1 style="text-align: center;">
            <img src="/common/logo_swift_32.png" style="vertical-align: middle; margin: 0 auto;"
                 alt="<%=SwiftWebContext.getServletConfig().getTitle()%>"/>
        </h1>
    </div>

    <p>Search multiple tandem mass spec. datafiles using <b>multiple search engines at once</b>: Mascot, Sequest,
        X!Tandem, OMSSA and Myrimatch.</p>

    <h2>Swift inputs</h2>

    <p>Swift accepts <b>one or many raw or mgf files</b>. You can process separate files or entire directories.</p>

    <h2>Swift outputs</h2>

    Swift produces Scaffold 3 reports (.sf3 files). You can view these reports on your own computer, just download and
    install
    the free Scaffold 3 viewer.

    There are several possibilities how to map input files to Scaffold reports. You can produce the following:

    <ul>
        <li>separate Scaffold report for each input file</li>
        <li>one combined Scaffold report for all input files</li>
        <li>one combined Scaffold where each input is treated as a separate biological sample</li>
        <li>your own custom combination!</li>
    </ul>

    <h2>Try it!</h2>

    <p>Click the buttons below to start using Swift:</p>

    <div class="buttons">
        <a class="button" href="start">Start new search</a>
        <a class="button" href="report/report.jsp">View existing searches</a>
    </div>
</div>
</body>
</html>
		