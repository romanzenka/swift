<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page import="edu.mayo.mprc.swift.ServletInitialization" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <% if (ServletInitialization.redirectToConfig(getServletConfig().getServletContext(), response)) {
        return;
    } %>
    <title>Monitor | ${title}</title>
    <link href="../../common/bootstrap/css/bootstrap.min.css" rel="stylesheet" media="screen">
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
        <c:forEach var="entry" items="${monitoredConnections}">
            <tr>
                <c:choose>
                    <c:when test="${empty entry.value}">
                        <td>?</td>
                        <td>${entry.key.connectionName}</td>
                        <td>?</td>
                        <td>?</td>
                    </c:when>
                    <c:otherwise>
                        <td>${entry.value.recentlyOk}</td>
                        <td>${entry.key.connectionName}</td>
                        <td><fmt:formatDate value="${entry.value.lastResponseDate}" type="both"/></td>
                        <td>${entry.value.message}</td>
                    </c:otherwise>
                </c:choose>
                <td></td>
                <td></td>
                <td></td>
                <td></td>
            </tr>
        </c:forEach>
    </table>
</div>
<script src="../../common/bootstrap/js/jquery_1.9.0.min.js"></script>
<script src="../../common/bootstrap/js/bootstrap.min.js"></script>
</body>
</html>
