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
<style type="text/css">
        /* http://stackoverflow.com/questions/3790935/can-i-hide-the-html5-number-inputs-spin-box */
    input::-webkit-outer-spin-button,
    input::-webkit-inner-spin-button {
        /* display: none; <- Crashes Chrome on hover */
        -webkit-appearance: none;
        margin: 0; /* <-- Apparently some margin are still there even though it's hidden */
    }

    body {
        margin-top: 60px;
    }

    .graph {
        width: 100%;
        height: 320px;
        margin-bottom: 10px;
    }

    .simple-graph {
        width: 100%;
        height: 200px;
        margin-bottom: 10px;
    }

    h4 {
        text-align: center;
    }

    .legend dt {
        float: left;
        margin-right: 0.5em;
    }

    .dygraph-legend {
        background-color: rgba(255, 255, 255, 0.7) !important;
    }

    .dygraph-legend > span.highlight {
        background-color: #f9f500;
    }

    .dygraph-legend > span > b {
        white-space: nowrap;
    }

    button.highlight {
        color: #bff;
    }

    .btn-toolbar {
        margin-top: 0;
        margin-bottom: 0;
    }

    .btn-group {
        margin-right: 5px;
    }

    #selected-path {
        margin: 0 5px 0 5px;
        color: #888;
        height: 25px;
        min-height: 25px;
        font-family: Monaco, Menlo, Consolas, "Courier New", monospace;
        font-size: 13px;
        text-overflow: ellipsis;
        white-space: nowrap;
        overflow: hidden;
    }

    #selected-path span {
        color: #bff;
    }

    .navbar a.brand {
        margin-left: -15px;
    }

</style>
<!-- HTML5 shim, for IE6-8 support of HTML5 elements -->
<!--[if lt IE 9]>
<script src="/common/bootstrap/js/html5shiv.js"></script>
<![endif]-->

<script type="text/javascript" src="/common/bootstrap/js/jquery_1.9.0.min.js"></script>
<script type="text/javascript" src="/common/bootstrap/js/bootstrap.js"></script>

<script type="text/javascript" src="dygraph-combined.js"></script>

<!--Load the AJAX API-->
<script type="text/javascript" src="https://www.google.com/jsapi"></script>
<script type="text/javascript">

// Load the Visualization API and the core package.
google.load('visualization', '1.0', {'packages': ['corechart']});

// Set a callback to run when the Google Visualization API is loaded.
google.setOnLoadCallback(drawChart);

function columnIndex(id, data) {
    for (var i = 0; i < data.getNumberOfColumns(); i++) {
        if (data.getColumnId(i) == id) {
            return i;
        }
    }
    alert("Column " + id + " does not exist");
}

// Add buttons to given div, one for each unique value from a given column
// div should be jQuery-enriched element
function addButtons(div, data, columnId) {
    var column = columnIndex(columnId, data);

    var names = {};
    for (var row = 0; row < data.getNumberOfRows(); row++) {
        var value = data.getValue(row, column);
        if (names[value]) {
            names[value]++;
        } else {
            names[value] = 1;
        }
    }
    var keys = $.map(names,function (element, index) {
        return index
    }).sort();

    $.each(keys, function (index, value) {
        div.append('<button type="button" class="btn btn-primary" value="' + value + '">' + value + ' (' + names[value] + ')<' + '/button>');
    });
}

function prePostCategory(path) {
    if (/_Pre[^.]+/.test(path)) {
        return "Pre";
    } else if (/_Post[^.]+/.test(path)) {
        return "Post";
    } else {
        return "Sample";
    }
}

// Same as above for the paths
function addPrePostButtons(div, data, columnId) {
    var column = columnIndex(columnId, data);

    var names = {};
    for (var row = 0; row < data.getNumberOfRows(); row++) {
        var value = data.getValue(row, column);
        var category = prePostCategory(value);
        if (names[category]) {
            names[category]++;
        } else {
            names[category] = 1;
        }
    }
    var keys = $.map(names,function (element, index) {
        return index
    }).sort();

    $.each(keys, function (index, value) {
        div.append('<button type="button" class="btn btn-primary" value="' + value + '">' + value + ' (' + names[value] + ')<' + '/button>');
    });
}


// Tokenize by underscore, wrap tokens in <span>
function spanAllUnderscoreTokens(s) {
    return s.split("_").map(function (item) {
        return '<span>' + item + '</span>'
    }).join('_');
}

// When true, redraws of graphs will not trigger more redraws
var blockRedraw = false;

// Row of the selected point
var pointSelected = -1;

// Row of the highlighted point
var pointHighlighted = -1;

// Transaction currently selected
var selectedTransaction = -1;

// Callback that filters all the views, updating the stdev ranges
function updateAllViews(views, filteredRows) {
    blockRedraw = true;
    // We deselect the user-selected point
    pointSelected = -1;
    pointHighlighted = -1;
    for (var i = 0; i < views.length; i++) {
        views[i].dataView.setRows(filteredRows);
        var sum = 0;
        var count = views[i].dataView.getNumberOfRows();
        var values = new Array(count);
        var j;
        for (j = 0; j < count; j++) {
            values[j] = views[i].dataView.getValue(j, 1);
        }
        // Remove the outlier 5% from each end
        values.sort();

        var percentRemoved = 5;
        values = values.slice(count * percentRemoved / 100, count * (100 - percentRemoved) / 100);
        count = values.length;

        for (j = 0; j < count; j++) {
            sum += values[j];
        }
        var average = count > 0 ? sum / count : 0;
        var sumSquares = 0;
        for (var j = 0; j < count; j++) {
            var delta = values[j] - average;
            sumSquares += delta * delta;
        }
        var stdev = count > 1 ? Math.sqrt(sumSquares / (count - 1)) : 1;

        views[i].minHighlightY = average - 3 * stdev;
        views[i].maxHighlightY = average + 3 * stdev;

        views[i].dygraph.updateOptions({file: views[i].dataView});
    }
    blockRedraw = false;
}

function selectPoint(data, dataRow) {
    if (dataRow == -1) {
        $('#icons').hide();
        selectedTransaction = -1;
    } else {
        $('#icons').show();
        var transactionColumnIndex = columnIndex("transaction", data);
        selectedTransaction = data.getValue(dataRow, transactionColumnIndex);
    }
}

function addDygraph(views, viewIndex, view, viewId, metricId, metrics, viewMetadata, data, pathColumnIndex, selectedPath, instrumentButtons, categoryButtons, instrumentColumnIndex, categoryColumnIndex) {
    views[viewIndex] = { dataView: view, minHighlightY: 1, maxHighlightY: -1, metricId: metricId };
    var currentView = views[viewIndex];

    // Row - the row in the original dataset
    function highlightRow(row) {
        if (row == -1) {
            selectedPath.text("");
            instrumentButtons.removeClass("highlight");
            categoryButtons.removeClass("highlight");
            return;
        }
        var path = data.getValue(row, pathColumnIndex);

        var pathChunks = /(.*\/)([^\/\\]+)(\.[^.]+)/.exec(path);
        var pathHtml = pathChunks[1] + spanAllUnderscoreTokens(pathChunks[2]) + pathChunks[3];

        selectedPath.html(pathHtml);

        instrumentButtons.removeClass("highlight");
        categoryButtons.removeClass("highlight");

        var instrument = data.getValue(row, instrumentColumnIndex);
        instrumentButtons.filter("[value='" + instrument + "']").addClass("highlight");

        var category = data.getValue(row, categoryColumnIndex);
        categoryButtons.filter("[value='" + category + "']").addClass("highlight");
    }

    var dygraph = new Dygraph(
            document.getElementById(viewId),
            views[viewIndex].dataView,
            {
                drawCallback: function (me, initial) {
                    if (blockRedraw || initial) return;
                    blockRedraw = true;
                    var range = me.xAxisRange();
                    for (var j = 0; j < views.length; j++) {
                        if (gs[j] == me) continue;
                        gs[j].updateOptions({
                            dateWindow: range
                        });
                    }
                    blockRedraw = false;
                },
                drawPoints: true,
                pointSize: 2,
                strokeWidth: 0.4,
                highlightSeriesOpts: {
                    strokeWidth: 2,
                    strokeBorderWidth: 1,
                    highlightCircleSize: 4
                },
                highlightCallback: function (event, x, points, viewRow, seriesName) {
                    pointHighlighted = viewRow;
                    var row = viewMetadata.filteredRows[viewRow];
                    if (pointSelected == -1) {
                        highlightRow(row);
                    }
                },
                unhighlightCallback: function (event) {
                    if (pointSelected == -1) {
                        highlightRow(-1);
                    }
                },
                underlayCallback: function (canvas, area, g) {
                    var metricIndex = 0;
                    for (var i = 0; i < metrics.length; i++) {
                        if (metrics.code == currentView.metricId) {
                            metricIndex = i;
                        }
                    }

                    if (currentView.minHighlightY < currentView.maxHighlightY) {
                        var bottom = g.toDomYCoord(currentView.minHighlightY);
                        var top = g.toDomYCoord(currentView.maxHighlightY);

                        var good = metrics[metricIndex].good;
                        canvas.fillStyle = "rgba(255, 235, 235, 1.0)";
                        if (good == "range" || good == "low") {
                            canvas.fillRect(area.x, area.y, area.w, top);
                        }
                        if (good == "range" || good == "high") {
                            canvas.fillRect(area.x, bottom, area.w, area.h - (bottom - area.y));
                        }
                    }
                    if (pointSelected >= 0) {
                        // Date to be selected
                        var date = currentView.dataView.getValue(pointSelected, 0);
                        var xcoord = g.toDomXCoord(date);
                        canvas.fillStyle = "rgba(10, 120, 255, 1.0)";
                        canvas.fillRect(xcoord - 2, area.y, 4, area.h);
                    }
                },
                pointClickCallback: function (event, point) {
                    // Select a point
                    if (pointSelected != pointHighlighted) {
                        pointSelected = pointHighlighted;
                    } else {
                        pointSelected = -1;
                    }
                    selectPoint(data, pointSelected == -1 ? -1 : viewMetadata.filteredRows[pointSelected]);
                    var row = viewMetadata.filteredRows[pointHighlighted];
                    highlightRow(row);

                    dygraph.updateOptions({file: currentView.dataView});
                }
            }
    );
    views[viewIndex].dygraph = dygraph;
    gs.push(views[viewIndex].dygraph);

    viewIndex++;
    return viewIndex;
}
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

    var viewMetadata = {};
    var allRows = Array(data.getNumberOfRows());
    for (var i = 0; i < data.getNumberOfRows(); i++) {
        allRows[i] = i;
    }
    viewMetadata.filteredRows = allRows;
    var views = [];

    var metricCategories = {
        c: "Chromatography",
        ds: "Dynamic Sampling",
        is: "Ion Source",
        ms1: "MS1 Signal",
        ms2: "MS2 Signal",
        p: "Protease"
    };
    var metrics = [
        { code: "c_1a", label: "C-1A", name: "Bleed Ratio", good: "low", simple: 0, desc: "Fraction of peptides with repeat identifications >4 min earlier than identification closest to the chromatographic maximum" },
        { code: "c_1b", label: "C-1B", name: "Peak Tailing Ratio", good: "low", simple: 0, desc: "Fraction of peptides with repeat identifications >4 min later than identification closest to the chromatographic maximum" },
        { code: "c_2a", label: "C-2A", name: "Retention Window", good: "high", simple: 0, desc: "Retention time period over which the middle 50% of the identified peptides eluted (minutes)" },
        { code: "duration", label: "Duration", name: "Duration", good: "range", simple: 0, desc: "Acquisition duration (minutes)" },
        { code: "c_2b", label: "C-2B", name: "ID Rate", good: "high", simple: 1, desc: "Rate of peptide identification during the C-2A time range" },
        { code: "c_3a", label: "C-3A", name: "Peak Width", good: "low", simple: 1, desc: "Median identified peak width" },
        { code: "c_3b", label: "C-3B", name: "Peak Width Spread", good: "low", simple: 1, desc: "Interquantile range for peak widths" },
        { code: "c_4a", label: "C-4A", name: "Late Peak Width", good: "low", simple: 0, desc: "Median peak width over <i>last 10%</i> of the elution time" },
        { code: "c_4b", label: "C-4B", name: "Early Peak Width", good: "low", simple: 0, desc: "Median peak width over <i>first 10%</i> of the elution time" },
        { code: "c_4c", label: "C-4C", name: "Middle Peak Width", good: "low", simple: 0, desc: "Median peak width over <i>middle 10%</i> of the elution time" },
        { code: "ds_1a", label: "DS-1A", name: "Singly Identified", good: "high", simple: 0, desc: "Ratio of singly to doubly identified peptide ions." },
        { code: "ds_1b", label: "DS-1B", name: "Triply Identified", good: "high", simple: 0, desc: "Ratio of doubly to triply identified peptide ions." },
        { code: "ds_2a", label: "DS-2A", name: "MS1 Scans", good: "range", simple: 0, desc: "Number of MS1 scans acquired during the C-2A time range" },
        { code: "ds_2b", label: "DS-2B", name: "MS2 Scans", good: "high", simple: 0, desc: "Number of MS2 scans acquired during the C-2A time range" },
        { code: "ds_3a", label: "DS-3A", name: "Peak Sampling", good: "low", simple: 0, desc: "Median ratio of the maximum MS1 peak intensity over the MS1 intensity at the sampling time for all identified peptides. We want to capture peak at its apex." },
        { code: "ds_3b", label: "DS-3B", name: "Low Peak Sampling", good: "low", simple: 0, desc: "Median ratio of the maximum MS1 peak intensity over the MS1 intensity at the sampling time for peptides with peak intensity in bottom 50%. We want to capture peak at its apex even for low-intensity peptides." },
        { code: "is_1a", label: "IS-1A", name: "TIC Drop", good: "low", simple: 0, desc: "TIC dropped more than 10x in two consecutive MS1 scans (within the C-2A time range)" },
        { code: "is_1b", label: "IS-1B", name: "TIC Jump", good: "low", simple: 0, desc: "TIC jumped more than 10x in two consecutive MS1 scans (within the C-2A time range)" },
        { code: "is_2", label: "IS-2", name: "Precursor", good: "range", simple: 1, desc: "Median precursor of identified peptide ions" },
        { code: "is_3a", label: "IS-3A", name: "1+ charge", good: "low", simple: 0, desc: "Ratio of 1+/2+ identified peptides" },
        { code: "is_3b", label: "IS-3B", name: "3+ charge", good: "low", simple: 0, desc: "Ratio of 3+/2+ identified peptides" },
        { code: "is_3c", label: "IS-3C", name: "4+ charge", good: "low", simple: 0, desc: "Ratio of 4+/2+ identified peptides" },
        { code: "ms1_1", label: "MS1-1", name: "MS1 Injection", good: "low", simple: 0, desc: "Median injection time for MS1 spectra" },
        { code: "ms1_2a", label: "MS1-2A", name: "MS1 S/N", good: "high", simple: 1, desc: "Ratio of maximum to median signal in MS1 spectra" },
        { code: "ms1_3a", label: "MS1-3A", name: "MS1 Dynamic Range", good: "high", simple: 0, desc: "Dynamic range - ratio of 95th and 5th percentile of MS1 maximum identities for identified peptides in C-2A time range" },
        { code: "ms1_2b", label: "MS1-2B", name: "MS1 TIC", good: "high", simple: 1, desc: "Median MS1 Total Ion Current" },
        { code: "ms1_5a", label: "MS1-5A", name: "AMU Error Median", good: "0", simple: 0, desc: "Median difference between the theoretical precursor m/z and the measured precursor m/z value as reported in the scan header" },
        { code: "ms1_5b", label: "MS1-5B", name: "AMU Error Mean", good: "0", simple: 0, desc: "Mean absolute difference between the theoretical precursor m/z and the measured precursor m/z value as reported in the scan header" },
        { code: "ms1_5c", label: "MS1-5C", name: "PPM Error", good: "0", simple: 1, desc: "Median precursor mass error in PPM" },
        { code: "ms1_5d", label: "MS1-5D", name: "PPM Error Range", good: "0", simple: 0, desc: "Interquartile range for mass error in PPM" },
        { code: "ms2_1", label: "MS2-1", name: "MS2 Injection", good: "low", simple: 0, desc: "Median injection time for MS2 spectra" },
        { code: "ms2_2", label: "MS2-2", name: "MS2 S/N", good: "high", simple: 1, desc: "Ratio of maximum to median signal in MS2 spectra" },
        { code: "ms2_3", label: "MS2-3", name: "MS2 Peaks#", good: "range", simple: 1, desc: "Median number of MS2 peaks" },
        { code: "ms2_4a", label: "MS2-4A", name: "MS2 ID 1", good: "range", simple: 0, desc: "Fraction of MS2 scans identified in the 1st quartile of peptides sorted by MS1 max intensity" },
        { code: "ms2_4b", label: "MS2-4B", name: "MS2 ID 2", good: "range", simple: 0, desc: "Fraction of MS2 scans identified in the 2nd quartile of peptides sorted by MS1 max intensity" },
        { code: "ms2_4c", label: "MS2-4C", name: "MS2 ID 3", good: "range", simple: 0, desc: "Fraction of MS2 scans identified in the 3rd quartile of peptides sorted by MS1 max intensity" },
        { code: "ms2_4d", label: "MS2-4D", name: "MS2 ID 4", good: "range", simple: 0, desc: "Fraction of MS2 scans identified in the 4th quartile of peptides sorted by MS1 max intensity" },
        { code: "p_1", label: "P-1", name: "Search Score", good: "high", simple: 1, desc: "Median peptide ID score" },
        { code: "p_2a", label: "P-2A", name: "MS2 Tryptic Spectra", good: "high", simple: 0, desc: "Number of MS2 spectra identifying tryptic peptide ions" },
        { code: "p_2b", label: "P-2B", name: "MS2 Tryptic Ions", good: "high", simple: 0, desc: "Number of tryptic peptide ions identified" },
        { code: "p_2c", label: "P-2C", name: "Distinct Peptides", good: "high", simple: 0, desc: "Number of distinct identified tryptic peptide sequences, ignoring modifications and charge state" },
        { code: "p_3", label: "P-3", name: "Semitryptic Ratio", good: "low", simple: 1, desc: "Ratio of semitryptic/tryptic peptides" }
    ];

    gs = [];

    function col(id) {
        return columnIndex(id, data);
    }

    selectPoint(data, -1);
    $('#search-link').click(function (event) {
        window.open('/start/?load=' + selectedTransaction);
    });
    $('#qa-link').click(function (event) {
        window.open('/service/qa/' + selectedTransaction + "/index.html");
    });

    var selectedPath = $('#selected-path');
    var pathColumnIndex = col('path');

    // Make buttons
    var categoryDiv = $('#category-buttons');
    addButtons(categoryDiv, data, 'category');

    var instrumentDiv = $('#instrument-buttons');
    addButtons(instrumentDiv, data, 'instrument');

    var prePostDiv = $('#prepost-buttons');
    addPrePostButtons(prePostDiv, data, 'path');

    var categoryButtons = categoryDiv.find('.btn');
    var categoryColumnIndex = col('category');
    var instrumentButtons = instrumentDiv.find('.btn');
    var instrumentColumnIndex = col('instrument');
    var prePostButtons = prePostDiv.find('.btn');
    var prePostColumnIndex = col('path');

    blockRedraw = true;

    var viewIndex = 0;
    var previousCategory = '';
    for (var i = 0; i < metrics.length; i++) {
        var metric = metrics[i];
        var categoryCode;
        var metricId = metric.code;
        if ("duration" == metricId) {
            categoryCode = "c";
        } else {
            categoryCode = metricId.split("_", 2)[0];
        }
        if (categoryCode != previousCategory) {
            $('<h3>' + metricCategories[categoryCode] + '</h3>').appendTo("#detailedGraphs");
            $('<h3>' + metricCategories[categoryCode] + '</h3>').appendTo("#simpleGraphs");
            previousCategory = categoryCode;
        }

        var view = new google.visualization.DataView(data);
        var cols = [];
        cols.push(col("startTime"));
        cols.push(col(metricId));
        view.setColumns(cols);
        var viewId = "graph-" + metricId;
        $('<div class="row-fluid">' +
                '<div class="span12">' +
                '<b>' + metric.name + '</b> '
                + metric.desc
                + '<div id="' + viewId + '" class="simple-graph"></div>' +
                '</div></div>')
                .appendTo("#detailedGraphs");
        viewIndex = addDygraph(
                views, viewIndex, view, viewId, metricId, metrics, viewMetadata, data, pathColumnIndex, selectedPath, instrumentButtons, categoryButtons, instrumentColumnIndex, categoryColumnIndex);

        if (1 == metric.simple) {
            viewId = "simpleGraph-" + metricId;
            $('<div class="row-fluid">' +
                    '<div class="span12">' +
                    '<b>' + metric.name + '</b> '
                    + metric.desc
                    + '<div id="' + viewId + '" class="simple-graph"></div>' +
                    '</div></div>')
                    .appendTo("#simpleGraphs");
            viewIndex = addDygraph(
                    views, viewIndex, view, viewId, metricId, metrics, viewMetadata, data, pathColumnIndex, selectedPath, instrumentButtons, categoryButtons, instrumentColumnIndex, categoryColumnIndex);
        }
    }

    blockRedraw = false;

    $('.btn').button();
    var allButtons = $.merge(categoryButtons, $.merge(instrumentButtons, prePostButtons));
    allButtons.click(function (event) {
        var current = $(this);

        if (!event.shiftKey) {
            current.siblings().removeClass("btn-primary");
            current.removeClass("btn-primary");
        }

        current.toggleClass("btn-primary");

        var selectedCategory = [];
        categoryButtons.each(function () {
            if ($(this).hasClass('btn-primary')) {
                selectedCategory.push($(this).attr("value"));
            }
        });

        var selectedInstrument = [];
        instrumentButtons.each(function () {
            if ($(this).hasClass('btn-primary')) {
                selectedInstrument.push($(this).attr("value"));
            }
        });

        var selectedPrePost = [];
        prePostButtons.each(function () {
            if ($(this).hasClass('btn-primary')) {
                selectedPrePost.push($(this).attr("value"));
            }
        });

        function filterRows() {
            var filteredRows = [];
            for (var row = 0; row < data.getNumberOfRows(); row++) {
                var category = data.getValue(row, categoryColumnIndex);
                var instrument = data.getValue(row, instrumentColumnIndex);
                var prePost = prePostCategory(data.getValue(row, prePostColumnIndex));
                if (0 <= $.inArray(category, selectedCategory)
                        && 0 <= $.inArray(instrument, selectedInstrument)
                        && 0 <= $.inArray(prePost, selectedPrePost)) {
                    filteredRows.push(row);
                }
            }
            return filteredRows;
        }

        var filteredRows = filterRows();

        viewMetadata['filteredRows'] = filteredRows;
        updateAllViews(views, filteredRows);
    });

    $("#compact-button").click(function (event) {
        var current = $(this);
        if (current.hasClass("btn-info")) {
            current.removeClass("btn-info");
            $('#detailedGraphs').css("display", "block");
            $('#simpleGraphs').css("display", "none");
        } else {
            current.addClass("btn-info");
            $('#detailedGraphs').css("display", "none");
            $('#simpleGraphs').css("display", "block");
        }
    });

    $('#detailedGraphs').css("display", "none");
}
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
                <div class="btn-group" id="prepost-buttons">
                </div>
            </div>

            <div class="btn-toolbar pull-left">
                <div class="btn-group" id="view-buttons">
                    <div class="btn btn-info" id="compact-button">Simple</div>
                </div>
            </div>

            <div class="row-fluid">
                <div class="span12">
                    <span id="icons">
                        <img src="/report/search_edit.gif" id="search-link">
                        <img src="/report/search.gif" id="qa-link">
                    </span>
                    <span id="selected-path"></span>
                </div>
            </div>
        </div>

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

        <div id="detailedGraphs">
        </div>

        <div id="simpleGraphs">
        </div>

        <% } %>
    </div>

</body>
</html>