<%@ page import="edu.mayo.mprc.ServletIntialization" %>
<%@ page import="edu.mayo.mprc.swift.SwiftWebContext" %>
<%@ page import="edu.mayo.mprc.swift.report.JsonWriter" %>
<%@ page import="edu.mayo.mprc.utilities.StringUtilities" %>
<%@ page import="edu.mayo.mprc.workspace.User" %>
<%@ page import="java.util.List" %>
<%ServletIntialization.initServletConfiguration(getServletConfig());%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
"http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<title><%=SwiftWebContext.getServletConfig().getTitle()%> Search Status</title>
<!--[if IE]>
<style type="text/css">
    .opacityZero {
        filter: alpha(opacity = 0);
    }
</style>
<![endif]-->

<script type="text/javascript">
    // This is a prefix that has to be removed from the files in order to map the web paths
    var pathPrefix = "<%=SwiftWebContext.getPathPrefix().replace("\\", "\\\\") %>";

    // How does the raw file root map to the web browser? Idea is that you strip pathPrefix from the path,
    // prepend pathWebPrefix instead and use the resulting URL in your browser
    var pathWebPrefix = "<%=SwiftWebContext.getServletConfig().getBrowseWebRoot().replace("\\", "\\\\") %>";
</script>

<link rel="stylesheet" href="report.css">
<script type="text/javascript" src="/start/filechooser/cookies.js"></script>
<script type="text/javascript" src="prototype.js"></script>
<script type="text/javascript" src="updates.js"></script>
<script type="text/javascript" src="visualizers.js"></script>
<script type="text/javascript" src="filters.js"></script>
<script type="text/javascript">
    window.test = ({ total : 0 });
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
        this.array.onchange = function() {
            myself.update();
        };
        this.parentElement = parentElement;
        this.id = id;
        this.itemVisualizer = itemVisualizer;
    }

    SimpleArrayDisplayer.prototype.render = function() {
        for (var i = 0; i < this.array.total; i++) {
            var item = this.array.getItemById(i);
            var element = this.itemVisualizer.render(this.id + "_" + i, item, 'tbody');
            this.parentElement.appendChild(element);
        }
    };

    SimpleArrayDisplayer.prototype.update = function() {
        removeChildrenExcept(this.parentElement, /noRemove/i);
        this.render();
    };

    SimpleArrayDisplayer.prototype.listExpandedItems = function() {
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

    function createFilters() {
        var title = new FilterDropDown("title");
        title.addRadioButtons("sort", "order", ["Sort A to Z", "Sort Z to A"], ["title ASC", "title DESC"], -1);
        title.addSeparator();
        title.addSeparator();
        title.addOkCancel();
        $('popups').appendChild(title.getRoot());

    <%
    SwiftWebContext.getServletConfig().getSwiftDao().begin();
    final StringBuilder userList=new StringBuilder();
    final StringBuilder idList = new StringBuilder();
    try {
       final List<User> userInfos = SwiftWebContext.getServletConfig().getWorkspaceDao().getUsers();
       if (userInfos != null) {
           for (final User userInfo : userInfos) {
               if(userList.length()>0) {
                   userList.append(",");
                   idList.append(",");
               }
               userList.append("'").append(StringUtilities.toUnicodeEscapeString(JsonWriter.escapeSingleQuoteJavascript(userInfo.getFirstName()))).append(' ').append(StringUtilities.toUnicodeEscapeString(JsonWriter.escapeSingleQuoteJavascript(userInfo.getLastName()))).append("'");
               idList.append("'").append(JsonWriter.escapeSingleQuoteJavascript(String.valueOf(userInfo.getId()))).append("'");
           }
       }
       SwiftWebContext.getServletConfig().getSwiftDao().commit();
    } catch(Exception ignore) {
        SwiftWebContext.getServletConfig().getSwiftDao().rollback();
    }
%>
        user = new FilterDropDown("user");
        user.addRadioButtons("sort", "order", ["Sort A to Z", "Sort Z to A", "Sort by submission time"], ["1", "-1", "0"], 2);
        user.addSeparator();
        user.addText('Display:');
        user.addCheckboxes("filter", "where", [<%=userList.toString()%>], [<%=idList.toString()%>], true);
        user.addSeparator();
        user.addOkCancel();
        user.onSubmitCallback = function() {
            user.saveToCookies();
            ajaxRequest('load');
        };
        $('popups').appendChild(user.getRoot());

        var submission = new FilterDropDown("submission");
        submission.addRadioButtons("sort", "order", ["Sort newest to oldest", "Sort oldest to newest"], ["submission ASC", "submission DESC"], -1);
        submission.addSeparator();
        submission.addText('Display:');
        submission.addRadioButtons("filter", "where",
                ["All", "Submitted today", "Submitted this week", "Older than 1 week"],
                ['', 'submission<=1', 'submission<=7', 'submission>7'], 0);
        submission.addSeparator();
        submission.addOkCancel();
        $('popups').appendChild(submission.getRoot());

        var results = new FilterDropDown("results");
        results.addText("Sort by number of errors");
        results.addRadioButtons("sort", "order", ["Error free first", "Most errors first"], ["errors ASC", "errors DESC"], -1);
        results.addSeparator();
        results.addText("Sort by last error");
        results.addRadioButtons("sort", "order", ["Newest errors to oldest", "Oldest errors to newest"], ["errotime ASC", "errotime DESC"], -1);
        results.addSeparator();
        results.addText('Display:');
        results.addCheckboxes("filter", "where", ["Error free", "Warnings", "Failures"], ["status='ok'", "status='warnings'", "status='failures'"], true);
        results.addSeparator();
        results.addOkCancel();
        $('popups').appendChild(results.getRoot());

        filters = [
            new FilterButton('title', 'Title', null /*title*/),
            new FilterButton('user', 'Owner', user),
            new FilterButton('submission', 'Submission', null /*submission*/),
            new FilterButton('duration', 'Duration', null /*duration*/),
            new FilterButton('actions', '', null /*duration*/),
            new FilterButton('results', 'Results', null /*results*/),
            new FilterButton('progress', 'Progress', null)
        ];
        filterManager = new FilterManager(filters);
    }

    function closeForm(evt) {
        $('popupMask').style.display = 'none';
        for (var i = 0; i < filters.length; i++)
            if (filters[i].dropdown)
                filters[i].dropdown.hide();
        Event.stop(evt);
    }

    function ajaxRequest(action) {
        new Ajax.Request('reportupdate', {
            method: 'get',
            parameters: {
                action: action,
                start: firstEntry,
                count: listedEntries,
                expanded: displayer.listExpandedItems(),
                timestamp : window.timestamp,
                userfilter: user.getRequestString(),
                showHidden: showHidden
            }
        });
    }


    var timestamp = 0;

    var periodicalUpdate;
    var updateDelay = 60;
    var queries = getQueryString();
    var listedEntries = queries['count'] == null ? 100 : queries['count'];
    var firstEntry = queries['start'] == null ? 0 : queries['start'];
    var showHidden = queries['showHidden'] == null ? 0 : queries['showHidden'];
    var displayer;

    Event.observe(window, 'load', function() {

        window.root = turnIntoSparseArray(window.test, true);
        var reportTable = document.getElementById("reportTable");
        displayer = new SimpleArrayDisplayer(window.root, reportTable, "test", new SearchRunItemVisualizer());
        displayer.render();

        createFilters();
        var filterRow = document.getElementById('filterRow');

        for (var i = 0; i < window.filters.length; i++) {
            filterRow.appendChild(window.filters[i].render());
        }

        Event.observe('popupMask', 'click', closeForm);

        user.loadFromCookies();
        ajaxRequest('load');

        periodicalUpdate = new PeriodicalExecuter(function(pe) { ajaxRequest('update'); }, updateDelay);

    });
</script>
<link rel="stylesheet" href="/common/topbar.css" media="all">
</head>
<body id="body">
<div class="topbar">
    <span class="logo-small"><%=SwiftWebContext.getServletConfig().getTitle()%></span>
    <ul class="locations">
        <li><a href="/start">New search</a></li>
        <li class="active-tab"><a href="/report/report.jsp">Existing searches</a></li>
        <li><a href="/">About Swift</a></li>
        <li><a href="/extras">Extras</a></li>
    </ul>
</div>
<% if (SwiftWebContext.getServletConfig().getUserMessage().messageDefined()) { %>
<div class="user-message">
    <%=SwiftWebContext.getServletConfig().getUserMessage().getMessage()%>
</div>
<% } %>
<div id="contents">
    <table class="report" id="reportTable">
        <thead class="noRemove">
        <tr class="columns" id="filterRow">
            <th>&nbsp;</th>
        </tr>
        </thead>
    </table>
    <div id="navigation">
        <script type="text/javascript">
            if (firstEntry > 0) {
                document.write('<a class="first" href="?start=0" title="Go to the first search">&lt;&lt;&lt First</a>&nbsp;');
                document.write('<a class="prev" href="?start=' + (Number(firstEntry) - Number(listedEntries)) + '" title="Go to previous page">&lt; Prev</a>&nbsp;');
            }
            document.write('<a class="next" href="?start=' + (Number(firstEntry) + Number(listedEntries)) + '" title="Go to next page">&gt;&gt;&lt Next</a>&nbsp;');
        </script>
    </div>
</div>
<div id="popupMask" class="opacityZero">&nbsp;</div>
<div id="popups" style="clear: both;"></div>
</body>
</html>
