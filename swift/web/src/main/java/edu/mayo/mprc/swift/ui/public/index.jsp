<%@ page import="edu.mayo.mprc.swift.SwiftWebContext" %>
<html>
<head>
    <title>New Swift Search</title>
    <meta name='gwt:module' content='edu.mayo.mprc.swift.ui.SwiftApp'>
    <meta HTTP-EQUIV="Content-Type" CONTENT="text/html; charset=utf-8">
    <meta Http-Equiv="Cache-Control" Content="no-cache">
    <link rel=stylesheet href="DBCurator.css" media="all">
    <link rel="stylesheet" href="SwiftApp.css" media="all">
    <link rel="stylesheet" href="/common/topbar.css" media="all">
    <link rel="stylesheet" href="filechooser/style.css" media="all">

    <script type="text/javascript" src="start.nocache.js"></script>
    <script type="text/javascript" src="filechooser/jquery-1.7.min.js"></script>
    <script type="text/javascript" src="filechooser/cookies.js"></script>
    <script type="text/javascript" src="filechooser/expanded.js"></script>
    <script type="text/javascript" src="filechooser/dialog.js"></script>
    <script type="text/javascript" src="filechooser/startup.js"></script>
</head>
<body>
<iframe src="javascript:''" id='__gwt_historyFrame' style='position:absolute;width:0;height:0;border:0'></iframe>
<div class="topbar">
    <span class="logo-small"><%=SwiftWebContext.getWebUi().getTitle()%></span>
    <ul class="locations">
        <li class="active-tab"><a href="/start">New search</a></li>
        <li><a href="/report/report.jsp">Existing searches</a></li>
        <li><a href="/">About Swift</a></li>
        <li><a href="/quameter">QuaMeter</a></li> <!-- TODO - make optional -->
        <li><a href="/extras">Extras</a></li>
    </ul>
</div>
<div id="contents">
    <!-- this div will disappear when the page gets properly loaded -->
    <div id="shown_while_loading">
        <div class="loader-anim"></div>
    </div>
    <!-- this div will appear when the page gets properly loaded -->
    <div id="hidden_while_loading">
        <div id="messagePlaceholder">
        </div>
        <table id="maintable">
            <tbody>
            <tr class="shrink">
                <th class="leftcol" colspan="2">Title<span class="required">*</span>:</th>
                <td>
                    <div id="title" style="width: 100%"></div>
                </td>
                <th class="leftcol">User<span class="required">*</span>:</th>
                <td>
                    <div id="email"></div>
                </td>
                <td class="rightcol" colspan="2">
                    <div class="warning" id="titleWarning"></div>
                </td>
            </tr>
            <tr class="shrink">
                <th class="leftcol" colspan="2">Output<span class="required"></span>:</th>
                <td colspan="3">
                    <div id="output"></div>
                </td>
                <td class="rightcol" colspan="2">
                    <div class="warning" id="outputWarning"></div>
                </td>
            </tr>

            <tr class="shrink">
                <td class="params-border-verytop"></td>
                <th class="leftcol" valign="top" rowspan="2">
                    <div id="paramsToggle">
                        <div id="paramsToggleButton"></div>
                        Parameters<span class="required">*</span>:
                    </div>
                </th>
                <td id="paramsSelector" rowspan="2" colspan="3"></td>
                <td id="globalParamsValidation" class="rightcol" rowspan="2"></td>
                <td class="params-border-verytop"></td>
            </tr>

            <tr class="shrink">
                <td class="params-border-top-left"></td>
                <!-- rowspan -->
                <!-- rowspan -->
                <!-- rowspan -->
                <!-- rowspan -->
                <!-- rowspan -->
                <td class="params-border-top-right"></td>
            </tr>
            </tbody>

            <tbody>
            <tr id="paramDbRow" class="shrink">
                <td id="paramDbLabel" colspan="2" class="params-row-left"></td>
                <td id="paramDbEntry" colspan="3"></td>
                <td id="paramDbValidation" class="rightcol params-row-right" colspan="2"></td>
            </tr>
            </tbody>

            <!-- the following row will get cloned for each param item. -->
            <tbody>
            <tr id="paramRow" class="shrink">
                <td id="paramLabelLeftCol" colspan="2" class="params-row-left"></td>
                <td id="paramEntryLeftCol"></td>
                <td id="paramLabelRightCol"></td>
                <td id="paramEntryRightCol"></td>
                <td id="paramValidation" class="rightcol params-row-right" colspan="2"></td>
            </tr>
            </tbody>

            <tbody>
            <tr id="scaffoldRow" class="shrink">
                <td id="scaffoldLabel" colspan="2" class="params-row-left"></td>
                <td id="scaffoldEntry" colspan="3"></td>
                <td id="scaffoldValidation" class="rightcol params-row-right" colspan="2"></td>
            </tr>
            </tbody>

            <tbody>
            <tr id="enginesRow" class="shrink">
                <td id="enginesLabel" colspan="2" class="params-row-left"></td>
                <td id="enginesEntry" colspan="3"></td>
                <td id="enginesValidation" class="rightcol params-row-right" colspan="2"></td>
            </tr>
            </tbody>

            <tbody>

            <tr>
                <td class="params-border-bottom" colspan="7"></td>
            </tr>

            <tr class="shrink" id="parameterEditorDisabledMessage">
                <td colspan="7">
                    <div class="warning-message">
                        The parameter editor is disabled.
                        Please select user with right to edit parameters (see the User: field above).
                    </div>
                </td>
            </tr>

            <tr class="shrink" id="spectrumQaRow" style="display:none;">
                <th class="leftcol" colspan="2">QA:</th>
                <td colspan="3">
                    <div id="spectrumQa"></div>
                </td>
                <td class="rightcol" colspan="2">
                    <div class="warning" id="spectrumQaWarning"></div>
                </td>
            </tr>

            <tr class="shrink" id="reportingRow" style="display:none;">
                <th class="leftcol" colspan="2">Reporting:</th>
                <td colspan="3">
                    <div id="report"></div>
                </td>
                <td class="rightcol" colspan="2">
                    <div class="warning" id="reportWarning"></div>
                </td>
            </tr>

            <tr class="shrink" id="publicMgfsRow">
                <th class="leftcol" colspan="2">Additional:</th>
                <td colspan="3">
                    <div id="publicResults"></div>
                </td>
                <td class="rightcol" colspan="2">
                    <div class="warning" id="publicMgfsWarning"></div>
                </td>
            </tr>

            </tbody>
        </table>

        <div id="fileTable"></div>
        <br clear="all"/>

        <div id="addFileButton" class="addFileButton"></div>
        <div class="bottom-bar">
            <div id="runButton" class="runButton"></div>
            <br clear="all"/>
        </div>

        <div id="dbCuratorDisabled"></div>
    </div>
</div>
</body>
</html>
