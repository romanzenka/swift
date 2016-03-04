/** Global Vars to hack Google DataViews **/
var rawInstrumentNames = [];
var views = [];
var gs = [];
var numberOfSimpleGraphs = 0;
var dyViewsByCode = {};
var viewMetadata = {};
var selectedCategories = [];
var selectedInstruments = [];

// Array of
//  id, metricCode, quameterResultId, text
// for each annotation
var annotCollection = getAnnotationCollection();

// List currently hidden objects and mark them in a hidden array
var hiddenAnnotCollection = getHiddenAnnotationCollection();
for (var i in hiddenAnnotCollection) {
    if (hiddenAnnotCollection.hasOwnProperty(i)) {
        var obj = hiddenAnnotCollection[i];
        hiddenIds['id' + obj.quameterResultId] = true;
    }
}

/* Current selection info */
var selectedRow = -1;
var selectedTransaction = -1;
var selectedId = -1;


function populateInstArray(dt) {
    var instrumentCol = columnIndex("instrument", dt);
    for (var r = 0; r < dt.getNumberOfRows(); r++) {
        var val = dt.getValue(r, instrumentCol);
        if (!rawInstrumentNames.contains(val)) {
            rawInstrumentNames.push(val);
        }
    }
}

function columnIndex(id, data) {
    for (var i = 0; i < data.getNumberOfColumns(); i++) {
        if (data.getColumnId(i) === id) {
            return i;
        }
    }
    window.alert("Column " + id + " does not exist");
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
    var keys = $.map(names, function (element, index) {
        return index;
    }).sort();

    var iter = 1;
    $.each(keys, function (index, value) {
        var niceName = value;
        var btnClass = 'btn-default';
        if (columnId === 'instrument') {
            if (index === 0) {
                selectedInstruments.push(value);
                btnClass = 'btn-orig' + iter; // Instrument buttons have btn-orig# class when highlighted
            }
        } else if (columnId === 'category') {
            if (index === 0) {
                selectedCategories.push(value);
                btnClass = 'btn-primary'; // Only first button is highlighted
            }
        } else {
            alert("Unsupported button columnId " + columnId);
        }
        div.append('<button type="button" class="btn btn-small ' + btnClass + '" value="' + value + '" data-toggle="tooltip" data-placement="bottom" data-container="body" data-num=' + iter + ' data-enum="' + columnId + '" title="' + niceName + ': ' + names[value] + ' entries">' + niceName + '<' + '/button>');
        iter++;
    });
}

function instrumentButtons() {
    var instrumentDiv = $('#instrument-buttons');
    return instrumentDiv.find('.btn');
}

function categoryButtons() {
    var categoryDiv = $('#category-buttons');
    return categoryDiv.find('.btn');
}

function zoomButtons() {
    var zoomDiv = $("#zoom-buttons");
    return zoomDiv.find('.btn');
}

function createNewAnnotationForm(parentName, dbID) {
    var metricCode = parentName.split("-")[1];
    $('#hiddenMetricCode').val(metricCode);
    $('#hiddenRowid').val(dbID);
    var annotationText = $('#annotationText');
    annotationText.val('');
    for (i in annotCollection) {
        if (annotCollection.hasOwnProperty(i)) {
            if (annotCollection[i].metricCode === metricCode && annotCollection[i].quameterResultId === dbID) {
                annotationText.val(annotCollection[i].text);
            }
        }
    }
    $('#annotFormDiv').show();
}

function getXaxisNseriesById(data, dbId) {
    var idColumn = columnIndex("id", data);
    for (var i = 0; i < data.getNumberOfRows(); i++) {
        if (data.getValue(i, idColumn).toString() === dbId.toString()) {
            return [data.getValue(i, columnIndex("startTime", data)), data.getValue(i, columnIndex("instrument", data))];
        }
    }
    // Annotation can be older than 1 year, no longer shows
    return [-1, null];
}

// Make a list of annotations for dygraph.
// collection - annotations coming from the client
// data - all the data from the client
// metricCode - which metric are we making dygraph for
function buildCollection(collection, data, metricCode) {
    var arrayForDYgraphs = [];
    for (var o in collection) {
        if (collection.hasOwnProperty(o)) {
            if (collection[o].metricCode === metricCode) {
                var nthx = getXaxisNseriesById(data, collection[o].quameterResultId);
                if (nthx[0] >= 0) {
                    arrayForDYgraphs.push(
                        {
                            series: nthx[1],
                            x: nthx[0].toString(),
                            shortText: $.trim(collection[o].text)[0],
                            text: collection[o].text
                        }
                    );
                }
            }
        }
    }
    return arrayForDYgraphs;
}

function getMetricTitle(n) {
    var hLink, qLink;
    if (metrics[n].hasOwnProperty('link') && metrics[n].link !== "") {
        hLink = 'href="/quameter/' + metrics[n].link + '" target="_blank"';
        qLink = '&nbsp;<i class="icon-large icon-question-sign"></i>';
    }
    else {
        hLink = 'href="#"';
        qLink = '';
    }
    return '<a ' + hLink + ' class="modLink" >' +
        metrics[n].name + qLink +
        '</a> <span class="metric-desc">&mdash; ' +
        metrics[n].desc +
        ' (' + metrics[n].label + ')' +
        '</span>';
}

// Tokenize by underscore, wrap tokens in <span>
function spanAllUnderscoreTokens(s) {
    return s.split("_").map(function (item) {
        return '<span>' + item + '</span>';
    }).join('_');
}


function getCategoriesToShow() {
    var categoriesToShow = selectedCategories.slice(0); // Clone
    var addContaminantProteins = true;
    $.each(specialMetrics, function (key, value) {
        if (selectedCategories.contains(key)) {
            if (value.hasOwnProperty("pool")) {
                // This category is a pool of other categories
                categoriesToShow.splice($.inArray(key, categoriesToShow), 1);
                categoriesToShow = categoriesToShow.concat(value.pool);
            }
            if (value.hasOwnProperty("noContaminants")) {
                // Do not show contaminant proteins
                addContaminantProteins = false;
                // Do not show this category either
                categoriesToShow.splice($.inArray(key, categoriesToShow), 1);
            }
        }
    });

    if (addContaminantProteins) {
        categoriesToShow = categoriesToShow.concat(contaminantCategories);
    }
    return categoriesToShow;
}

function activeCategoriesFilters() {
    var categoriesToShow = getCategoriesToShow();
    allCategories.forEach(function (value) {
        var wrapperId = "#wrapper-" + value; //for protein id, hide entire graph when excluded
        if (categoriesToShow.contains(value)) {
            if ($(wrapperId).length) // use this if you are using id to check
            {
                $(wrapperId).show();
            }
        }
        else {
            if ($(wrapperId).length) // use this if you are using id to check
            {
                $(wrapperId).hide();
            }
        }
    });
    return selectedCategories;
}

function activeInstrumentFilters() {
    return selectedInstruments;
}

// Callback that filters all the views, updating the stdev ranges
function updateAllViews(data) {
    blockRedraw = true;
    // We deselect the user-selected point
    pointSelected = -1;
    pointHighlighted = -1;

    var activeCats = activeCategoriesFilters();

    var filteredRows = [];
    for (var r = 0; r < data.getNumberOfRows(); r++) {
        var category = data.getValue(r, columnIndex('category', data));
        var rowId = data.getValue(r, columnIndex("id", data));
        if (activeCats.contains(category) && !hiddenIds["id" + rowId]) {
            filteredRows.push(r);
        }
    }

    viewMetadata.filteredRows = filteredRows;

    for (var i = 0; i < views.length; i++) {
        if (views[i] === undefined) {
            console.log(i);
            continue;
        } //empty for detail graphs until generated
        dyViewsByCode[views[i].metricId] = i;

        var columns = getSmartColumns(views[i].metricId, data);
        views[i].dataView.setColumns(columns);
        views[i].dataView.setRows(filteredRows);

        // We can only do this if we have a single instrument, otherwise the error bars are meaningless
        if (columns.length === 2) {
            var sum = 0;
            var count = views[i].dataView.getNumberOfRows();
            var values = new Array(count);
            var j;
            for (j = 0; j < count; j++) {
                values[j] = views[i].dataView.getValue(j, 1 /* 0-startTime, 1-value */);
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
            for (j = 0; j < count; j++) {
                var delta = values[j] - average;
                sumSquares += delta * delta;
            }
            var stdev = count > 1 ? Math.sqrt(sumSquares / (count - 1)) : 1;

            views[i].minHighlightY = average - 3 * stdev;
            views[i].maxHighlightY = average + 3 * stdev;
            views[i].minHighlightY2 = average - 5 * stdev;
            views[i].maxHighlightY2 = average + 5 * stdev;
        } else {
            // Disable highlights by providing empty ranges.
            views[i].minHighlightY = 1;
            views[i].maxHighlightY = -1;
            views[i].minHighlightY2 = 1;
            views[i].maxHighlightY2 = -1;
        }

        views[i].dygraph.updateOptions({
            file: views[i].dataView,
            valueRange: getMetricByCode(views[i].metricId).range,
            colors: getSmartColors()
        }, false);
    }
    blockRedraw = false;
}

function selectPoint(data, dataRow) {
    if (dataRow === -1) {
        $('#icons').hide();
        selectedTransaction = -1;
        selectedId = -1;
        selectedRow = -1;
    } else {
        var transactionColumnIndex = columnIndex("transaction", data);
        selectedRow = dataRow;
        selectedTransaction = data.getValue(dataRow, transactionColumnIndex);
        selectedId = data.getValue(dataRow, columnIndex("id", data));
        $('#search-link').attr("href", '/start/?load=' + selectedTransaction);
        $('#qa-link').attr("href", '/service/qa/' + selectedTransaction + "/index.html");
        $('#icons').show();
    }
}

var blockRedraw = false;

function addDygraph(viewIndex, view, viewId, metricId, viewMetadata, data, range, annotCollection) {
    views[viewIndex] = {
        dataView: view,
        minHighlightY: 1,
        maxHighlightY: -1,
        minHighlightY2: 1,
        maxHighlightY2: -1,
        metricId: metricId
    };
    var currentView = views[viewIndex];
    var selectedPath = $('#selected-path');

    var instrumentText = $.map(instrumentButtons(), function (val) {
        return val.firstChild.data;
    });
    instrumentText.unshift("Date");

    // Row - the row in the original dataset
    function highlightRow(row) {
        if (row === -1) {
            selectedPath.text("");
            instrumentButtons().removeClass("highlight");
            categoryButtons().removeClass("highlight");
            return;
        }
        var path = data.getValue(row, columnIndex('path', data));

        var pathChunks = /(.*\/)([^\/\\]+)(\.[^.]+)/.exec(path);
        var pathHtml = pathChunks[1] + spanAllUnderscoreTokens(pathChunks[2]) + pathChunks[3];

        selectedPath.html(pathHtml);

        instrumentButtons().removeClass("highlight");
        categoryButtons().removeClass("highlight");

        var instrument = data.getValue(row, columnIndex('instrument', data));
        instrumentButtons().filter("[value='" + instrument + "']").addClass("highlight");

        var category = data.getValue(row, columnIndex('category', data));
        categoryButtons().filter("[value='" + category + "']").addClass("highlight");
    }

    var dygraph = new Dygraph(
        document.getElementById(viewId),
        views[viewIndex].dataView,
        {
            zoomCallback: function (minDate, maxDate, yRanges) {
                if (blockRedraw) {
                    return;
                }
                blockRedraw = true;
                var range = [minDate, maxDate];
                for (var j = 0; j < views.length; j++) {
                    if (gs[j] === undefined) {
                        console.log("Errant LOOKUP", j, gs)
                    }
                    if (gs[j] !== dygraph) {
                        gs[j].updateOptions({
                            dateWindow: range
                        });
                    }
                }
                blockRedraw = false;
            },
            drawPoints: true,
            connectSeparatedPoints: true,
            pointSize: 2,
            strokeWidth: 0.7,
            highlightSeriesOpts: {
                strokeWidth: 2,
                strokeBorderWidth: 1,
                highlightCircleSize: 4
            },
            highlightCallback: function (event, x, points, viewRow, seriesName) {
                if (viewRow < 0 || viewRow >= viewMetadata.filteredRows.length) {
                    highlightRow(-1);
                } else {
                    pointHighlighted = viewRow;
                    var row = viewMetadata.filteredRows[viewRow];
                    if (pointSelected === -1) {
                        highlightRow(row);
                    }
                }
            },
            unhighlightCallback: function (event) {
                if (pointSelected === -1) {
                    highlightRow(-1);
                }
            },
            underlayCallback: function (canvas, area, g) {
                if (currentView.minHighlightY < currentView.maxHighlightY && !currentView.metricId.match(/^id_/)) {
                    var bottom = g.toDomYCoord(currentView.minHighlightY);
                    var top = g.toDomYCoord(currentView.maxHighlightY);
                    var bottom2 = g.toDomYCoord(currentView.minHighlightY2);
                    var top2 = g.toDomYCoord(currentView.maxHighlightY2);

                    var good = getMetricByCode(currentView.metricId).good;
                    //canvas.fillStyle = "rgba(255, 235, 235, 1.0)"; //red
                    canvas.fillStyle = "rgba(224,224,224,1.0)"; //Gray
                    if (good === "range" || good === "low") {
                        canvas.fillRect(area.x, area.y, area.w, top);
                    }
                    if (good === "range" || good === "high") {
                        canvas.fillRect(area.x, bottom, area.w, area.h - (bottom - area.y));
                    }
                    //canvas.fillStyle = "rgba(250, 189, 189, 1.0)"; //red
                    canvas.fillStyle = "rgba(160,160,160,1.0)"; //gray
                    if (good === "range" || good === "low") {
                        canvas.fillRect(area.x, area.y, area.w, top2);
                    }
                    if (good === "range" || good === "high") {
                        canvas.fillRect(area.x, bottom2, area.w, area.h - (bottom2 - area.y));
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
            clickCallback: function (event, x, point) {
                if (point.length > 0) {
                    point = point[0];
                    // Select a point
                    if (pointSelected != pointHighlighted) {
                        pointSelected = pointHighlighted;
                    } else {
                        pointSelected = -1;
                    }
                    selectPoint(data, pointSelected === -1 ? -1 : viewMetadata.filteredRows[pointSelected]);
                    var row = viewMetadata.filteredRows[pointHighlighted];
                    highlightRow(row);

                    if (pointSelected !== -1) {
                        createNewAnnotationForm(event.target.parentNode.parentNode.id, data.getValue(row, columnIndex("id", data)));
                    } else {
                        $('#annotFormDiv').hide();
                    }
                    for (var i in views) {
                        if (views.hasOwnProperty(i)) {
                            views[i].dygraph.updateOptions({
                                file: views[i].dataView
                            });
                        }
                    }
                }
            }
        }
    );

    var annotArr = buildCollection(annotCollection, data, metricId);
    dygraph.setAnnotations(annotArr);
    views[viewIndex].dygraph = dygraph;
    gs.push(views[viewIndex].dygraph);
}


//  Important basic graphing functions
function drawGraphsByMetrics(data, renderDetailGraphs, viewMetadata) {
    var viewIndex = 0;
    if (renderDetailGraphs) {
        viewIndex = numberOfSimpleGraphs - 1;
    }
    var previousCategory = '';
    for (var i = 0; i < metrics.length; i++) {
        var metric = metrics[i];
        var categoryCode;
        var metricId = metric.code;
        var categoryCode = getCategoryCodeFromMetric(metric);

        if (categoryCode !== previousCategory) {
            if (renderDetailGraphs) {
                $('<h3>' + metricCategories[categoryCode] + '</h3>').appendTo("#detailedGraphs");
            }
            else {
                $('<h3>' + metricCategories[categoryCode] + '</h3>').appendTo("#simpleGraphs");
            }
            previousCategory = categoryCode;
        }

        var view = new google.visualization.DataView(data);

        view.setColumns(getSmartColumns(metricId, data));

        var viewId;
        if (renderDetailGraphs) {
            viewId = "graph-" + metricId;
            $('<div id="wrapper-' + metric.label + '" class="row-fluid"><div class="span12">' +
                getMetricTitle(i) +
                '<div id="' + viewId + '" class="simple-graph"></div>' +
                '</div></div>'
            ).appendTo("#detailedGraphs");
            addDygraph(viewIndex, view, viewId, metricId, viewMetadata, data, metric.range, annotCollection);
            viewIndex++;
        }
        else {
            if (1 === metric.simple) {
                viewId = "simpleGraph-" + metricId;
                $('<div id="wrapper-' + metric.label + '" class="row-fluid">' +
                    '<div class="span12">' +
                    getMetricTitle(i) +
                    '<div id="' + viewId + '" class="simple-graph"></div>' +
                    '</div></div>'
                ).appendTo("#simpleGraphs");
                addDygraph(viewIndex, view, viewId, metricId, viewMetadata, data, metric.range, annotCollection);
                viewIndex++;
            }
        }
    }
    if (!renderDetailGraphs) {
        numberOfSimpleGraphs = viewIndex;
    }
}

// Start a new report page
function startNewPage(doc, config, pageNumber, numPages) {
    doc.setFontSize(config.headerFontSize);
    doc.setLineWidth(0);
    doc.text("Immunostains Monthly Mass Spectrometry QC Report", 0.1, 0.2);
    var value = new Date().toLocaleDateString('en-US', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit'
    });
    var dateString = "Generated " + value;
    doc.text(dateString, config.pageWidth - 0.2 - doc.getTextDimensions(dateString).w / 72, 0.2);
    var pageString = "Page " + pageNumber + " of " + numPages;
    doc.text(pageString, config.pageWidth - 0.2 - doc.getTextDimensions(pageString).w / 72, config.pageHeight - 0.1);

    var yCoord = config.topMargin;

    return yCoord;
}

function getCategoryCodeFromMetric(metric) {
    var categoryCode;
    var metricId = metric.code;
    if ("duration" === metricId || "fileSize" === metricId) {
        categoryCode = "c";
    } else {
        categoryCode = metricId.split("_", 2)[0];
    }
    return categoryCode;
}

// Does the actual report drawing
// Returns number of pages generated. If numPages is set to -1, we do not do an actuall drawing pass,
// we are just calculating the pages
function drawGraphsToReport(data, viewMetadata, doc, numPages) {
    var noDrawing = numPages === -1;
    var viewIndex = 0;
    var previousCategory = '';
    var categoriesToShow = getCategoriesToShow();
    var c = {
        'topMargin': 0.6,
        'bottomMargin': 1,
        'lineHeight': 0.2,
        'annotLineHeight': 0.19,
        'finePrintLineHeight': 0.12,
        'titleLineHeight': 0.3,
        'headerFontSize': 6,
        'pageWidth': 8.5,
        'pageHeight': 11,
        'typicalGraphHeight': 3,
        'categoryFontSize': 14,
        'metricFontSize': 9,
        'titleFontSize': 12,
        'lineFontSize': 9,
        'annotFontSize': 8,
        'finePrintFontSize': 6,
    };

    // Find the first dygraph to be displayed so we can extract the date ranges
    var firstDygraph = null;
    for (var i = 0; i < metrics.length; i++) {
        var metric = metrics[i];
        var metricId = metric.code;
        if (1 === metric.simple) {
            var nthView = dyViewsByCode[metricId];
            firstDygraph = views[nthView].dygraph;
        }
    }
    var xAxisRange = firstDygraph.xAxisRange();
    var fromDate = new Date(xAxisRange[0]);
    var toDate = new Date(xAxisRange[1]);

    var yCoord = c.topMargin;

    var pageNumber = 1;

    yCoord = startNewPage(doc, c, pageNumber, numPages);

    doc.setFontSize(c.titleFontSize);

    function centerText(txt) {
        doc.text(txt, (c.pageWidth - doc.getTextDimensions(txt).w / 72) / 2, yCoord);
        yCoord += c.titleLineHeight;
    }

    centerText("Mayo Clinic Laboratories");

    centerText("RST");

    centerText("Anatomic Pathology, Immunostains Laboratory");

    yCoord += c.titleLineHeight;

    centerText("Monthly Mass Spectrometry QC Report");

    yCoord += c.lineHeight;

    doc.setFontSize(c.metricFontSize);
    function formatDate(d) {
        return d.toISOString().substring(0, 10);
    }

    doc.text("Date Range: " + formatDate(fromDate) + " to " + formatDate(toDate), 1, yCoord);
    yCoord += c.titleLineHeight;

    doc.text("Instrument: " + selectedInstruments[0], 1, yCoord);
    yCoord += c.titleLineHeight;

    for (var i = 0; i < metrics.length; i++) {

        if (yCoord + c.typicalGraphHeight + c.titleLineHeight * 2 > c.pageHeight - c.bottomMargin) {
            doc.addPage();
            pageNumber++;
            yCoord = startNewPage(doc, c, pageNumber, numPages);
        }

        var metric = metrics[i];
        var categoryCode = getCategoryCodeFromMetric(metric);
        var metricId = metric.code;

        var view = new google.visualization.DataView(data);
        view.setColumns(getSmartColumns(metricId, data));

        if (1 === metric.simple && (categoryCode != 'id' || categoriesToShow.contains(metric.label))) {
            if (categoryCode !== previousCategory) {
                yCoord += c.titleLineHeight / 2;

                doc.setFontSize(c.categoryFontSize);
                doc.text(metricCategories[categoryCode], 1, yCoord);
                yCoord += c.titleLineHeight;

                previousCategory = categoryCode;
            }

            doc.setFontSize(c.metricFontSize);
            doc.text(metrics[i].name + " - " + metrics[i].desc + " (" + metric.label + ")", 1, yCoord);
            yCoord += c.lineHeight;

            var nthView = dyViewsByCode[metricId];

            c.typicalGraphHeight = addGraphToReport(doc, yCoord, data, view, metric.range, annotCollection, views[nthView].dygraph, noDrawing);
            yCoord += c.typicalGraphHeight;
            yCoord += c.lineHeight;
            viewIndex++;
        }
    }

    function addZero(i) {
        if (i < 10) {
            i = "0" + i;
        }
        return i;
    }

    // -------------------------------------------------------------------------
    function listAnnotations(title, annotCollection, filteredRowsCheck) {
        doc.setFontSize(c.lineFontSize);
        doc.text(title, 1, yCoord);
        yCoord += c.lineHeight;

        if (yCoord > c.pageHeight - c.bottomMargin) {
            doc.addPage();
            pageNumber++;
            yCoord = startNewPage(doc, c, pageNumber, numPages);
        }

        var instruments = activeInstrumentFilters();
        for (var annot in annotCollection) {
            if (!annotCollection.hasOwnProperty(annot)) {
                continue;
            }
            doc.setFontSize(c.annotFontSize);

            if (annotCollection.hasOwnProperty(annot)) {
                var obj = annotCollection[annot];

                var row = findRowForId(data, obj.quameterResultId);
                if (row === -1) {
                    continue;
                }
                var startTime = data.getValue(row, columnIndex('startTime', data));

                var instrument = data.getValue(row, columnIndex('instrument', data));

                if (startTime >= fromDate && startTime <= toDate &&
                    instruments.contains(instrument)) {

                    var path = data.getValue(row, columnIndex('path', data));
                    var pathChunks = /(.*\/)([^\/\\]+)/.exec(path);
                    var fileName = pathChunks[2];

                    doc.text(fileName + ": ", 1.2, yCoord);
                    yCoord += c.annotLineHeight;

                    var text = "    " + startTime.getFullYear() + "-" +
                        addZero(startTime.getMonth() + 1) + "-" +
                        addZero(startTime.getDate()) + " " +
                        addZero(startTime.getHours()) + ":" +
                        addZero(startTime.getMinutes()) +
                        " - " + obj.text;

                    doc.text(text, 1.2, yCoord);

                    yCoord += c.annotLineHeight * 1.5;

                    if (yCoord > c.pageHeight - c.bottomMargin) {
                        doc.addPage();
                        pageNumber++;
                        yCoord = startNewPage(doc, c, pageNumber, numPages);
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------

    var hiddenAnnotCollection = getHiddenAnnotationCollection();
    listAnnotations("Hidden Files:", hiddenAnnotCollection);
    listAnnotations("Additional Annotations:", annotCollection);

    doc.addPage();
    pageNumber++;
    yCoord = startNewPage(doc, c, pageNumber, numPages);

    doc.setFontSize(c.lineFontSize);
    doc.text("REVIEW AND APPROVAL SIGNATURES", 1, yCoord);
    yCoord += c.lineHeight;
    yCoord += c.lineHeight;
    yCoord += c.lineHeight;

    doc.text("Technical Specialist    _________________________   Date _________________", 1, yCoord);
    yCoord += c.lineHeight;
    yCoord += c.lineHeight;
    yCoord += c.lineHeight;

    doc.text("Quality Specialist       _________________________   Date _________________", 1, yCoord);
    yCoord += c.lineHeight;
    yCoord += c.lineHeight;
    yCoord += c.lineHeight;

    doc.text("Supervisor                 _________________________   Date _________________", 1, yCoord);
    yCoord += c.lineHeight;
    yCoord += c.lineHeight;
    yCoord += c.lineHeight;

    doc.setFontSize(c.finePrintFontSize);
    doc.text("COPYRIGHT (c) Mayo Foundation for Medical Education and Research. This information is intended for the use of Mayo and affiliate laboratories employees only.", 1, yCoord);
    yCoord += c.finePrintLineHeight;
    doc.text("It is confidential and no part of it may be transmitted in any form by electronic, mechanical, photocopying, or any other means to anyone outside Mayo and affiliate", 1, yCoord);
    yCoord += c.finePrintLineHeight;
    doc.text("laboratories without the prior permission of its approver and/or copyright holder. Inappropriate use or dissemination of this information may result in disciplinary", 1, yCoord);
    yCoord += c.finePrintLineHeight;
    doc.text("or other legal action.", 1, yCoord);

    return pageNumber;
}

// Add a graph to the pdf report
function addGraphToReport(doc, yCoord, data, view, range, annotations, dygraph, noDrawing) {
    var pageWidth = 8.5;
    var margin = 1;
    var graphLeft = margin;
    var graphRight = pageWidth - margin;
    var graphHeight = (graphRight - graphLeft) / dygraph.width_ * dygraph.height_;

    if (!noDrawing) {

        // Report coordinates
        var rx0 = graphLeft;
        var ry0 = yCoord;
        var rw = graphRight - graphLeft;
        var rh = graphHeight;

        // Graph coordinates
        var x0 = dygraph.xAxisRange()[0];
        var y0 = dygraph.yAxisRange[0];
        var w = dygraph.xAxisRange()[1] - dygraph.xAxisRange()[0];
        var h = dygraph.yAxisRange()[1] - dygraph.yAxisRange()[0];

        var imgData = Dygraph.Export.asPNG(dygraph);

        doc.addImage(imgData, 'PNG', rx0, ry0, rw, rh);
    }

    return graphHeight;
}

function highlightInstrumentButtons() {
    instrumentButtons().each(function (index, button) {
        button = $(button);
        button.removeClass("btn-orig" + button[0].dataset.num);
        if (selectedInstruments.contains(button.val())) {
            button.addClass('btn-orig' + button[0].dataset.num);
        }
    });
}

function highlightCategoryButtons() {
    categoryButtons().each(function (index, button) {
        button = $(button);
        button.removeClass("btn-primary");
        button.removeClass("btn-default");
        if (selectedCategories.contains(button.val())) {
            button.addClass("btn-primary");
        } else {
            button.addClass("btn-default");
        }
    });
}

/** INIT() draws only visible **/
function initSimpleCharts(graphObj) {
    // Create the data table. 
    var data = new google.visualization.DataTable(graphObj, 0.6);
    populateInstArray(data);

    // Populate array with index to every row
    var allRows = new Array(data.getNumberOfRows());
    for (var i = 0; i < data.getNumberOfRows(); i++) {
        allRows[i] = i;
    }
    viewMetadata.filteredRows = allRows;

    selectPoint(data, -1);

    // Make buttons
    var categoryDiv = $('#category-buttons');
    addButtons(categoryDiv, data, 'category');
    var instrumentDiv = $('#instrument-buttons');
    addButtons(instrumentDiv, data, 'instrument');

    blockRedraw = true;

    drawGraphsByMetrics(data, false, viewMetadata);

    blockRedraw = false;

    // Change button Colors then Filter based on value
    $('.btn').button();

    // Instrument buttons allow shift-click selection

    function getButtonClickHandler(buttonType, supportMultiselect) {
        return function (event) {
            var current = $(this);
            if (!(supportMultiselect && event.shiftKey)) {
                // Deselect all the buttons by setting their third class to btn-default
                if (buttonType === 'instrument') {
                    selectedInstruments = [];
                } else {
                    selectedCategories = [];
                }
            }

            // Toggle current button
            if (buttonType === 'instrument') {
                var index = $.inArray(current.val(), selectedInstruments)
                if (index !== -1) {
                    selectedInstruments.splice(index, 1);
                } else {
                    selectedInstruments.push(current.val());
                }
                highlightInstrumentButtons();
            } else {
                selectedCategories = [];
                selectedCategories.push(current.val());
                highlightCategoryButtons();
            }

            updateAllViews(data);
        };
    }

    instrumentButtons().click(getButtonClickHandler("instrument", true)); // Support multiselect
    categoryButtons().click(getButtonClickHandler("category", false));  // Only one category can be shown at a time

    // Simple/Detailed Button
    $("#compact-button").click(function (event) {
        var current = $(this);
        if (current.hasClass("btn-info")) {
            current.removeClass("btn-info");
            current.text("Simple");
            $('#detailedGraphs').css("display", "block");
            $('#simpleGraphs').css("display", "none");
        } else {
            current.addClass("btn-info");
            current.text("Details");
            $('#detailedGraphs').css("display", "none");
            $('#simpleGraphs').css("display", "block");
        }

        if (!detailsExist) {
            drawGraphsByMetrics(data, true, viewMetadata);
            updateAllViews(data);
            detailsExist = true;
        }

    });

    $("#zoom1m-button").click(function (event) {
        setZoomMonths(1, data, viewMetadata);
    });

    $("#zoom2m-button").click(function (event) {
        setZoomMonths(2, data, viewMetadata);
    });

    $("#zoom6m-button").click(function (event) {
        setZoomMonths(6, data, viewMetadata);
    });


    //Little Hide Icon, when point on a graph is selected
    $('#hide-entry').click(function (event) {
        event.stopPropagation();
        var path = data.getValue(selectedRow, columnIndex('path', data));
        $('#hide-path').html(path.replace(/\//g, '/&#8203;').replace(/-/g, '&#8209;'));
        var hideReason = $('#hideReason');
        hideReason.val("");
        for (i in annotCollection) {
            if (annotCollection.hasOwnProperty(i)) {
                var obj = annotCollection[i];
                if (obj.quameterResultId === selectedId && obj.metricCode === "hidden") {
                    hideReason.val(obj.text);
                    break;
                }
            }
        }

        $('#hideDialog').modal('show');
    });

    $('#hideSubmit').click(function (event) {
        $("#hideAlert").hide();
        $.ajax({
            type: "POST",
            url: "/service/quameter-hide/" + selectedId,
            data: {reason: $("#hideReason").val()}
        }).done(function () {
            hiddenIds["id" + selectedId] = true;
            selectPoint(data, -1);
            updateAllViews(data);
            $("#hideDialog").modal('hide');
        }).fail(function (request, status, error) {
            var hideAlert = $("#hideAlert");
            hideAlert.find("span").text("Error hiding the data point: " + error);
            hideAlert.show();
        });
    });


    // Async form for submitting
    $("#submitAnnotation").click(function () {
        var annotForm = $('#annotForm');
        var annotText = annotForm.find('textarea[name="text"]').val();

        $.ajax({
            type: 'POST',
            url: "/service/new-annotation",
            data: annotForm.serialize(),
            success: function (response) {
                var metricId = annotForm.find('input[name="metricCode"]').val();
                var nthView = dyViewsByCode[metricId];
                var nthx = getXaxisNseriesById(data, annotForm.find('input[name="dbId"]').val());
                // Update the collection of annotations. We could do this faster, but this is robust
                annotCollection = getAnnotationCollection();
                var annotArr = buildCollection(annotCollection, data, metricId);
                views[nthView].dygraph.setAnnotations(annotArr);
            },
            error: function () {
                alert("There was an error submitting this annotation comment!");
            }
        });
        annotForm.find('textarea[name="text"]').val("");
        $('#annotFormDiv').hide();
    });

    //Looks at the buttons and filters rows & columns based on selection
    updateAllViews(data);
    $("body").tooltip({selector: '[data-toggle="tooltip"]'});


    $("#reportButton").click(function (event) {
        if (selectedInstruments.length > 1) {
            alert("Cannot produce a report for multiple instruments simultaneously. Select a single instrument and try again");
            return;
        }

        var doc = new jsPDF("portrait", "in", "letter");

        // First pass - just count pages
        var numPages = drawGraphsToReport(data, viewMetadata, doc, -1);

        // Second pass - actually make a .pdf
        doc = new jsPDF("portrait", "in", "letter");
        drawGraphsToReport(data, viewMetadata, doc, numPages);
        doc.save('report.pdf');
    });
}

function getAnnotationCollection() {
    var jsonData = null;
    $.ajax({
        dataType: "json",
        async: false,
        url: "/service/list-annotation.json",
        success: function (response) {
            jsonData = response.quameterannotation;
        }
    });
    return jsonData;
}

function getHiddenAnnotationCollection() {
    var jsonData = null;
    $.ajax({
        dataType: "json",
        async: false,
        url: "/service/hidden-annotation.json",
        success: function (response) {
            jsonData = response.quameterannotation;
        }
    });
    return jsonData;
}

// We create separate columns for each of the instruments using the original metric.
// These columns have null for each instrument except the one that the data belongs to
//
// In case of the protein id columns, we do even more. We would set to null any value
// that does not match the category of the sample
// metricId - name of the metric to be used
function getSmartColumns(metricId, data) {
    var dataIdx = columnIndex("startTime", data);
    var cols = [dataIdx];
    var rawInstrumentNames = activeInstrumentFilters();

    function getValueExtractionFunction(iterJ, metID, metricIndex, instrumentCol) {
        if (metID.match(/^id_/)) {
            // The id_ columns are special. We do extra filtering on null values
            return function (dt, row) {
                if (dt.getValue(row, instrumentCol) === rawInstrumentNames[iterJ]) {
                    var value = dt.getValue(row, metricIndex);
                    return value === 0 ? null : value;
                }
                return null;
            };
        } else {
            return function (dt, row) {
                return (dt.getValue(row, instrumentCol) === rawInstrumentNames[iterJ]) ? dt.getValue(row, metricIndex) : null;
            };
        }
    }

    for (var j = 0; j < rawInstrumentNames.length; j++) {
        var instrumentCol = columnIndex("instrument", data);
        var metricIndex = columnIndex(metricId, data);

        cols.push({
            type: 'number', label: rawInstrumentNames[j],
            calc: (getValueExtractionFunction)(j, metricId, metricIndex, instrumentCol)
        });
    }
    return cols;
}


/*
 * Color the lines by instrument
 */
function getSmartColors() {
    var colors = [];
    instrumentButtons().each(function () {
        if (selectedInstruments.contains($(this).val())) {
            colors.push($(this).css('background-color'));
        }
    });
    return colors;
}

function setZoomMonths(months) {
    var d = new Date();
    var maxDate = d.getTime();
    var minDate = maxDate - months * 31 * 24 * 60 * 60 * 1000; // We use 31-day months to always report same window
    for (var gIndex in gs) {
        if (gs.hasOwnProperty(gIndex)) {
            var g = gs[gIndex];
            g.updateOptions({
                dateWindow: [minDate, maxDate]
            }, false);
        }
    }
}

function findRowForId(data, id) {
    var idColumn = columnIndex("id", data);
    for (var i = 0; i < data.getNumberOfRows(); i++) {
        if (data.getValue(i, idColumn).toString() === id.toString()) {
            return i;
        }
    }
    return -1;
}

function findMetricByCode(code) {
    for (var i = 0; i < metrics.length; i++) {
        var metric = metrics[i];
        if (metric.code === code) {
            return i;
        }
    }
    return -1;
}
