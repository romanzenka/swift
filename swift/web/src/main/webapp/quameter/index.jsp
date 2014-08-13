<%@ page import="edu.mayo.mprc.MprcException" %>
<%@ page import="edu.mayo.mprc.config.ResourceConfig" %>
<%@ page import="edu.mayo.mprc.quameterdb.QuameterUi" %>
<%@ page import="edu.mayo.mprc.swift.MainFactoryContext" %>
<%@ page import="edu.mayo.mprc.swift.SwiftWebContext" %>
<%@ page import="java.io.StringWriter" %>
<!DOCTYPE html>
<% final ResourceConfig quameterUiConfig = MainFactoryContext.getSwiftEnvironment().getSingletonConfig(QuameterUi.Config.class); %>
<html lang="en">
<head>
<title>QuaMeter Results | <%=SwiftWebContext.getWebUi().getTitle()%>
</title>

<meta name="viewport" content="width=device-width, initial-scale=1.0">
<link href="/common/bootstrap/css/bootstrap.min.css" rel="stylesheet" media="screen">
<link href="css/quameter.css" rel="stylesheet" media="screen">

<!-- HTML5 shim, for IE6-8 support of HTML5 elements -->
<!--[if lt IE 9]>
    <script src="/common/bootstrap/js/html5shiv.js"></script>
<![endif]-->

<script type="text/javascript" src="/common/bootstrap/js/jquery_1.9.0.min.js"></script>
<!--Load the AJAX API-->
<script type="text/javascript" src="https://www.google.com/jsapi"></script>
<script>
    // Load the Visualization API and the core package.
    google.load('visualization', '1.0', {'packages': ['corechart']});
</script>
</head>
<body>
<div class="container-fluid">
    <div class="navbar navbar-fixed-top navbar-inverse">
        <div class="navbar-inner">
            <a href="#" class="brand">QuaMeter Results</a>
            <div class="btn-toolbar pull-left">
                <div class="btn-group" id="category-buttons">
                </div>
            </div>
            <div class="btn-toolbar pull-left">
                <div class="btn-group" id="instrument-buttons">
                </div>
            </div>
            <div class="btn-toolbar pull-left">
                <div class="btn-group" id="view-buttons">
                    <div class="btn btn-info" id="compact-button">Details</div>
                </div>
            </div>
            <div class="row-fluid">
                <div class="span12">
                    <span id="icons">
                        <a href="#" id="search-link"><img src="/report/search_edit.gif" style="border: 0"></a>
                        <a href="#" id="qa-link"><img src="/report/search.gif" style="border: 0"></a>
                        <a id="hide-entry"><img src="/report/search_hide.gif" style="border: 0"></a>
                    </span>
                    <span id="selected-path"></span>
                </div>
            </div>
        </div>
    </div>

    <% if (quameterUiConfig != null) { %>  <!-- TODO REMOVE NOT BEFORE COMMIT -->
    <div class="alert">
        <p><strong>Warning</strong> The QuaMeter module is not configured.</p>
        <p>You need to add the QuaMeterUi resource to the
            <code><%= MainFactoryContext.getSwiftEnvironment().getDaemonConfig().getName() %>
            </code> daemon.</p>
    </div>
    <% } else { %>
        <div id="detailedGraphs" style="margin-top: 50px; display: none"></div>
        <div id="simpleGraphs" style="margin-top: 50px"></div>

    <% } %>
</div>


<div id="annotFormDiv" class="annotationDiv">
    <form id="annotForm" action="form2.html" method="post">
        <textarea name="text" cols="50" rows="3" style="margin: 1px"></textarea></br>
        <input type="submit" value="Submit" style="float:right">
    </form>
</div>

<script type="text/javascript" src="/common/bootstrap/js/bootstrap.js"></script>

<!-- Graph Dependancies -->
<script type="text/javascript" src="js/tmp.js"></script> <!-- TODO REMOVE LINE BEFORE COMMIT -->
<script type="text/javascript" src="js/dygraph-combined.js"></script>
<script type="text/javascript" src="js/quameter-definitions.js"></script>
<script type="text/javascript" src="js/quameter.js"></script>
<script type="text/javascript">
    //var graphDataSrvr = <%  // TODO remove commment before commit
    if(quameterUiConfig!=null) {
        final QuameterUi quameterUi = (QuameterUi) MainFactoryContext.getSwiftEnvironment().createResource(quameterUiConfig);
        quameterUi.begin();
        try {
            final StringWriter writer = new StringWriter(10000);
            quameterUi.dataTableJson(writer);
            quameterUi.commit();
            out.print(writer.toString());
        } catch (Exception e) {
            quameterUi.rollback();
            throw new MprcException(e);
        }
    } else { %>
    null
    <% } %>
    // Set a callback to run when the Google Visualization API is loaded.
    google.setOnLoadCallback( initSimpleCharts(graphDataSrvr) );
    $("body").tooltip({ selector: '[data-toggle="tooltip"]' });

</script>
<!--  Help Docs come from: http://massqc.proteomesoftware.com/help/metrics.php  -->
<!-- Need to add troubleshooting? http://massqc.proteomesoftware.com/help/troubleshooting.php -->

</body>
</html>