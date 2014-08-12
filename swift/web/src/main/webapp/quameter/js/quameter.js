/** Global Vars to hack Google DataViews **/
rawInsturmentNames=[];
var defaultSelectedInsturmentNames=['01475B']; // By Default only Show Orbi
var views = [];

function populateInstArray(dt){
    for(var r=0; r < dt.getNumberOfRows(); r++) {
        // Column 4 = Instrument Name
        var val = dt.getValue(r, 4);
        if ( ! rawInsturmentNames.contains(val)  ) {
            rawInsturmentNames.push(val);
        }
    }
}

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
    var keys = $.map(names, function (element, index) {
        return index
    }).sort();

    $.each(keys, function (index, value) {
        var btnClass = 'btn-primary';
//        if(columnId === 'instrument' && !defaultSelectedInsturmentNames.contains(value)){
//            btnClass = 'btn-default';
//        }
        div.append('<button type="button" class="btn '+btnClass+' value="' + value + '">' + getNiceName(value) + ' (' + names[value] + ')<' + '/button>');
    });
}

function getMetricTitle(n){
    var hLink, qLink;
    if(metrics[n].hasOwnProperty('link')){
        hLink = 'href="'+metrics[n].link+'" target="_blank"';    
        qLink = '&nbsp; <i class="icon-large icon-question-sign"></i>';    
    }
    else{
        hLink = 'href="#"';    
        qLink = '';    
    }
    return '<a '+hLink+' data-toggle="tooltip" data-placement="right" title="'+metrics[n].desc+'" class="modLink" >'+metrics[n].name+qLink+'</a>'
}

// Tokenize by underscore, wrap tokens in <span>
function spanAllUnderscoreTokens(s) {
    return s.split("_").map(function (item) {
        return '<span>' + item + '</span>'
    }).join('_');
}

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

        views[i].minHighlightY = average - 5 * stdev;
        views[i].maxHighlightY = average + 5 * stdev;

        views[i].dygraph.updateOptions({file: views[i].dataView});
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
        var idColumnIndex = columnIndex("id", data);
        selectedId = data.getValue(dataRow, idColumnIndex);
        $('#search-link').attr("href", '/start/?load=' + selectedTransaction);
        $('#qa-link').attr("href", '/service/qa/' + selectedTransaction + "/index.html");
        $('#icons').show();
    }
}


function addDygraph( viewIndex, view, viewId, metricId, viewMetadata, data, pathColumnIndex, selectedPath, instrumentButtons, categoryButtons, instrumentColumnIndex, categoryColumnIndex) {
    views[viewIndex] = { dataView: view, minHighlightY: 1, maxHighlightY: -1, metricId: metricId };
    var currentView = views[viewIndex];

    var instrumentText=  $.map( instrumentButtons, function( val, i ) { return val.firstChild.data });
    instrumentText.unshift("Date");
    //console.log( instrumentText ); //

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

 //   console.log(viewId);
 //   console.log(metricId);
 //   console.log(views[viewIndex].dataView);
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
                connectSeparatedPoints: true,
                //labels: instrumentText,
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


function drawChart(graphObj) {
    // Create the data table. 
    var data = new google.visualization.DataTable( graphObj, 0.6 );
    //var table2 = new google.visualization.Table(document.getElementById('table2'));
    populateInstArray(data);

    var viewMetadata = {};
    var allRows = Array(data.getNumberOfRows());
    for (var i = 0; i < data.getNumberOfRows(); i++) {
        allRows[i] = i;
    }
    viewMetadata.filteredRows = allRows;
    var views = [];
    gs = [];

    function col(id) {
        return columnIndex(id, data);
    }

    selectPoint(data, -1);

    var selectedPath = $('#selected-path');
    var pathColumnIndex = col('path');

    // Make buttons
    var categoryDiv = $('#category-buttons');
    addButtons(categoryDiv, data, 'category');

    var instrumentDiv = $('#instrument-buttons');
    addButtons(instrumentDiv, data, 'instrument');

    var categoryButtons = categoryDiv.find('.btn');
    var categoryColumnIndex = col('category');
    var instrumentButtons = instrumentDiv.find('.btn');
    var instrumentColumnIndex = col('instrument');
    var idColumnIndex = col('id');

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
        var cols = [ col("startTime") ];

        for(j=0; j<rawInsturmentNames.length; j++){
            cols.push({type:'number', label: getNiceName(rawInsturmentNames[j]),
                calc: (function (iterJ, metID) {
                    return function (dt, row) {
                        return (dt.getValue(row, 4) === rawInsturmentNames[iterJ]) ?  dt.getValue(row, col(metID)) : null;                    }
                })(j,metricId)
            });
        };

        view.setColumns(cols);

       // var myView = google.visualization.DataView.fromJSON(data, view.toJSON())

        var viewId = "graph-" + metricId;
        $('<div class="row-fluid"><div class="span12">' +
                getMetricTitle(i)
                + '<div id="' + viewId + '" class="simple-graph"></div>' +
                '</div></div>')
                .appendTo("#detailedGraphs");
        viewIndex = addDygraph(
                 viewIndex, view, viewId, metricId, viewMetadata, data, pathColumnIndex, selectedPath, instrumentButtons, categoryButtons, instrumentColumnIndex, categoryColumnIndex);

        if (1 == metric.simple) {
            viewId = "simpleGraph-" + metricId;
            $('<div class="row-fluid">' +
                    '<div class="span12">' +
                    getMetricTitle(i)
                    + '<div id="' + viewId + '" class="simple-graph"></div>' +
                    '</div></div>')
                    .appendTo("#simpleGraphs");
            viewIndex = addDygraph(
                     viewIndex, view, viewId, metricId, viewMetadata, data, pathColumnIndex, selectedPath, instrumentButtons, categoryButtons, instrumentColumnIndex, categoryColumnIndex);
        }
    }

    blockRedraw = false;

    function refilterRows() {

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

         function filterRows() {
             var filteredRows = [];
             for (var row = 0; row < data.getNumberOfRows(); row++) {
                 var category = data.getValue(row, categoryColumnIndex);
                 var instrument = data.getValue(row, instrumentColumnIndex);
                 var rowId = data.getValue(row, idColumnIndex)
                 //var prePost = prePostCategory(data.getValue(row, prePostColumnIndex));
                 if (0 <= $.inArray(category, selectedCategory)
                         && 0 <= $.inArray(instrument, selectedInstrument)
                        // && 0 <= $.inArray(prePost, selectedPrePost)
                         && !hiddenIds["id"+rowId]) {
                     filteredRows.push(row);
                 }
             }
             return filteredRows;
         }

         var filteredRows = filterRows();

         viewMetadata['filteredRows'] = filteredRows;
         updateAllViews(views, filteredRows);
    }

    $('.btn').button();
    var allButtons = $.merge(categoryButtons,instrumentButtons);
    allButtons.click(function (event) {
        var current = $(this);

        if (!event.shiftKey) {
            current.siblings().removeClass("btn-primary");
            current.removeClass("btn-primary");
        }

        current.toggleClass("btn-primary");

        refilterRows();
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

    $('#hide-entry').click(function(event) {
        event.stopPropagation();
        $.post("/service/quameter-hide/" + selectedId);
        hiddenIds["id"+selectedId] = true;
        selectPoint(data, -1);
        refilterRows();
      });

    var allRowsIndex = $.map($(Array(data.getNumberOfRows())),function(val, i) { return i; }) // Create dummy array to display thresholds for all values on init()
    updateAllViews(views, allRowsIndex);
    $('#detailedGraphs').css("display", "none");
}


