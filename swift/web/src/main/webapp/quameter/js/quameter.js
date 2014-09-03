/** Global Vars to hack Google DataViews **/
rawInsturmentNames = [];
views = [], gs = [];
numberOfSimpleGraphs = 0;
dyViewsByCode = {};
viewMetadata = {};


function populateInstArray(dt) {
    for (var r = 0; r < dt.getNumberOfRows(); r++) {
        // Column 4 = Instrument Name
        var val = dt.getValue(r, 4);
        if (!rawInsturmentNames.contains(val)) {
            rawInsturmentNames.push(val);
        }
    }
}

function columnIndex(id, data) {
    for (var i = 0; i < data.getNumberOfColumns(); i++) {
        if (data.getColumnId(i) === id) {
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
    var keys = $.map(names, function (element, index) {
        return index
    }).sort();

    var iter = 1;
    $.each(keys, function (index, value) {
        var niceName = value;
        var btnClass = 'btn-orig' + iter;
        if (!niceName.match(/^Orbi/) && columnId === 'instrument') {
            btnClass = 'btn-default';
        }
        else if (columnId !== 'instrument') {
            var btnClass = 'btn-primary';
        }
        div.append('<button type="button" class="btn ' + btnClass + '" value="' + value + '" data-num=' + iter + ' data-enum="' + columnId + '">' + niceName + ' (' + names[value] + ')<' + '/button>');
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

function createNewAnnotationForm(parentName, dbID) {
    var metricCode = parentName.split("-")[1];
    $('#hiddenMetricCode').val(metricCode);
    $('#hiddenRowid').val(dbID);
    $('#annotFormDiv').show();
}

function getXaxisNseriesById(data, dbId) {
    for (var i = 0; i < data.getNumberOfRows(); i++) {
        if (data.getValue(i, columnIndex("id", data)).toString() === dbId.toString()) {
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
                )
            }
        }
    }
    return arrayForDYgraphs;
}

function getMetricTitle(n) {
    var hLink, qLink;
    if (metrics[n].hasOwnProperty('link')) {
        hLink = 'href="' + metrics[n].link + '" target="_blank"';
        qLink = '&nbsp; <i class="icon-large icon-question-sign"></i>';
    }
    else {
        hLink = 'href="#"';
        qLink = '';
    }
    return '<a ' + hLink + ' data-toggle="tooltip" data-placement="right" title="' + metrics[n].desc + '" class="modLink" >' + metrics[n].name + qLink + '</a>'
}

// Tokenize by underscore, wrap tokens in <span>
function spanAllUnderscoreTokens(s) {
    return s.split("_").map(function (item) {
        return '<span>' + item + '</span>'
    }).join('_');
}


function activeCatagoriesFilters() {
    var selectedCategory = [];
    categoryButtons().each(function () {
        var wrapperId = "#wrapper-" + $(this).attr("value"); //for protein id, hide entire graph when excluded
        if ($(this).hasClass('btn-primary')) {
            selectedCategory.push($(this).attr("value"));

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
    return selectedCategory;
}

function activeInstrumentFilters() {
    var selectedCategory = [];
    instrumentButtons().each(function () {
        if (!$(this).hasClass('btn-default')) {
            selectedCategory.push($(this).attr("value"));
        }
    });
    return selectedCategory;
}

// Callback that filters all the views, updating the stdev ranges
function updateAllViews(data) {
    blockRedraw = true;
    // We deselect the user-selected point
    pointSelected = -1;
    pointHighlighted = -1;

    activeCats = activeCatagoriesFilters();

    var filteredRows = [];
    for (var r = 0; r < data.getNumberOfRows(); r++) {
        var category = data.getValue(r, columnIndex('category', data));
        var rowId = data.getValue(r, columnIndex("id", data))
        if (activeCats.contains(category) && !hiddenIds["id" + rowId]) {
            filteredRows.push(r)
        }
    }

    viewMetadata.filteredRows = filteredRows;

    for (var i = 0; i < views.length; i++) {
        if (views[i] === undefined) {
            console.log(i);
            continue;
        } //empty for detail graphs until generated
        dyViewsByCode[views[i].metricId] = i;

        views[i].dataView.setColumns(getSmartColumns(columnIndex("startTime", data), views[i].metricId));
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
        views[i].minHighlightY2 = average - 5 * stdev;
        views[i].maxHighlightY2 = average + 5 * stdev;


        views[i].dygraph.updateOptions({file: views[i].dataView, valueRange: getMetricByCode(views[i].metricId).range, colors: getSmartColors()});

    }
    blockRedraw = false;
}

function selectPoint(data, dataRow) {
    if (dataRow == -1) {
        $('#icons').hide();
        selectedTransaction = -1;
        selectedId = -1;
    } else {
        var transactionColumnIndex = columnIndex("transaction", data);
        selectedTransaction = data.getValue(dataRow, transactionColumnIndex);
        selectedId = data.getValue(dataRow, columnIndex("id", data));
        $('#search-link').attr("href", '/start/?load=' + selectedTransaction);
        $('#qa-link').attr("href", '/service/qa/' + selectedTransaction + "/index.html");
        $('#icons').show();
    }
}

// var blockRedraw = false;

function addDygraph(viewIndex, view, viewId, metricId, viewMetadata, data, range, annotCollection) {
    views[viewIndex] = { dataView: view, minHighlightY: 1, maxHighlightY: -1, minHighlightY2: 1, maxHighlightY2: -1, metricId: metricId };
    var currentView = views[viewIndex];
    var selectedPath = $('#selected-path');

    var instrumentText = $.map(instrumentButtons(), function (val, i) {
        return val.firstChild.data
    });
    instrumentText.unshift("Date");

    // Row - the row in the original dataset
    function highlightRow(row) {
        if (row == -1) {
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
            drawCallback: function (me, initial) {
                if (blockRedraw || initial) return;
                blockRedraw = true;
                var range = me.xAxisRange();
                for (var j = 0; j < views.length; j++) {
                    if (gs[j] == me) continue;
                    if (gs[j] === undefined) {
                        console.log("Errant LOOKUP", j, gs)
                    }
                    gs[j].updateOptions({
                        dateWindow: range
                    });
                }
                blockRedraw = false;
            },
            //drawPoints: true,
            connectSeparatedPoints: true,
            pointSize: 2,
            strokeWidth: 0.7,
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
                    var bottom2 = g.toDomYCoord(currentView.minHighlightY2);
                    var top2 = g.toDomYCoord(currentView.maxHighlightY2);

                    var good = metrics[metricIndex].good;
                    canvas.fillStyle = "rgba(255, 235, 235, 1.0)";
                    if (good == "range" || good == "low") {
                        canvas.fillRect(area.x, area.y, area.w, top);
                    }
                    if (good == "range" || good == "high") {
                        canvas.fillRect(area.x, bottom, area.w, area.h - (bottom - area.y));
                    }
                    canvas.fillStyle = "rgba(250, 189, 189, 1.0)";
                    if (good == "range" || good == "low") {
                        canvas.fillRect(area.x, area.y, area.w, top2);
                    }
                    if (good == "range" || good == "high") {
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

                createNewAnnotationForm(event.target.parentNode.parentNode.id, data.getValue(row, columnIndex("id", data)));

                dygraph.updateOptions({file: currentView.dataView});
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
    var annotCollection = getAnnotationCollection();
    if (renderDetailGraphs) {
        viewIndex = numberOfSimpleGraphs - 1
    }
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
            if (renderDetailGraphs) {
                $('<h3>' + metricCategories[categoryCode] + '</h3>').appendTo("#detailedGraphs");
            }
            else {
                $('<h3>' + metricCategories[categoryCode] + '</h3>').appendTo("#simpleGraphs");
            }
            previousCategory = categoryCode;
        }

        var view = new google.visualization.DataView(data);

        view.setColumns(getSmartColumns(columnIndex("startTime", data), metricId));

        if (renderDetailGraphs) {
            var viewId = "graph-" + metricId;
            $('<div id="wrapper-' + metric.label + '" class="row-fluid"><div class="span12">' +
                    getMetricTitle(i) +
                    '<div id="' + viewId + '" class="simple-graph"></div>' +
                    '</div></div>'
            ).appendTo("#detailedGraphs");
            addDygraph(viewIndex, view, viewId, metricId, viewMetadata, data, metric.range, annotCollection);
            viewIndex++;
        }
        else {
            if (1 == metric.simple) {
                var viewId = "simpleGraph-" + metricId;
                $('<div id="wrapper-' + metric.label + '" class="row-fluid">' +
                        '<div class="span12">' +
                        getMetricTitle(i)
                        + '<div id="' + viewId + '" class="simple-graph"></div>' +
                        '</div></div>'
                ).appendTo("#simpleGraphs");
                addDygraph(viewIndex, view, viewId, metricId, viewMetadata, data, metric.range, annotCollection);
                viewIndex++;
            }
        }
    }
    if (!renderDetailGraphs) {
        numberOfSimpleGraphs = viewIndex
    }
}


/** INIT() draws only visible **/
function initSimpleCharts(graphObj) {
    // Create the data table. 
    var data = new google.visualization.DataTable(graphObj, 0.6);
    populateInstArray(data);

    // Populate array with index to every row
    var allRows = Array(data.getNumberOfRows());
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
    var filterButtons = $.merge(categoryButtons(), instrumentButtons());
    filterButtons.click(function (event) {
        var current = $(this);
        if (!event.shiftKey) {
            current.siblings().each(function () {
                var classList = $(this)[0].className.split(' ');
                if (classList[1] !== "btn-default") {
                    $(this).removeClass(classList[1]);
                    $(this).addClass("btn-default");
                }
            });
        }
        current.removeClass("btn-default");

        if (current[0].dataset.enum === 'instrument') {
            current.toggleClass('btn-orig' + current[0].dataset.num);
        } else {
            current.toggleClass("btn-primary");
        }
        updateAllViews(data);
    });


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

    //Little Hide Icon, when point on a graph is selected
    $('#hide-entry').click(function (event) {
        event.stopPropagation();
        $.post("/service/quameter-hide/" + selectedId);
        hiddenIds["id" + selectedId] = true;
        selectPoint(data, -1);
        updateAllViews(data);
    });


    // Async form for submitting
    $("#submitAnnotation").click(function () {
        $.ajax({
            type: 'POST',
            url: "/service/new-annotation",
            data: $('#annotForm').serialize(),
            success: function (response) {
                var nthView = dyViewsByCode[$('#annotForm').find('input[name="metricCode"]').val()];
                var nthx = getXaxisNseriesById(data, $('#annotForm').find('input[name="dbId"]').val());
                var txt = $(this).closest('form').find("input[type=text], textarea").val();
                var annotations = views[nthView].dygraph.annotations();
                annotations.push(
                    {
                        series: nthx[1],
                        x: nthx[0].toString(),
                        shortText: "A",
                        text: txt
                    }
                );
                views[nthView].dygraph.setAnnotations(annotations);
            },
            error: function () {
                alert("There was an error submitting this annotation comment!");
            }
        });
        $(this).closest('form').find("input[type=text], textarea").val("");
        $('#annotFormDiv').hide();
    });

    //Looks at the butons and filters rows & columns based on selection
    updateAllViews(data);
}


function getAnnotationCollection() {
    var jsonData;
    $.ajax({
        dataType: "json",
        async: false,
        url: "/service/list-annotation.json",
        success: function (response) {
            jsonData = response['quameterannotation'];
        }
    });
    return jsonData;
}


function getSmartColumns(dataIdx, metricId) {
    var cols = [ dataIdx ];
    var rawInsturmentNames = activeInstrumentFilters();
    for (j = 0; j < rawInsturmentNames.length; j++) {
        cols.push({type: 'number', label: rawInsturmentNames[j],
            calc: (function (iterJ, metID) {
                return function (dt, row) {
                    return (dt.getValue(row, 4) === rawInsturmentNames[iterJ]) ? dt.getValue(row, columnIndex(metID, dt)) : null;
                }
            })(j, metricId)
        });
    }
    ;
    return cols;
}


/*
 * Color the lines by instrument
 */
function getSmartColors() {
    var colors = [];
    instrumentButtons().each(function () {
        if (!$(this).hasClass('btn-default')) {
            colors.push($(this).css('background-color'));
        }
    });
    return colors;
}

