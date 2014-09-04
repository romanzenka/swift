<%@ page import="edu.mayo.mprc.ReleaseInfoCore" %>
<%@ page import="edu.mayo.mprc.swift.ServletInitialization" %>
<%@ page import="edu.mayo.mprc.swift.SwiftWebContext" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
    <% if (ServletInitialization.redirectToConfig(getServletConfig().getServletContext(), response)) {
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

        .logo {
            font-family: Verdana, sans-serif;
            font-weight: 100;
            color: #44aaee;
            font-size: 60px;
        }

            /* CSS code */

        .reflected {
            position: relative;
        }

        .reflected:before, .reflected:after {
            display: block;
            position: absolute;
            bottom: -.84em; /* You should change this value to fit your font */
            left: 0;
            right: 0;
        }

        .reflected:before {
            content: '<%=SwiftWebContext.getWebUi().getTitle()%>';
            opacity: .3;
            /* This is how the text is flipped vertically */
            -webkit-transform: scaleY(-1);
            -moz-transform: scaleY(-1);
            -o-transform: scaleY(-1);
        }

        .reflected:after {
            /* Fading using CSS gradient */
            /* Don't forget to change the colors to your background color */
            background: -webkit-gradient(linear, left top, left center, from(rgba(255, 255, 255, 0)), to(rgb(255, 255, 255)));
            background: -moz-linear-gradient(top, rgba(255, 255, 255, 0), rgb(255, 255, 255));
            /* I left out the `filter` property,
               because IE doesn't know `:before` and `:after` pseudo-elements anyway */
            content: ' ';
            height: 1em;
        }
    </style>
</head>
<body>
<div class="topbar">
    <span class="logo-small"><%=SwiftWebContext.getWebUi().getTitle()%></span>
    <ul class="locations">
        <li><a href="/start">New search</a></li>
        <li><a href="/report/report.jsp">Existing searches</a></li>
        <li class="active-tab"><a href="/">About Swift</a></li>
        <li><a href="/extras">Extras</a></li>
    </ul>
</div>
<div id="content">
    <% if (SwiftWebContext.getWebUi().getUserMessage().messageDefined()) { %>
    <div class="user-message">
        <%=SwiftWebContext.getWebUi().getUserMessage().getMessage()%>
    </div>
    <% } %>
    <div style="height: 130px; overflow: hidden">
        <h1 style="text-align: center;" class="logo reflected"><%=SwiftWebContext.getWebUi().getTitle()%>
        </h1>
    </div>

    <p>Search multiple tandem mass spec. datafiles using <b>multiple search engines at once</b>: Mascot, Comet,
        X!Tandem and MyriMatch.</p>

    <h2>Swift inputs</h2>

    <p>Swift accepts <b>one or many raw or mgf files</b>. You can process separate files or entire directories.</p>

    <h2>Swift outputs</h2>

    Swift produces Scaffold reports (.sf3 files). You can view these reports on your own computer, just
    <a href="http://www.proteomesoftware.com/products/free-viewer/">download and
    install the free Scaffold viewer</a>.

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
		