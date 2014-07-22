<%--
  Created by IntelliJ IDEA.
  User: m088378
  Date: 7/21/14
  Time: 3:39 PM
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>
        <%--outputStream.print("Report for patient " + report.getName() + " from " + format.format(report.getDate()));--%>
        HEME REPORT PAGE
    </title>


    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="/common/bootstrap/css/bootstrap.min.css" rel="stylesheet" media="screen">
</head>
<body>

<div class="container">


<h1>" + report.getName() + "</h1>
    <h2>" + format.format(report.getDate()) + "</h2>

    Listing proteins with mass delta of " + report.getMass() + " specified with tolerance of " + report.getMassTolerance()
    <hr/>


    <h3>Matching proteins</h3>

<p>These protein groups contain at least one protein that matches the requested intact mass within given tolerance.</p>
<p>If there are multiple proteins listed in a cell, it is because we could not distinguish between them based on the peptide evidence itself.</p>

    reportEntries(outputStream, report.getWithinRange(), report);


<h3>Related proteins, not matching</h3>
<p>These are proteins of interest whose mass delta did not match the query.</p>

    reportEntries(outputStream, report.getHaveMassDelta(), report);

<h3>Contaminants</h3>
<p>These proteins are unrelated or not targetted, but were detected in the sample.</p>
    reportEntries(outputStream, report.getAllOthers(), report);

</div>
</body>
</html>