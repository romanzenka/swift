<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%--
  Created by IntelliJ IDEA.
  User: m088378
  Date: 8/22/14
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>QuaMeter Tags | ${title}
    </title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="/common/bootstrap/css/bootstrap.min.css" rel="stylesheet" media="screen">
    <link href="/quameter/css/quameter.css" rel="stylesheet" media="screen">
    <script type="text/javascript" src="/common/bootstrap/js/jquery_1.9.0.min.js"></script>
    <script type="text/javascript" src="/common/bootstrap/js/tablesorter.js"></script>
</head>
<body>
<div class="container-fluid">
    <div class="navbar navbar-fixed-top navbar-inverse">
        <div class="navbar-inner">
            <a href="/quameter" class="brand">QuaMeter</a>
        </div>
    </div>

    <h3>Tags</h3>
    <table id="tags" class="table table-hover table-condensed">
        <thead>
        <tr>
            <th>Path</th>
            <th>File name</th>
            <th>Instrument</th>
            <th>Metric</th>
            <th>Tag</th>
        </tr>
        </thead>
        <c:forEach var="tag" items="${tags}">
            <tr>
                <td style="font-size: smaller">${tag.directory}</td>
                <td>${tag.fileName}</td>
                <td>${tag.instrument}</td>
                <td>${tag.metric}</td>
                <td>${tag.tagText}</td>
            </tr>
        </c:forEach>
    </table>

    <script>
        $(function () {
            $("#tags").tablesorter();
        });
    </script>
</div>

</body>
</html>