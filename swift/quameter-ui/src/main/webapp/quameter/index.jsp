<%@ page import="edu.mayo.mprc.MprcException" %>
<%@ page import="edu.mayo.mprc.config.ResourceConfig" %>
<%@ page import="edu.mayo.mprc.quameterdb.QuameterUi" %>
<%@ page import="edu.mayo.mprc.swift.MainFactoryContext" %>
<%@ page import="edu.mayo.mprc.swift.SwiftWebContext" %>
<%@ page import="java.io.StringWriter" %>
<% final ResourceConfig quameterUiConfig = MainFactoryContext.getSwiftEnvironment().getSingletonConfig(QuameterUi.Config.class); %>
<!DOCTYPE html>
<html lang="en">
<head>
    <title>QuaMeter Data | <%=SwiftWebContext.getWebUi().getTitle()%>
    </title>

    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="/common/bootstrap/css/bootstrap.min.css" rel="stylesheet" media="screen">
    <style type="text/css">
            /* http://stackoverflow.com/questions/3790935/can-i-hide-the-html5-number-inputs-spin-box */
        input::-webkit-outer-spin-button,
        input::-webkit-inner-spin-button {
            /* display: none; <- Crashes Chrome on hover */
            -webkit-appearance: none;
            margin: 0; /* <-- Apparently some margin are still there even though it's hidden */
        }

    </style>
    <!-- HTML5 shim, for IE6-8 support of HTML5 elements -->
    <!--[if lt IE 9]>
    <script src="/common/bootstrap/js/html5shiv.js"></script>
    <![endif]-->

    <script type="text/javascript" src="/common/bootstrap/js/jquery_1.9.0.min.js"></script>

    <script type="text/javascript" src="dygraph-combined.js"></script>

    <!--Load the AJAX API-->
    <script type="text/javascript" src="https://www.google.com/jsapi"></script>
    <script type="text/javascript">

        // Load the Visualization API and the core package.
        google.load('visualization', '1.0', {'packages': ['corechart']});

        // Set a callback to run when the Google Visualization API is loaded.
        google.setOnLoadCallback(drawChart);

        // Callback that creates and populates a data table,
        // instantiates the pie chart, passes in the data and
        // draws it.
        function drawChart() {

            // Create the data table.
            var data = new google.visualization.DataTable(
                    <%

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
} else {

%>
                    null
                    <%
                   }
                   %>
                    , 0.6);

            g = new Dygraph(document.getElementById("chart_div"), data);
        }
    </script>

</head>
<body>
<div class="container">
    <h2>QuaMeter Results</h2>
    <% if (quameterUiConfig == null) {
    %>
    <div class="alert">
        <p><strong>Warning</strong> The QuaMeter module is not configured.</p>

        <p>You need to add the QuaMeterUi resource to the
            <code><%= MainFactoryContext.getSwiftEnvironment().getDaemonConfig().getName() %>
            </code> daemon.</p>
    </div>


    <% } else {
    %>
    <div id="chart_div"/>
    <% } %>
</div>

</body>
</html>