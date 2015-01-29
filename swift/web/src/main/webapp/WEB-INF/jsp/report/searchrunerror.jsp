<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head><title>Task Error Report | ${title}</title></head>
<body>

<h1>Transaction Error</h1>
<table border="2" cellspacing="1" cellpadding="5">
    <tr>
        <th valign="top">Transaction name</th>
        <td>${data.title}</td>
    </tr>
    <tr>
        <th valign="top">Exception</th>
        <td>${exception}</td>
    </tr>
</table>

</body>
</html>
