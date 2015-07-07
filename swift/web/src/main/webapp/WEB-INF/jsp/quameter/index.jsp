<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <title>QuaMeter | ${title}</title>

    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="/common/bootstrap/css/bootstrap.min.css" rel="stylesheet" media="screen">
    <link href="/quameter/css/quameter.css?v=${ver}" rel="stylesheet" media="screen">

    <!-- HTML5 shim, for IE6-8 support of HTML5 elements -->
    <!--[if lt IE 9]>
    <script src="/common/bootstrap/js/html5shiv.js"></script>
    <![endif]-->

    <script type="text/javascript" src="/common/bootstrap/js/jquery_1.9.0.min.js"></script>
    <script type="text/javascript" src="/quameter/js/jspdf.min.js"></script>
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
            <div class="btn-toolbar pull-right">
                <div class="btn-group" id="annotFormDiv" class="annotationDiv">
                    <form id="annotForm" action="/service/new-annotation" method="post" onsubmit="return false;"
                          class="navbar-form">
                        <div class="form-group">
                            <div class="input-append">
                                <input id="annotationText" name="text" size="50" class="form-control">
                                <button type="submit" class="btn btn-default" id="submitAnnotation" value="Submit">
                                    Submit
                                </button>
                            </div>
                        </div>
                        <input type="hidden" id="hiddenMetricCode" name="metricCode">
                        <input type="hidden" id="hiddenRowid" name="dbId">
                    </form>
                </div>
                <div class="btn-group" id="extra-buttons">
                    <a href="/quameter/unhide" class="btn"><i class="icon-remove"></i> Unhide</a>
                    <a href="/quameter/tags" class="btn"><i class="icon-tag"></i> Tags</a>
                    <a href="/service/getQuameterDataTable" class="btn"><i class="icon-download"></i> Data</a>
                    <a class="btn" id="reportButton"><i class="icon-book"></i> Report</a>
                </div>
            </div>
            <div class="row-fluid">
                <div class="span12">
                    <span id="icons">
                        <a href="#" id="search-link"><img src="/report/search_edit.gif" style="border: 0"></a>
                        <a href="#" id="qa-link"><img src="/report/search.gif" style="border: 0"></a>
                        <a id="hide-entry" role="button"><img src="/report/search_hide.gif" style="border: 0"></a>
                    </span>
                    <span id="selected-path"></span>
                </div>
            </div>
        </div>
    </div>

    <c:choose>
        <c:when test="${empty quameterUi}">
            <div class="alert">
                <p><strong>Warning</strong> The QuaMeter module is not configured.</p>

                <p>You need to add the QuaMeterUi resource to the <code>${daemonName}</code> daemon.</p>
            </div>
        </c:when>
        <c:otherwise>
            <div id="detailedGraphs" style="margin-top: 40px; display: none;"></div>
            <div id="simpleGraphs" style="margin-top: 40px"></div>
        </c:otherwise>
    </c:choose>

    <div id="hideDialog" class="modal hide fade" aria-hidden="true">
        <div class="modal-header">
            <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
            <h3>Hide a data point</h3>
        </div>
        <div class="modal-body">
            <p>You are about to hide a data point for file:</p>

            <p id="hide-path"></p>

            <p>Please provide a reason:</p>

            <p><input type="text" id="hideReason" class="input-block-level"
                      placeholder="Enter reason for hiding this data point"></p>

            <div class="alert alert-error" id="hideAlert" style="display:none;">
                <button type="button" class="close" data-dismiss="alert">&times;</button>
                <span></span>
            </div>
        </div>
        <div class="modal-footer">
            <a href="#" class="btn" data-dismiss="modal">Close</a>
            <a href="#" class="btn btn-primary" id="hideSubmit">Save changes</a>
        </div>
    </div>
</div>

<script type="text/javascript" src="/common/bootstrap/js/bootstrap.js"></script>

<script type="text/javascript">
    var metrics = ${metricsJson};
</script>

<script type="text/javascript" src="../../../quameter/js/dygraph-combined.js?v=${ver}"></script>
<script type="text/javascript" src="../../../quameter/js/dygraph-extra.js?v=${ver}"></script>
<script type="text/javascript" src="../../../quameter/js/quameter-definitions.js?v=${ver}"></script>
<script type="text/javascript" src="../../../quameter/js/quameter.js?v=${ver}"></script>
<script type="text/javascript">
    var graphDataSrvr = ${dataJson};
    // Set a callback to run when the Google Visualization API is loaded.
    google.setOnLoadCallback(initSimpleCharts(graphDataSrvr));

</script>
<!--  Help Docs come from: http://massqc.proteomesoftware.com/help/metrics.php  -->
<!-- Need to add troubleshooting? http://massqc.proteomesoftware.com/help/troubleshooting.php -->

</body>
</html>