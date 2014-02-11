<!DOCTYPE html>
<%@ page import="edu.mayo.mprc.MprcException" %>
<%@ page import="edu.mayo.mprc.config.ResourceConfig" %>
<%@ page import="edu.mayo.mprc.quameterdb.QuameterUi" %>
<%@ page import="edu.mayo.mprc.swift.MainFactoryContext" %>
<%@ page import="edu.mayo.mprc.swift.SwiftWebContext" %>
<%@ page import="java.io.StringWriter" %>
<% final ResourceConfig quameterUiConfig = MainFactoryContext.getSwiftEnvironment().getSingletonConfig(QuameterUi.Config.class); %>
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

        .graph {
            width: 100%;
            height: 320px;
            margin-bottom: 10px;
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

            function col(id) {
                for (var i = 0; i < data.getNumberOfColumns(); i++) {
                    if (data.getColumnId(i) == id) {
                        return i;
                    }
                }
                alert("Column " + id + " does not exist");
            }

            views = [
                { "view": "tails",
                    "columns": ["c_1a", "c_1b"]
                },
                { "view": "retention",
                    "columns": ["c_2a", "duration"]
                },
                { "view": "idRate",
                    "columns": ["c_2b"]
                },
                { "view": "peakBroadening",
                    "columns": ["c_3a", "c_3b", "c_4a", "c_4b", "c_4c"]
                },
                { "view": "oversampling",
                    "columns": ["ds_1a", "ds_1b"]
                },
                { "view": "samplingRate",
                    "columns": ["ds_2a", "ds_2b"]
                },
                { "view": "peakSampling",
                    "columns": ["ds_3a", "ds_3b"]
                },
                { "view": "electrospray",
                    "columns": ["is_1a", "is_1b"]
                },
                { "view": "tuning",
                    "columns": ["is_2"]
                },
                { "view": "chargeState",
                    "columns": ["is_3a", "is_3b", "is_3c"]
                },
                { "view": "ms1Time",
                    "columns": ["ms1_1"]
                },
                { "view": "ms1SigToNoise",
                    "columns": ["ms1_2a", "ms1_3a"]
                },
                { "view": "ms1Tic",
                    "columns": ["ms1_2b"]
                },
                { "view": "ms1PrecError",
                    "columns": ["ms1_5a", "ms1_5b"]
                },
                { "view": "ms1PrecErrorPpm",
                    "columns": ["ms1_5c", "ms1_5d"]
                },
                { "view": "ms2Time",
                    "columns": ["ms2_1"]
                },
                { "view": "ms2SigToNoise",
                    "columns": ["ms2_2"]
                },
                { "view": "ms2NumPeaks",
                    "columns": ["ms2_3"]
                },
                { "view": "ms2IdRatio",
                    "columns": ["ms2_4a", "ms2_4b", "ms2_4c", "ms2_4d"]
                },
                { "view": "searchScore",
                    "columns": ["p_1"]
                },
                { "view": "trypticMs2",
                    "columns": ["p_2a"]
                },
                { "view": "trypticPeptides",
                    "columns": ["p_2b"]
                },
                { "view": "distinctPeptides",
                    "columns": ["p_2c"]
                },
                { "view": "semiToTryptic",
                    "columns": ["p_3"]
                }
            ];

            var blockRedraw = false;
            for (var i = 0; i < views.length; i++) {
                var v = views[i];
                var view = new google.visualization.DataView(data);
                var cols = [];
                cols.push(col("startTime"));
                for (var j = 0; j < v.columns.length; j++) {
                    cols.push(col(v.columns[j]))
                }
                view.setColumns(cols);
                gs = [];
                gs.push(
                        new Dygraph(
                                document.getElementById(v.view),
                                view,
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
                                    }
                                }
                        ));
            }
        }
    </script>

</head>
<body>
<div class="container-fluid">
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
<h3>Chromatography</h3>

<div class="row-fluid">
    <div class="span3">
        <h4>Peak Tails</h4>

        <div id="tails" class="graph"></div>
        <div class="legend">
            <dl>
                <dt>C-1A <span>Bleed Ratio</span></dt>
                <dd>Fraction of peptides with repeat identifications >4 min earlier than identification closest to
                    the chromatographic maximum
                </dd>
                <dt>C-1B <span>Peak Tailing Ratio</span></dt>
                <dd>Fraction of peptides with repeat identifications >4 min later than identification closest to the
                    chromatographic maximum
                </dd>
            </dl>
        </div>
    </div>
    <div class="span3">
        <h4>Retention</h4>

        <div id="retention" class="graph"></div>
        <div class="legend">
            <dl>
                <dt>C-2A</dt>
                <dd>Retention time period over which the middle 50% of the identified peptides eluted (minutes)
                </dd>
                <dt>Duration</dt>
                <dd>Acquisition duration (minutes)
                </dd>

            </dl>
        </div>
    </div>

    <div class="span3">
        <h4>ID Rate</h4>

        <div id="idRate" class="graph"></div>
        <div class="legend">
            <dl>
                <dt>C-2B</dt>
                <dd>Rate of peptide identification during the C-2A time range
                </dd>
            </dl>
        </div>
    </div>

    <div class="span3">
        <h4>Peak Broadening</h4>

        <div id="peakBroadening" class="graph"></div>
        <div class="legend">
            <dl>
                <dt>C-3A</dt>
                <dd>Median identified peak width
                </dd>
                <dt>C-3B</dt>
                <dd>Interquantile range for peak widths
                </dd>

                <dt>C-4A</dt>
                <dd>Median peak width over <i>last 10%</i> of the elution time
                </dd>
                <dt>C-4B</dt>
                <dd>Median peak width over <i>first 10%</i> of the elution time
                </dd>
                <dt>C-4C</dt>
                <dd>Median peak width over <i>middle 10%</i> of the elution time
                </dd>
            </dl>
        </div>
    </div>


</div>

<h3>Dynamic Sampling</h3>

<div class="row-fluid">
    <div class="span4">
        <h4>Oversampling</h4>

        <div id="oversampling" class="graph"></div>
        <div class="legend">
            <dl>
                <dt>DS-1A</dt>
                <dd>Ratio of singly to doubly identified peptide ions. If no doubly identified, ratio reported as 1
                </dd>
                <dt>DS-1B</dt>
                <dd>Ratio of doubly to triply identified peptide ions. If no triply identified, ratio reported as 1
                </dd>
            </dl>
        </div>
    </div>

    <div class="span4">
        <h4>Sampling Rate</h4>

        <div id="samplingRate" class="graph"></div>
        <div class="legend">
            <dl>
                <dt>DS-2A</dt>
                <dd>Number of MS1 scans acquired during the C-2A time range
                </dd>
                <dt>DS-2B</dt>
                <dd>Number of MS2 scans acquired during the C-2A time range
                </dd>
            </dl>
        </div>
    </div>

    <div class="span4">
        <h4>Peak Sampling</h4>

        <div id="peakSampling" class="graph"></div>
        <div class="legend">
            <dl>
                <dt>DS-3A</dt>
                <dd>Median ratio of the maximum MS1 peak intensity over the MS1 intensity at the sampling time for all
                    identified peptides.
                    We want to capture peak at its apex.
                </dd>
                <dt>DS-3B</dt>
                <dd>Median ratio of the maximum MS1 peak intensity over the MS1 intensity at the sampling time for
                    peptides with peak intensity in bottom 50%.
                    We want to capture peak at its apex even for low-intensity peptides.
                </dd>
            </dl>
        </div>
    </div>
</div>

<h3>Ion Source</h3>

<div class="row-fluid">
    <div class="span4">
        <h4>Electrospray Instability</h4>

        <div id="electrospray" class="graph"></div>
        <div class="legend">
            <dl>
                <dt>IS-1A</dt>
                <dd>TIC dropped more than 10x in two consecutive MS1 scans (within the C-2A time range)
                </dd>
                <dt>IS-1B</dt>
                <dd>TIC jumped more than 10x in two consecutive MS1 scans (within the C-2A time range)
                </dd>
            </dl>
        </div>
    </div>

    <div class="span4">
        <h4>Tuning</h4>

        <div id="tuning" class="graph"></div>
        <div class="legend">
            <dl>
                <dt>IS-2</dt>
                <dd>Median precursor of identified peptide ions
                </dd>
            </dl>
        </div>
    </div>

    <div class="span4">
        <h4>Charge State</h4>

        <div id="chargeState" class="graph"></div>
        <div class="legend">
            <dl>
                <dt>IS-3A</dt>
                <dd>Ratio of 1+/2+ identified peptides
                </dd>
                <dt>IS-3B</dt>
                <dd>Ratio of 3+/2+ identified peptides
                </dd>
                <dt>IS-3C</dt>
                <dd>Ratio of 4+/2+ identified peptides
                </dd>

            </dl>
        </div>
    </div>
</div>


<h3>MS1 Signal</h3>

<div class="row-fluid">
    <div class="span4">
        <h4>MS1 Time</h4>

        <div id="ms1Time" class="graph"></div>
        <div class="legend">
            <dl>
                <dt>MS1-1</dt>
                <dd>Median injection time for MS1 spectra
                </dd>
            </dl>
        </div>
    </div>

    <div class="span4">
        <h4>MS1 Signal to Noise</h4>

        <div id="ms1SigToNoise" class="graph"></div>
        <div class="legend">
            <dl>
                <dt>MS1-2A</dt>
                <dd>Ratio of maximum to median signal in MS1 spectra
                </dd>
                <dt>MS1-3A</dt>
                <dd>Dynamic range - ratio of 95th and 5th percentile of MS1 maximum identities for identified peptides
                    in C-2A time range
                </dd>
            </dl>
        </div>
    </div>

    <div class="span4">
        <h4>MS1 Total Ion Current</h4>

        <div id="ms1Tic" class="graph"></div>
        <div class="legend">
            <dl>
                <dt>MS1-2B</dt>
                <dd>Median MS1 Total Ion Current
                </dd>
            </dl>
        </div>
    </div>
</div>

<div class="row-fluid">
    <div class="span4">
        <h4>MS1 Precursor Error</h4>

        <div id="ms1PrecError" class="graph"></div>
        <div class="legend">
            <dl>
                <dt>MS1-5A</dt>
                <dd>Median difference between the theoretical precursor m/z and the measured precursor m/z value as
                    reported in the scan header
                </dd>
                <dt>MS1-5B</dt>
                <dd>Mean absolute difference between the theoretical precursor m/z and the measured precursor m/z value
                    as reported in the scan header
                </dd>
            </dl>
        </div>
    </div>

    <div class="span4">
        <h4>MS1 Precursor Error (PPM)</h4>

        <div id="ms1PrecErrorPpm" class="graph"></div>
        <div class="legend">
            <dl>
                <dt>MS1-5C</dt>
                <dd>Median precursor mass error in PPM
                </dd>
                <dt>MS1-5D</dt>
                <dd>Interquartile range for mass error in PPM
                </dd>
            </dl>
        </div>
    </div>
</div>

<h3>MS2 Signal</h3>

<div class="row-fluid">
    <div class="span4">
        <h4>MS2 Time</h4>

        <div id="ms2Time" class="graph"></div>
        <div class="legend">
            <dl>
                <dt>MS2-1</dt>
                <dd>Median injection time for MS2 spectra
                </dd>
            </dl>
        </div>
    </div>

    <div class="span4">
        <h4>MS2 Signal to Noise</h4>

        <div id="ms2SigToNoise" class="graph"></div>
        <div class="legend">
            <dl>
                <dt>MS2-2</dt>
                <dd>Ratio of maximum to median signal in MS2 spectra
                </dd>
            </dl>
        </div>
    </div>

    <div class="span4">
        <h4>MS2 Number of Peaks</h4>

        <div id="ms2NumPeaks" class="graph"></div>
        <div class="legend">
            <dl>
                <dt>MS2-3</dt>
                <dd>Median number of MS2 peaks
                </dd>
            </dl>
        </div>
    </div>
</div>

<div class="row-fluid">
    <div class="span4">
        <h4>MS2 ID Ratio</h4>

        <div id="ms2IdRatio" class="graph"></div>
        <div class="legend">
            <dl>
                <dt>MS2-4(A-D)</dt>
                <dd>Fraction of MS2 scans identified in the 1st-4th quartile of peptides sorted by MS1 max intensity
                </dd>
            </dl>
        </div>
    </div>

    <div class="span4">
        <h4>Search Score</h4>

        <div id="searchScore" class="graph"></div>
        <div class="legend">
            <dl>
                <dt>P-1</dt>
                <dd>Median peptide ID score
                </dd>
            </dl>
        </div>
    </div>
</div>

<h3>MS2 - Tryptic Peptides</h3>

<div class="row-fluid">
    <div class="span3">
        <h4>Tryptic MS2</h4>

        <div id="trypticMs2" class="graph"></div>
        <div class="legend">
            <dl>
                <dt>P-2A</dt>
                <dd>Number of MS2 spectra identifying tryptic peptide ions
                </dd>
            </dl>
        </div>
    </div>
    <div class="span3">
        <h4>Tryptic Peptides</h4>

        <div id="trypticPeptides" class="graph"></div>
        <div class="legend">
            <dl>
                <dt>P-2B</dt>
                <dd>Number of tryptic peptide ions identified
                </dd>
            </dl>
        </div>
    </div>
    <div class="span3">
        <h4>Distinct Peptides</h4>

        <div id="distinctPeptides" class="graph"></div>
        <div class="legend">
            <dl>
                <dt>P-2C</dt>
                <dd>Number of distinct identified tryptic peptide sequences, ignoring modifications and charge state
                </dd>
            </dl>
        </div>
    </div>
    <div class="span3">
        <h4>Semitryptic Ratio</h4>

        <div id="semiToTryptic" class="graph"></div>
        <div class="legend">
            <dl>
                <dt>P-3</dt>
                <dd>Ratio of semitryptic/tryptic peptides
                </dd>
            </dl>
        </div>
    </div>
</div>
<% } %>
</div>

</body>
</html>