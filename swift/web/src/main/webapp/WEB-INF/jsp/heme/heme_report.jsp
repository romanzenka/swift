<%@ taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="sw" uri="http://swift.mayo.edu/swift.tld" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Report | ${title}
    </title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="/common/bootstrap/css/bootstrap.min.css" rel="stylesheet" media="screen">
    <script type="text/javascript" src="/common/bootstrap/js/jquery_1.9.0.min.js"></script>
    <script type="text/javascript" src="/common/bootstrap/js/bootstrap.js"></script>
    <script type="text/javascript" src="/common/bootstrap/js/tablesorter.js"></script>
</head>
<body>
<style>
    .monoText {
        font-family: "Courier New", Courier, monospace;
        font-size: 16pt;
    }
</style>

<div class="container">
    <div class="navbar navbar-your-class navbar-static-top">
        <div class="navbar-inner">
            <a href="/heme" class="btn btn-info pull-right">Return to Reports</a>

            <h1>Report: ${report.name}
            </h1>
            Target Mass: ${report.mass} &#177; ${report.massTolerance}<br/>
            <small style="margin-left:20px;">Date: ${reportDate} </small>
        </div>
    </div>


    <h3>Hemoglobin Mutant Peptides</h3>

    <blockquote class="muted">
        These protein groups contain at least one protein that matches the requested intact mass within given
        tolerance.</br>
        If there are multiple proteins listed in a cell, it is because we could not distinguish between them based on
        the peptide evidence itself.
    </blockquote>

    <table id="confirmed" class="table table-striped">
        <thead>
        <tr>
            <th>Protein <tags:arrows/></th>
            <th>Peptides <tags:arrows/></th>
            <th>Spectra <tags:arrows/></th>
            <th>Description <tags:arrows/></th>
            <th>Mass <tags:arrows/></th>
            <th></th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="p" items="${confirmedList}">
            <tr>
                <td>${p.accNum}</td>
                <td>${fn:length(p.peptides)}</td>
                <td style="text-align:center;">${p.totalSpectra}</td>
                <td>${p.neatDescription}</td>
                <td>${p.mass}</td>
                <td><a href="#modal_${p.accNum}" role="button" class="btn" data-toggle="modal">View</a></td>
            </tr>
        </c:forEach>
        </tbody>
    </table>


    <h3>Related Mutant Proteins</h3>
    <blockquote class="muted">
        <p>These are proteins of interest whose mass delta did not match the query.</p>
    </blockquote>


    <table id="related" class="table table-striped">
        <thead>
        <tr>
            <th>Protein <tags:arrows/>
            </th>
            <th>Spectra <tags:arrows/>
            </th>
            <th>Description <tags:arrows/>
            </th>
            <th>Mass <tags:arrows/>
            </th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="p" items="${relatedList}">
            <tr>
                <td>${p.accNum}</td>
                <td style="text-align:center;">${p.totalSpectra}</td>
                <td>${p.neatDescription}</td>
                <td>${p.mass}</td>
            </tr>
        </c:forEach>
        </tbody>
    </table>


    <h3>Other Protein Ids</h3>
    <blockquote class="muted">
        <p>These proteins are unrelated or not targetted, but were detected in the sample.</p>
    </blockquote>

    <table id="other" class="table table-striped">
        <thead>
        <tr>
            <th>Protein <tags:arrows/>
            </th>
            <th>Spectra <tags:arrows/>
            </th>
            <th>Description <tags:arrows/>
            </th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="p" items="${otherList}">
            <tr>
                <td><a href="http://www.genome.jp/dbget-bin/www_bget?sp:${p.accNum}">${p.accNum}</a></td>
                <td style="text-align:center;">${p.totalSpectra}</td>
                <td>
                    <small>${p.neatDescription}</small>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>


</div>


<!-- HOME FOR MODALS  -->
<c:forEach var="p" items="${confirmedList}">
    <!-- Modal -->
    <div id="modal_${p.accNum}" class="modal hide fade" tabindex="-1" role="dialog"
         aria-labelledby="myModalLabel"
         aria-hidden="true">
        <div class="modal-header">
            <button type="button" class="close" data-dismiss="modal" aria-hidden="true">×</button>
            <h3 id="myModalLabel">${p.accNum}</h3>
        </div>
        <div class="modal-body">
            <br/>

            <p class="monoText"
               style="font-weight: bold;">${sw:colorizeSequence(p.cigar, p.sequence)}
            </p>
            <c:forEach var="pep" items="${p.peptides}">
                <p class="monoText">${sw:nonBlankSpaces(pep.start)}${pep.sequence}</p>
            </c:forEach>
        </div>
        <div class="modal-footer">
            <button class="btn" data-dismiss="modal" aria-hidden="true">Close</button>
        </div>
    </div>
</c:forEach>

<c:forEach var="p" items="${unsupportedList}">
    <!-- Modal -->
    <div id="modal_${p.accNum}" class="modal hide fade" tabindex="-1" role="dialog" aria-labelledby="myModalLabel"
         aria-hidden="true">
        <div class="modal-header">
            <button type="button" class="close" data-dismiss="modal" aria-hidden="true">×</button>
            <h3 id="myModalLabel">${p.accNum}</h3>
        </div>
        <div class="modal-body">
            </br>
            <p class="monoText"
               style="font-weight: bold;">${sw:colorizeSequence(p.cigar, p.sequence)}
            </p>
            <c:forEach var="pep" items="${p.peptides}">
                <p class="monoText">${sw:nonBlankSpaces(pep.start)}${pep.sequence}
                </p>
            </c:forEach>
        </div>
        <div class="modal-footer">
            <button class="btn" data-dismiss="modal" aria-hidden="true">Close</button>
        </div>
    </div>
</c:forEach>

<script>
    $(function () {
        $("#confirmed").tablesorter();
        $("#related").tablesorter();
        $("#other").tablesorter();
    });
</script>


</body>
</html>