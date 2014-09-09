// When true, redraws of graphs will not trigger more redraws
var blockRedraw = false;

// Row of the selected point
var pointSelected = -1;

// Row of the highlighted point
var pointHighlighted = -1;

// Transaction currently selected
var selectedTransaction = -1;

// ID of the Quameter result currently selected
var selectedId = -1;

// Array that contains true for every quameter result id that is to be hidden
var hiddenIds = new Object();

//Place holder to determine if Detailed graphs are drawn yet.
var detailsExist = false;

//Global list of ids that should NOT be displayed
var hiddenIds = {};

//Categories for Drawing graphs in series
var metricCategories = {
        c: "Chromatography",
        ds: "Dynamic Sampling",
        is: "Ion Source",
        ms1: "MS1 Signal",
        ms2: "MS2 Signal",
        p: "Protease",
        id: "Protein ID"
    };

function getMetricByCode(cc){
    for(m in metrics){
        if(metrics[m].code === cc){
            return metrics[m];
        }
    }
}


/**
 * Array.prototype.[method name] allows you to define/overwrite an objects method
 * returns true if element is in the array, and false otherwise
 */
Array.prototype.contains = function ( element ) {
    for (i in this) {
        if (this[i] === element) return true;
    }
    return false;
}