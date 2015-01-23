<%@ page import="edu.mayo.mprc.swift.ServletInitialization" %>
<%@ page import="edu.mayo.mprc.swift.SwiftWebContext" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <% if (ServletInitialization.redirectToConfig(getServletConfig().getServletContext(), response)) {
        return;
    } %>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><sitemesh:write property='title'/> | <%=SwiftWebContext.getWebUi().getTitle()%>
    </title>
    <link href="/common/bootstrap/css/bootstrap.min.css" rel="stylesheet" media="screen">
    <script src="/common/bootstrap/js/jquery_1.9.0.min.js"></script>
    <script type="text/javascript" src="/common/bootstrap/js/jquery.tmpl.1.1.1.js"></script>
    <script src="/common/bootstrap/js/bootstrap.min.js"></script>

    <!-- HTML5 shim, for IE6-8 support of HTML5 elements -->
    <!--[if lt IE 9]>
    <script src="/common/bootstrap/js/html5shiv.js"></script>
    <![endif]-->
    <style>
        body {
            padding-top: 60px; /* 60px to make the container go all the way to the bottom of the topbar */
        }

        .user-message {
            border: 1px solid #f00;
            background-color: #fee;
            padding: 10px;
            margin: 5px;
        }

    </style>
    <sitemesh:write property="head"/>

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
                    <li><a href="/quameter">QuaMeter</a></li>
                    <!-- TODO - make optional -->
                    <li><a href="/extras">Extras</a></li>
                </ul>
            </div>
            <!--/.nav-collapse -->
        </div>
    </div>
</div>
<div class="container">
    <sitemesh:write property="body"/>
</div>
</body>
</html>
