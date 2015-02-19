<!-- These classes know how to visualize a given object as HTML -->

//======================================================================================================================
function AbstractItemVisualizer() {
}

// Creates a wrapper to hold the rendered item contents, sets its id to the given one
// and fills it with contents
AbstractItemVisualizer.prototype.render = function (id, object, elementType) {
    var element = document.createElement(elementType);
    element.id = id;
    this.fillWithContents(element, id, object);
    return element;
};

// Renders object of given id and data into a given element
AbstractItemVisualizer.prototype.fillWithContents = function (element, id, object) {
    var contents;
    if (object != null)
        contents = document.createTextNode("Object '" + id + "' = " + object.toString());
    else
        contents = document.createTextNode("Object '" + id + "' not loaded");
    $(element).append(contents);
};

// Updates information for already rendered object
AbstractItemVisualizer.prototype.update = function (element, object) {
    var e = $(element)[0];
    removeChildrenExcept(e, /noRemove/i);
    this.fillWithContents(e, e.id, object);
};

//======================================================================================================================
function SearchRunItemVisualizer() {
}
SearchRunItemVisualizer.prototype = new AbstractItemVisualizer();

SearchRunItemVisualizer.prototype.toggleExpanded = function (evt) {
    var id = evt.data.id;
    var object = evt.data.object;
    var t = evt.data.obj;
    var element = ($(this).parent().parent().parent())[0]; // a 1-> td 2-> tr
    evt.stopPropagation();
    object.expanded = !object.expanded;
    t.update(element, object);
    if (object.expanded) {
        var objectId = object.id;
        $.ajax({
                "url": "/report/reportupdate",
                "data": {action: 'expand', id: objectId}
            }
        ).done(function (data) {
                eval(data);
            });
    }
};

SearchRunItemVisualizer.prototype.render = function (id, object, elementType) {
    var element = document.createElement(elementType);
    this.fillWithContents(element, id, object);
    return element;
};

SearchRunItemVisualizer.prototype.multiResultHeadTemplate = '<table class="reportList"><tr><td class="result-link"><span class="result-list">';
SearchRunItemVisualizer.prototype.multiResultEntryTemplate = '<a href="#{fullUrl}" title="#{filePath}">#{fileName}</a><br/>';
// Entry has analysis attached
SearchRunItemVisualizer.prototype.multiResultEntryAnalysisTemplate = '<a href="/analysis?id=#{reportId}" class="analysis-data-link">Data</a><a href="#{fullUrl}" title="#{filePath}">#{fileName}</a><br clear="left"/>';
SearchRunItemVisualizer.prototype.multiResultTailTemplate = '</span></td><td class="result-buttons">' +
'<a href="#{parentUrl}" class="parent-dir-link" title="#{parentPath}">Directory</a>' +
'<a href="/service/qa/#{searchId}/index.html" class="qa-link" title="Quality Analysis">QA</a>' +
'</td></tr></table>';

SearchRunItemVisualizer.prototype.displayTransactionError = function (event, message) {
    alert(message);
};

SearchRunItemVisualizer.prototype.getParentFile = function (filename) {
    var i = filename.lastIndexOf('/');
    if (i < 0) {
        return "";
    }
    return filename.substr(0, i);
};

SearchRunItemVisualizer.prototype.confirmRerun = function (event) {
    var id = event.data.id;
    var title = event.data.title;
    if (!window.confirm("Are you sure you want to restart search " + title + " (id=" + id + ") ?"
        )) {
        event.stopPropagation();
    }
};

SearchRunItemVisualizer.prototype.confirmHide = function (event) {
    var id = event.data.id;
    var title = event.data.title;
    if (!window.confirm("Are you sure you want to hide search " + title + " (id=" + id + ") ?\n\n"
        + "After a search was hidden, it takes an admin to unhide it again.")) {
        event.stopPropagation();
    }
};

// Splits a path string into several parts that are set as on the fileinfo object
SearchRunItemVisualizer.prototype.splitPathIntoParts = function (originalPath, fileInfo) {
    // The path is absolute, while pathWebPrefix maps to the web root
    // We must trim the path so it is relative to web root

    var regex = new RegExp("^(<file>" + window.pathPrefix + ")?((?:[^\\/<]*\\/)+)([^\\/,<]*[^<.,]+)</file>$", "ig");
    fileInfo['parentUrl'] = originalPath.replace(regex, window.pathWebPrefix + '$2');
    fileInfo['fullUrl'] = originalPath.replace(regex, window.pathWebPrefix + '$2$3');
    fileInfo['parentPath'] = originalPath.replace(regex, '$2');
    fileInfo['filePath'] = originalPath.replace(regex, '$2$3');
    fileInfo['fileName'] = originalPath.replace(regex, '$3');
    return fileInfo;
};

SearchRunItemVisualizer.prototype.fillWithContents = function (fragment, id, object) {
    if (object != null) {
        var element = document.createElement("tr");
        element.id = id;

        $(fragment).append(element);

        // Contents
        var arrowCell = document.createElement('td');
        arrowCell.className = "arrowCell";
        arrowCell.title = object.id ? "Id: " + object.id : "";
        element.appendChild(arrowCell);

        // ------------------------------------------------------------------
        var tdTitle = document.createElement('td');
        tdTitle.appendChild(document.createTextNode(object.title));
        if (object.ranTooLong) {
            tdTitle.className = "ran-too-long";
        }
        var title = element.appendChild(tdTitle);

        // ------------------------------------------------------------------
        var tdUser = document.createElement('td');
        tdUser.appendChild(document.createTextNode(object.user));
        var user = element.appendChild(tdUser);

        // ------------------------------------------------------------------
        var tdSubmit = document.createElement('td');
        tdSubmit.appendChild(document.createTextNode(object.submitted));
        var submitted = element.appendChild(tdSubmit);

        // ------------------------------------------------------------------
        var tdDuration = document.createElement('td');

        tdDuration.appendChild(document.createTextNode(object.duration));

        var duration = element.appendChild(tdDuration);

        // ------------------------------------------------------------------
        var tdInstruments = document.createElement('td');

        tdInstruments.appendChild(document.createTextNode(object.instruments));

        var instruments = element.appendChild(tdInstruments);


        // ------------------------------------------------------------------
        var tdAction = document.createElement('td');

        if (object.id) {
            // Add a rerun link
            rerunLink = document.createElement("a");
            rerunLink.appendChild(document.createTextNode("Rerun"));
            rerunLink.className = "rerun-link";
            rerunLink.href = "/report/reportupdate?rerun=" + object.id;
            rerunLink.title = "Rerun " + object.id;
            $(rerunLink).on("click", {"id": object.id, "title": object.title}, this.confirmRerun);
            tdAction.appendChild(rerunLink);

            if (object.search && object.search != 0) {
                // Add an edit link
                editLink = document.createElement("a");
                editLink.appendChild(document.createTextNode("Edit"));
                editLink.className = "edit-link";
                editLink.href = "/start/?load=" + object.id;
                editLink.title = "Edit " + object.id;
                tdAction.appendChild(editLink);

                // Add a difference link
                diffLink = document.createElement("a");
                diffLink.appendChild(document.createTextNode("Diff"));
                diffLink.className = "diff-link";
                diffLink.href = "/search-diff?id=" + object.id;
                diffLink.title = "Differences for " + object.id;
                tdAction.appendChild(diffLink);
            }

            // Add a hide link
            hideLink = document.createElement("a");
            hideLink.appendChild(document.createTextNode("Hide"));
            hideLink.className = "hide-link";
            hideLink.href = "/report/reportupdate?hide=" + object.id;
            hideLink.title = "Hide " + object.id;
            $(hideLink).on("click", {"id": object.id, "title": object.title}, this.confirmHide);
            tdAction.appendChild(hideLink);

            // Add quameter link
            if (object.quameter == 1) {
                quameterLink = document.createElement("a");
                quameterLink.appendChild(document.createTextNode("Quameter"));
                quameterLink.className = "quameter-link";
                quameterLink.href = "/quameter?id=" + object.id;
                quameterLink.title = "Quameter result " + object.id;
                tdAction.appendChild(quameterLink);
            }
        }

        var action = element.appendChild(tdAction);

        // ------------------------------------------------------------------
        var tdResults = document.createElement('td');
        tdResults.style.padding = "0";
        tdResults.style.margin = "0";

        var running = object.running ? object.running : 0;
        var total = object.ok ? object.ok : 0;
        total += object.warnings ? object.warnings : 0;
        total += object.failures ? object.failures : 0;

        var statusMessage = "";
        if (object.subtasks > 0) {
            if (object.subtasks > total) {
                statusMessage = total + " of " + object.subtasks + " done";
                var detailed = "";
                if (object.warnings) detailed = object.warnings + " warning";
                if (object.failures) {
                    if (detailed != "") detailed += ", ";
                    detailed += object.failures + " failed";
                }
                if (object.running) {
                    if (detailed != "") detailed += ", ";
                    detailed += object.running + " running";
                }
                if (detailed != "")
                    statusMessage += " (" + detailed + ")";
            }

            var results = "";
            if (total == object.subtasks && (!object.results || object.results.length == 0)) {
                results = '<span class="result-status">No reports available</span>';
            }
            else {
                if (object.results && object.results.length > 0) {
                    results += $.tmpl(this.multiResultHeadTemplate, {id: "file_" + object.id});
                    var fileInfo;
                    for (var i = 0; i < object.results.length; i++) {
                        fileInfo = object.results[i];
                        this.splitPathIntoParts(fileInfo.path, fileInfo);
                        fileInfo['searchId'] = object.id;
                        if (fileInfo.analysis == 0) {
                            results += $.tmpl(this.multiResultEntryTemplate, fileInfo);
                        } else {
                            results += $.tmpl(this.multiResultEntryAnalysisTemplate, fileInfo);
                        }
                    }
                    results += $.tmpl(this.multiResultTailTemplate, fileInfo);
                } else {
                    results += "<span class='result-status'>" + statusMessage + "</span>";
                }
            }

            $(tdResults).append(results);
        }

        var status = element.appendChild(tdResults);

        // ------------------------------------------------------------------
        var tdProgress = document.createElement('td');
        tdProgress.className = "barcell";
        var bar;

        if (object.subtasks < 0 && objeerrerrormsg == "") {
            // Subtasks <=0! That is an error! And an error message is not set!
            object.errormsg = "ERROR: " + object.subtasks + " subtasks!";
        }

        if (object.errormsg == null || object.errormsg == "" || object.errormsg == "no error") {
            if (object.subtasks > total) {
                // Still running
                bar = document.createElement("div");
                bar.className = "progressbar";
                bar.title = statusMessage;

                var barrunning = document.createElement("div");
                barrunning.className = "barrunning";
                barrunning.style.width = ((total + running) / object.subtasks * 100) + "%";

                var barok = document.createElement("div");
                barok.className = "barok";
                barok.style.width = (total / object.subtasks * 100) + "%";

                var barwarn = document.createElement("div");
                barwarn.className = "barwarn";
                barwarn.style.width = ((total - object.ok) / object.subtasks * 100) + "%";

                var barfail = document.createElement("div");
                barfail.className = "barfail";
                barfail.style.width = ((total - (object.ok ? object.ok : 0) - (object.warnings ? object.warnings : 0)) / object.subtasks * 100) + "%";

                var bartext = document.createElement("div");
                bartext.className = "text";

                bartext.appendChild(document.createTextNode(object.heartbeat ? object.heartbeat : ""));

                bar.appendChild(barrunning);
                bar.appendChild(barok);
                bar.appendChild(barwarn);
                bar.appendChild(barfail);
                bar.appendChild(bartext);
            } else {
                // All tasks are completed, no errors
                bar = document.createElement("div");
                bar.className = "progressbar progressbar-complete";
                bar.title = "All " + object.subtasks + " tasks completed successfully.";
            }
        } else {
            bar = document.createElement("div");
            bar.className = "progressbar-error";
            errorLink = document.createElement("a");
            errorLink.title = object.errormsg;
            errorLink.href = "/report/taskerror?tid=" + object.id;
            errorLink.target = "_blank";
            errorLink.appendChild(document.createTextNode("ERROR"));
            bar.appendChild(errorLink);
        }

        tdProgress.appendChild(bar);

        var heartbeat = element.appendChild(tdProgress);

        // ------------------------------------------------------------------
        // Arrow
        var arrow = document.createElement("span");
        $(arrow).on('click', {"id": id, "object": object, "obj": this}, this.toggleExpanded);
        arrow.className = "arrow";
        var arrowImg = document.createElement("span");
        var details = null;
        if (object.expanded) {
            arrowImg.className = "arrowdown";
            if (object.details != null) {
                arrowCell.rowSpan = 2;
                var detailRow = document.createElement("tr");
                detailRow.id = id + "_details";
                var detailCell = document.createElement("td");
                detailCell.colSpan = 8;
                detailRow.appendChild(detailCell);
                var detailTable = document.createElement("table");
                detailTable.className = "tasktable";
                detailCell.appendChild(detailTable);

                detailRow.className = "search-details";

                var displayRoot = turnIntoSparseArray(object.details, true);
                var displayer = new SimpleArrayDisplayer(displayRoot, detailTable, id + "_details", new TaskItemVisualizer(object));
                this[id + "_details"] = displayer;
                displayer.render();

                fragment.appendChild(detailRow);
            }
        }
        else {
            arrowImg.className = "arrowright";
        }
        arrow.appendChild(arrowImg);
        arrowCell.appendChild(arrow);
    }
};

//======================================================================================================================
function TaskItemVisualizer(transaction) {
    this.transaction = transaction;
}

TaskItemVisualizer.prototype = new AbstractItemVisualizer();

TaskItemVisualizer.prototype.transaction = null;
TaskItemVisualizer.prototype.minTime = Number.MAX_VALUE;
TaskItemVisualizer.prototype.maxTime = 0;
TaskItemVisualizer.prototype.unknownStatusTemplate = 'ERROR: unknown status "#{status}" for object #{title}';

TaskItemVisualizer.prototype.getMinTime = function () {
    if (this.minTime == Number.MAX_VALUE) {
        if (this.transaction && this.transaction.details != null) {
            for (var i = 0; i < this.transaction.details.total; i++) {
                var subtask = this.transaction.details.getItemById(i);
                if (subtask != null && subtask.startstamp != null && subtask.startstamp < this.minTime) {
                    this.minTime = subtask.startstamp;
                }
                if (subtask != null && subtask.queuestamp != null && subtask.queuestamp < this.minTime) {
                    this.minTime = subtask.queuestamp;
                }
            }
        }
    }
    return this.minTime;
};

TaskItemVisualizer.prototype.getMaxTime = function () {
    if (this.maxTime == 0) {
        if (this.transaction && this.transaction.details != null) {
            for (var i = 0; i < this.transaction.details.total; i++) {
                var subtask = this.transaction.details.getItemById(i);
                if (subtask != null && subtask.endstamp > this.maxTime) {
                    this.maxTime = subtask.endstamp;
                }
            }
        }
    }
    return this.maxTime;
};

RegExp.escape = function (text) {
    if (!arguments.callee.sRE) {
        var specials = [
            '/', '.', '*', '+', '?', '|',
            '(', ')', '[', ']', '{', '}', '\\'
        ];
        arguments.callee.sRE = new RegExp(
            '(\\' + specials.join('|\\') + ')', 'g'
        );
    }
    return text.replace(arguments.callee.sRE, '\\$1');
};

TaskItemVisualizer.prototype.escapeRegex = function (regex) {
    var specials = [
        '/', '.', '*', '+', '?', '|',
        '(', ')', '[', ']', '{', '}', '\\'
    ];
    var sRE = new RegExp('(\\' + specials.join('|\\') + ')', 'g');
    return regex.replace(sRE, '\\$1');
};

TaskItemVisualizer.prototype.replacePathsWithHyperlinks = function (text) {
    var prefix = this.escapeRegex(window.pathPrefix);
    var regex = new RegExp("(\\s*)(<file>" + prefix + ")?((?:[^<\\/,]*\\/)+)([^<\\/,]*[^<.,]+)</file>", "ig");
    text = text.replace(regex, '$1<a class="path" href="' + window.pathWebPrefix + '$3$4" title="$3$4">$4</a>');
    return text;
};

TaskItemVisualizer.prototype.wrapPathIntoHyperlink = function (text, title) {
    var prefix = this.escapeRegex(window.pathPrefix);
    var regex = new RegExp("^(<file>" + prefix + ")?((?:[^\\/<]*\\/)+)([^\\/,<]*[^<.,]+)</file>$", "ig");
    text = text.replace(regex, '<a class="path" href="' + window.pathWebPrefix + '$2$3" title="$2$3">' + title + '</a>');
    return text;
};

TaskItemVisualizer.prototype.fillWithContents = function (element, id, object) {
    element.className = "subtask";
    if (object != null) {
        var row = document.createElement("tr");
        var item = document.createElement("td");
        row.appendChild(item);
        item.title = object.time + (object.warningmsg != null ? ' ' + object.warningmsg : '' );
        if (object.errormsg != null && object.errormsg != "") {
            var errorLink = document.createElement("a");
            errorLink.href = "/report/taskerror?id=" + object.taskid;
            errorLink.target = "_blank";
            errorLink.appendChild(document.createTextNode("ERROR "));
            item.title += " " + object.errormsg;
            item.appendChild(errorLink);
        }
        var text;
        switch (object.status) {
            case "Completed Successfully":
                text = object.title;
                item.className = "status-completed";
                break;
            case "Ready":
                text = object.title;
                item.className = "status-ready";
                break;
            case "Running":
                text = object.title;
                if (object.percentDone) {
                    text += " " + (Math.round(object.percentDone * 10) / 10) + "%";
                }
                item.className = "status-running";
                break;
            case "Initialization Failed":
                text = object.title;
                item.className = "status-failed";
                break;
            case "Run Failed":
                text = object.title;
                item.className = "status-failed";
                break;
            case "Uninitialized":
                text = object.title;
                item.className = "status-warnings";
                break;
            case "Running Warning":
                text = object.title;
                item.className = "status-running-warning";
                break;
            case "Completed with Warning":
                text = object.title;
                item.className = "status-completed-warning";
                break;
            default:
                text = $.tmpl(this.unknownStatusTemplate, object);
                break;
        }

        text = this.replacePathsWithHyperlinks(text);
        $(item).append(text);

        // Host -------------------------------------
        var host = document.createElement("td");
        if (object.host != null) {
            $(host).append(object.host);
        }
        row.appendChild(host)

        // JOB ID -----------------------------------
        var jobid = document.createElement("td");
        if (object.jobid != null) {
            if (object.status == "Running") {
                $(jobid).append('<a href="/report/reportupdate?qstat=' + object.jobid + '" title="Grid engine job id">' + object.jobid + '</a>')
            }
            else
                $(jobid).append(object.jobid);
        }
        row.appendChild(jobid);

        // LOGS -----------------------------------
        var logs = document.createElement("td");
        if (object.logs != null) {
            for (var log = 0; log < object.logs.length; log++) {
                var logInfo = object.logs[log];
                var logType = logInfo.type;
                longName = '<a class="path" href="'
                + '/service/task-log/' + object.taskid + '/' + (logType == 'STD_OUT' ? 'out' : 'err')
                + '">'
                + (logType == 'STD_OUT' ? 'out' : 'err')
                + '</a>';
                $(logs).append(longName + ' ');
            }
        }
        row.appendChild(logs);


        // GANTT BAR -----------------------------------
        if (object.startstamp != null || object.queuestamp != null) {
            var gantt = document.createElement("td");
            gantt.className = "gantt";
            this.minimumTime = this.getMinTime();
            this.maximumTime = this.getMaxTime();
            var end = object.endstamp == null ? this.maximumTime : object.endstamp;
            var start = object.startstamp == null ? end : object.startstamp;
            var queue = object.queuestamp == null ? start : object.queuestamp;
            if (queue > start) {
                queue = start;
            }

            var queuePos = (queue - this.minimumTime) / (this.maximumTime - this.minimumTime) * 100;
            var startPos = (start - this.minimumTime) / (this.maximumTime - this.minimumTime) * 100;
            var endPos = (end - this.minimumTime) / (this.maximumTime - this.minimumTime) * 100;

            var queueBar = document.createElement("div");
            queueBar.style.left = queuePos + "px";
            queueBar.style.width = (startPos - queuePos) + "px";
            queueBar.className = "queue-bar";
            gantt.appendChild(queueBar);

            var runBar = document.createElement("div");
            runBar.style.left = startPos + "px";
            runBar.style.width = (endPos - startPos) + "px";
            runBar.className = "run-bar";
            gantt.appendChild(runBar);

            row.appendChild(gantt);
        }
        // -----------------------------------

        element.appendChild(row);
    } else {
        $(element).append('<tr><td title="Not loaded">...</td><td></td><td></td><td class="gantt"></td></tr>');
    }
};

