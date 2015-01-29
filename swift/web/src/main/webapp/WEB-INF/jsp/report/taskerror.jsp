<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head><title>Task Error Report | ${title}</title></head>
<body>
<h1>Task Error</h1>
<table border="2" cellspacing="1" cellpadding="5">
    <tr>
        <th valign="top">Task name</th>
        <td>${taskName}</td>
    </tr>
    <tr>
        <th valign="top">Task description</th>
        <td>${taskDescription}</td>
    </tr>
    <tr>
        <th valign="top">Error message</th>
        <td>${errorMessage}</td>
    </tr>
    <tr>
        <th valign="top">Exception</th>
        <td>
            <pre>${exception}</pre>
        </td>
    </tr>
</table>
</body>
</html>
