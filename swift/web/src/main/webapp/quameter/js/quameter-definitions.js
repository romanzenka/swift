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

// var defaultSelectedInsturmentNames=['01475B']; // By Default only Show Orbi

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
 
function getNiceName( str ){   
    var niceNames = {
        "01475B" : "Orbi",
        "Exactive Serie 3093" : "QE1",
        "Q Exactive Plus 3093" : "QE1",
        "LTQ30471" : "LTQ-Velos"
    }
    if(str in niceNames){
        return niceNames[str];
    }
    else{
        return str;
    }
}

/// Peak width y range (0 to 30sec) & peak spread
/// Duration y range (0 60 sec)

var metrics = [
        { code: "c_1a", label: "C-1A", name: "Bleed Ratio", good: "low", simple: 0, desc: "Fraction of peptides with repeat identifications >4 min earlier than identification closest to the chromatographic maximum" },
        { code: "c_1b", label: "C-1B", name: "Peak Tailing Ratio", good: "low", simple: 0, desc: "Fraction of peptides with repeat identifications >4 min later than identification closest to the chromatographic maximum" },
        { code: "c_2a", label: "C-2A", name: "Retention Window", good: "high", simple: 0, link: 'help/retention_spread.html', desc: "Retention time period over which the middle 50% of the identified peptides eluted (minutes)" },
        { code: "duration", label: "Duration", name: "Duration", good: "range", simple: 0, desc: "Acquisition duration (minutes)" },
        { code: "c_2b", label: "C-2B", name: "ID Rate", good: "high", simple: 1, link: 'help/peptides_per_minute.html', desc: "Rate of peptide identification during the C-2A time range" },
        { code: "c_3a", label: "C-3A", name: "Peak Width", good: "low", simple: 1, range: [0,40], link: 'help/peak_width.html', desc: "Median identified peak width" },
        { code: "c_3b", label: "C-3B", name: "Peak Width Spread", good: "low", simple: 1, range: [0,40], link: 'help/peak_width_variability.html', desc: "Interquantile range for peak widths" },
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
        { code: "p_3", label: "P-3", name: "Semitryptic Ratio", good: "low", simple: 1, desc: "Ratio of semitryptic/tryptic peptides" },
        { code: "id_1", label: "ID-1", name: "Identified Spectra", good: "low", simple: 1, desc: "Number of identified spectra matching requested proteins for given category" }
    ];

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