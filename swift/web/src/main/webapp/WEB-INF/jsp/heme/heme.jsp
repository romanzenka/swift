<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
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
            background-image: url(../../../heme/ajax-loader.gif);
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
    <c:choose>
        <c:when test="${empty hemeUiConfig}">
            <div class="alert">
                <p><strong>Warning</strong> The heme pathology module is not configured.</p>

                <p>You need to add the Heme Pathology resource to the <code>${daemonName}</code> daemon.</p>
            </div>
        </c:when>
        <c:otherwise>
            <table class="table table-condensed">
                <tr>
                    <th>Patient</th>
                    <th>Date</th>
                    <th>Isotopic Mass<!--&Delta;--></th>
                    <th>Action</th>
                </tr>
                <c:forEach var="entry" items="${currentEntries}">
                    <tr>
                        <td>${entry.test.name}</td>
                        <td>${entry.test.date}</td>
                        <td>
                            <input class="mass-delta-value" data-id="${entry.test.id}" type="number" step="any"
                                   style="width:75px;text-align:right;"
                                   value="${entry.test.mass}">&nbsp;&nbsp;&plusmn;&nbsp;
                <span class="input-append"><input class="mass-delta-tolerance-value" data-id="${entry.test.id}"
                                                  type="number"
                                                  step="any" style="width: 30px" maxlength="8"
                                                  value="${entry.test.massTolerance}">
                    <span class="add-on">Da</span></span>
                            <span class="ajax-progress" id="save-${entry.test.id}">&nbsp;</span>

                            <div class="alert save-error" id="save-error-${entry.test.id}">Error!</div>
                        </td>
                        <td>
                            <c:choose>
                                <c:when test="${entry.status == notStarted}">
                                    <button class="btn btn-primary analyze-action" data-id="${entry.test.id}"
                                            type="button">Analyze
                                    </button>
                                </c:when>
                                <c:when test="${entry.status == running}">
                                    <div class="progress progress-striped" style="width: 100px">
                                        <div class="bar" style="width: ${entry.progressPercent}%;"></div>
                                    </div>
                                </c:when>
                                <c:when test="${entry.status == failed}">
                                    <div class="alert">Search failed</div>
                                    <button class="btn btn-primary analyze-action" data-id="${entry.test.id}"
                                            type="button">Re-analyze
                                    </button>
                                </c:when>
                                <c:when test="${entry.status == success}">
                                    <button class="btn btn-success result-action" data-id="${entry.test.id}" type="button">Result
                                    </button>
                                    <button class="btn btn-sm analyze-action" data-id="${entry.test.id}"
                                            type="button">Re-analyze
                                    </button>
                                </c:when>
                            </c:choose>
                        </td>
                    </tr>
                </c:forEach>
            </table>
        </c:otherwise>
    </c:choose>
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