<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ page pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <title>QuaMeter Unhide | ${title}</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="<spring:url value="/common/bootstrap/css/bootstrap.min.css"/>" rel="stylesheet" media="screen">
    <link href="<spring:url value="/quameter/css/quameter.css?v=${ver}"/>" rel="stylesheet" media="screen">
</head>
<body>
<div class="container-fluid">
    <div class="navbar navbar-fixed-top navbar-inverse">
        <div class="navbar-inner">
            <a href="<spring:url value="/quameter"/>" class="brand">QuaMeter</a>
        </div>
    </div>

    <h3>Hidden Data Points</h3>
    <table class="table">
        <thead>
        <tr>
            <th>Path</th>
            <th>File name</th>
            <th>Reason</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="unhide" items="${unhides}">
            <tr>
                <td class="path-small">${unhide.directory}</td>
                <td>${unhide.fileName}</td>
                <td>
                    <form action="/service/quameter-unhide/${unhide.id}" method="post">
                        <input type="text" id="reason" name="reason" placeholder="Reason for unhiding"
                               value="${unhide.reason}">
                        <input type="submit" value="Unhide This">
                    </form>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</div>

</body>
</html>