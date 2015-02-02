<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <title>Search Status | ${title}
    </title>
    <!--[if IE]>
    <style type="text/css">
        .opacityZero {
            filter: alpha(opacity=0);
        }
    </style>
    <![endif]-->

    <script type="text/javascript">
        // This is a prefix that has to be removed from the files in order to map the web paths
        var pathPrefix = "${pathPrefix}";

        // How does the raw file root map to the web browser? Idea is that you strip pathPrefix from the path,
        // prepend pathWebPrefix instead and use the resulting URL in your browser
        var pathWebPrefix = "${pathWebPrefix}";
    </script>

    <link rel="stylesheet" href="/report/report.css">
    <script type="text/javascript" src="/start/filechooser/cookies.js"></script>
    <script type="text/javascript" src="/common/bootstrap/js/jquery_1.9.0.min.js"></script>
    <script type="text/javascript" src="/common/bootstrap/js/jquery.tmpl.1.1.1.js"></script>
    <script type="text/javascript" src="/report/updates.js"></script>
    <script type="text/javascript" src="/report/visualizers.js"></script>
    <script type="text/javascript" src="/report/filters.js"></script>
    <script type="text/javascript">
        window.test = ({total: 0});
    </script>

    <script type="text/javascript">
        function getQueryString() {
            var result = {}, queryString = location.search.substring(1), re = /([^&=]+)=([^&]*)/g, m;

            while (m = re.exec(queryString)) {
                result[decodeURIComponent(m[1])] = decodeURIComponent(m[2]);
            }

            return result;
        }

        // Displays given sparse array using a table
        function SimpleArrayDisplayer(array, parentElement, id, itemVisualizer) {
            this.array = array;
            var myself = this;
            this.array.onchange = function () {
                myself.update();
            };
            this.parentElement = parentElement;
            this.id = id;
            this.itemVisualizer = itemVisualizer;
        }

        SimpleArrayDisplayer.prototype.render = function () {
            for (var i = 0; i < this.array.total; i++) {
                var item = this.array.getItemById(i);
                var element = this.itemVisualizer.render(this.id + "_" + i, item, 'tbody');
                $(this.parentElement).append(element);
            }
        };

        SimpleArrayDisplayer.prototype.update = function () {
            removeChildrenExcept(this.parentElement, /noRemove/i);
            this.render();
        };

        SimpleArrayDisplayer.prototype.listExpandedItems = function () {
            var list = "";
            for (var i = 0; i < this.array.total; i++) {
                var item = this.array.getItemById(i);
                if (item.expanded)
                    list += item.id + ",";
            }
            return list.substr(0, list.length - 1);
        };
    </script>

    <script type="text/javascript">

        var filters;
        var filterManager;

        var user;
        var title;
        var instrument;

        function createFilters() {
            title = new FilterDropDown("title");
            title.addRadioButtons("sort", "order", ["Sort A to Z", "Sort Z to A", "Do not sort"], ["1", "-1", "0"], -1);
            title.addSeparator();
            title.addTextBox("filter", "where")
            title.addSeparator();
            title.addOkCancel();
            title.onSubmitCallback = function () {
                title.saveToCookies();
                ajaxRequest('load');
            };

            $('#popups').append(title.getRoot());

            user = new FilterDropDown("user");
            user.addRadioButtons("sort", "order", ["Sort A to Z", "Sort Z to A", "Sort by submission time"], ["1", "-1", "0"], 2);
            user.addSeparator();
            user.addText('Display:');
            user.addCheckboxes("filter", "where", ${usersJson}, true);
            user.addSeparator();
            user.addOkCancel();
            user.onSubmitCallback = function () {
                user.saveToCookies();
                ajaxRequest('load');
            };
            $('#popups').append(user.getRoot());

            instrument = new FilterDropDown("instrument");
            instrument.addCheckboxes("filter", "where", ${instrumentsJson}, true);
            instrument.addSeparator();
            instrument.addOkCancel();
            instrument.onSubmitCallback = function () {
                instrument.saveToCookies();
                ajaxRequest('load');
            };
            $('#popups').append(instrument.getRoot());

            var submission = new FilterDropDown("submission");
            submission.addRadioButtons("sort", "order", ["Sort newest to oldest", "Sort oldest to newest"], ["submission ASC", "submission DESC"], -1);
            submission.addSeparator();
            submission.addText('Display:');
            submission.addRadioButtons("filter", "where",
                    ["All", "Submitted today", "Submitted this week", "Older than 1 week"],
                    ['', 'submission<=1', 'submission<=7', 'submission>7'], 0);
            submission.addSeparator();
            submission.addOkCancel();
            $('#popups').append(submission.getRoot());

            var results = new FilterDropDown("results");
            results.addText("Sort by number of errors");
            results.addRadioButtons("sort", "order", ["Error free first", "Most errors first"], ["errors ASC", "errors DESC"], -1);
            results.addSeparator();
            results.addText("Sort by last error");
            results.addRadioButtons("sort", "order", ["Newest errors to oldest", "Oldest errors to newest"], ["errotime ASC", "errotime DESC"], -1);
            results.addSeparator();
            results.addText('Display:');
            results.addCheckboxes("filter", "where", [{"status='ok'": "Error free"}, {"status='warnings'": "Warnings"}, {"status='failures'": "Failures"}], true);
            results.addSeparator();
            results.addOkCancel();
            $('#popups').append(results.getRoot());

            filters = [
                new FilterButton('title', 'Title', title),
                new FilterButton('user', 'Owner', user),
                new FilterButton('submission', 'Submission', null /*submission*/),
                new FilterButton('duration', 'Duration', null /*duration*/),
                new FilterButton('instruments', 'Instruments', instrument),
                new FilterButton('actions', '', null /*actions*/),
                new FilterButton('results', 'Results', null /*results*/),
                new FilterButton('progress', 'Progress', null)
            ];
            filterManager = new FilterManager(filters);
        }

        function closeForm(evt) {
            $('#popupMask').css("display", 'none');
            for (var i = 0; i < filters.length; i++)
                if (filters[i].dropdown)
                    filters[i].dropdown.hide();
            evt.stopPropagation();
        }

        function ajaxRequest(action) {
            $.ajax({
                "url": "/report/reportupdate",
                "type": "GET",
                "data": {
                    action: action,
                    start: firstEntry,
                    count: listedEntries,
                    expanded: displayer.listExpandedItems(),
                    timestamp: window.timestamp,
                    userfilter: user.getRequestString(),
                    titlefilter: title.getRequestString(),
                    instrumentfilter: instrument.getRequestString(),
                    showHidden: showHidden
                },
                "dataType": "html"
            }).done(function (data) {
                eval(data);
            })
        }

        var timestamp = 0;

        var periodicalUpdate;
        var updateDelay = 60 * 1000;
        var queries = getQueryString();
        var listedEntries = queries['count'] == null ? 100 : queries['count'];
        var firstEntry = queries['start'] == null ? 0 : queries['start'];
        var showHidden = queries['showHidden'] == null ? 0 : queries['showHidden'];
        var displayer;

        $(document).ready(function () {

            window.root = turnIntoSparseArray(window.test, true);
            var reportTable = document.getElementById("reportTable");
            displayer = new SimpleArrayDisplayer(window.root, reportTable, "test", new SearchRunItemVisualizer());
            displayer.render();

            createFilters();

            var filterRow = $(document.getElementById('filterRow'));

            for (var i = 0; i < window.filters.length; i++) {
                filterRow.append(window.filters[i].render());
            }

            $('#popupMask').click(closeForm);

            title.loadFromCookies();
            user.loadFromCookies();
            instrument.loadFromCookies();

            ajaxRequest('load');

            var periodicalUpdate = setInterval(function (pe) {
                ajaxRequest('update');
            }, updateDelay);

        });
    </script>
    <link rel="stylesheet" href="/common/topbar.css" media="all">
</head>
<body id="body">
<div class="topbar">
    <span class="logo-small">${title}</span>
    <ul class="locations">
        <li><a href="/start">New search</a></li>
        <li class="active-tab"><a href="/report">Existing searches</a></li>
        <li><a href="/">About Swift</a></li>
        <li><a href="/quameter">QuaMeter</a></li>
        <!-- TODO - make optional -->
        <li><a href="/extras">Extras</a></li>
    </ul>
</div>

<c:if test="${messageDefined}">
    <div class="user-message">
            ${userMessage}
    </div>
</c:if>

<div id="contents">
    <div class="navigation">
        <script type="text/javascript">
            hiddenStr = ""
            if (showHidden) {
                hiddenStr = "&showHidden=true"
            }
            if (firstEntry > 0) {
                document.write('<a class="first" href="?start=0' + hiddenStr + '" title="Go to the first search"></a>&nbsp;');
                document.write('<a class="prev" href="?start=' + (Number(firstEntry) - Number(listedEntries)) + hiddenStr + '" title="Go to previous page"></a>&nbsp;');
            }
            document.write('<a class="next" href="?start=' + (Number(firstEntry) + Number(listedEntries)) + hiddenStr + '" title="Go to next page"></a>&nbsp;');
        </script>
    </div>
    <table class="report" id="reportTable">
        <thead class="noRemove">
        <tr class="columns" id="filterRow">
            <th>&nbsp;</th>
        </tr>
        </thead>
    </table>
    <div class="navigation">
        <script type="text/javascript">
            hiddenStr = ""
            if (showHidden) {
                hiddenStr = "&showHidden=true"
            }
            if (firstEntry > 0) {
                document.write('<a class="first" href="?start=0' + hiddenStr + '" title="Go to the first search"></a>&nbsp;');
                document.write('<a class="prev" href="?start=' + (Number(firstEntry) - Number(listedEntries)) + hiddenStr + '" title="Go to previous page"></a>&nbsp;');
            }
            document.write('<a class="next" href="?start=' + (Number(firstEntry) + Number(listedEntries)) + hiddenStr + '" title="Go to next page"></a>&nbsp;');
        </script>
    </div>
</div>
<div id="popupMask" class="opacityZero">&nbsp;</div>
<div id="popups" style="clear: both;"></div>
</body>
</html>
