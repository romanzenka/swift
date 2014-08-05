<%@ page import="edu.mayo.mprc.MprcException" %>
<%@ page import="edu.mayo.mprc.config.ResourceConfig" %>
<%@ page import="edu.mayo.mprc.heme.HemeEntry" %>
<%@ page import="edu.mayo.mprc.heme.HemeUi" %>
<%@ page import="edu.mayo.mprc.swift.MainFactoryContext" %>
<%@ page import="java.util.List" %>
<% final ResourceConfig hemeUiConfig = MainFactoryContext.getSwiftEnvironment().getSingletonConfig(HemeUi.Config.class); %>
<!DOCTYPE html>
<html lang="en">
<head>
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

        .ajax-progress {
            visibility: hidden;
            display: inline-block;
            background-image: url(ajax-loader.gif);
            width: 16px;
            height: 11px;
            border: 0;
            padding: 0;
            margin: 0;
        }

        .save-error {
            display: none;
        }
    </style>
    <!-- HTML5 shim, for IE6-8 support of HTML5 elements -->
    <!--[if lt IE 9]>
    <script src="/common/bootstrap/js/html5shiv.js"></script>
    <![endif]-->

    <script type="text/javascript" src="/common/bootstrap/js/jquery_1.9.0.min.js"></script>
    <title>Heme Pathology</title>
    <script type="text/javascript">
    </script>
</head>
<body>
<div class="container">
    <h2>Heme Pathology</h2>
    <% if (hemeUiConfig == null) {
    %>
    <div class="alert">
        <p><strong>Warning</strong> The heme pathology module is not configured.</p>

        <p>You need to add the Heme Pathology resource to the
            <code><%= MainFactoryContext.getSwiftEnvironment().getDaemonConfig().getName() %>
            </code> daemon.</p>
    </div>
    <% } else {
        HemeUi hemeUi = (HemeUi) MainFactoryContext.getSwiftEnvironment().createResource(hemeUiConfig);
        hemeUi.begin();
        try {
            hemeUi.scanFileSystem();
            List<HemeEntry> currentEntries = hemeUi.getCurrentEntries();
    %>
    <table class="table table-condensed">
        <tr>
            <th>Patient</th>
            <th>Date</th>
            <th>Isotopic Mass<!--&Delta;--></th>
            <th>Action</th>
        </tr>
        <%
            for (HemeEntry entry : currentEntries) {
                int id = entry.getTest().getId();
        %>
        <tr>
            <td><%=entry.getTest().getName()%>
            </td>
            <td><%=entry.getTest().getDate().toString()%>
            </td>
            <td>
                <input class="mass-delta-value" data-id="<%= id %>" type="number" step="any" style="width:75px;text-align:right;"
                       value="<%= entry.getTest().getMass() %>">&nbsp;&nbsp;&plusmn;&nbsp;
                <span class="input-append"><input class="mass-delta-tolerance-value" data-id="<%= id %>" type="number"
                                                  step="any" style="width: 30px" maxlength="8"
                                                  value="<%= entry.getTest().getMassTolerance() %>">
                    <span class="add-on">Da</span></span>
                <span class="ajax-progress" id="save-<%= id %>">&nbsp;</span>

                <div class="alert save-error" id="save-error-<%= id %>">Error!</div>
            </td>
            <td>
                <%
                    switch (entry.getStatus()) {
                        case NOT_STARTED:
                %>
                <button class="btn btn-primary analyze-action" data-id="<%= +id %>" type="button">Analyze</button>
                <%
                        break;
                    case RUNNING:
                %>
                <div class="progress progress-striped" style="width: 100px">
                    <div class="bar" style="width: <%= entry.getProgressPercent() %>%;"></div>
                </div>
                <%
                        break;
                    case FAILED:
                %>
                <div class="alert">Search failed</div>
                <button class="btn btn-primary analyze-action" data-id="<%= id %>" type="button">Re-analyze</button>
                <%
                        break;
                    case SUCCESS:
                %>
                <button class="btn result-action" data-id="<%= id %>" type="button">Result</button>
                <%
                            break;
                    }

                %>
            </td>
        </tr>
        <%
            }
        %>
    </table>
    <%
            hemeUi.commit();
        } catch (Exception e) {
            hemeUi.rollback();
            throw new MprcException(e);
        }
    %>
    <% } %>
</div>

<script type="text/javascript">
    function runSearch(id) {
        $.ajax({
            url: "/service/heme/data/" + id + "/startSearch.json",
            method: "POST"
        }).complete(function () {
                    location.reload();
                });
    }

    function showResult(id) {
        window.location.href = "/service/heme/data/" + id + "/report.html";
    }

    function saveValue(id, type, value) {
        progress = $("#save-" + id);
        error = $("#save-error-" + id);
        error.hide();
        progress.css('visibility', 'visible');
        $
                .ajax({
                    url: "/service/heme/data/" + id + "/" + type + ".json",
                    method: "POST",
                    data: {
                        'value': value
                    }
                })
                .done(function () {
                    progress.css('visibility', 'hidden');
                })
                .error(function (xhr) {
                    progress.css('visibility', 'hidden');
                    error.html("Error: " + xhr.statusText);
                    error.show();
                });
    }

    $(".analyze-action").click(function () {
        id = parseInt($(this).data("id"));
        runSearch(id);
    });

    $(".result-action").click(function () {
        id = parseInt($(this).data("id"));
        showResult(id);
    });

    $(".mass-delta-value").change(function () {
        var e = $(this);
        id = parseInt(e.data("id"));
        saveValue(id, "massDelta", e.val());
    });

    $(".mass-delta-tolerance-value").change(function () {
        var e = $(this);
        id = parseInt(e.data("id"));
        saveValue(id, "massDeltaTolerance", e.val());
    });


</script>

</body>
</html>