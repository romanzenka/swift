var reqContainer;
var url = "DirectoryService";
var varurl = null;

var collapse = "filechooser/minus.gif";
var expand = "filechooser/plus.gif";
var childless = "filechooser/hline.gif";

var foldIcon = "filechooser/fold.png"
var unfoldIcon = "filechooser/unfold.png"
var fileIcon = "filechooser/file.png";
var rawFileIcon = "filechooser/raw.png";
var mgfFileIcon = "filechooser/mgf.png";
var agilentFileIcon = "filechooser/d.png";

var folderWidth = 16;
var folderHeight = 16;
var spin = "filechooser/spin.gif";
var folder = "folder";
var contents = "contents";
var selection = new Array();
var selectionParent = null;
var lastSelected = null;
var returnCallback = null;
var doc = null;
var okmsg = null;
var blankSrc = "filechooser/blank.gif";
// the top most node
var g_top = "";
// the topmost path
var g_basepath = "";
var firstQuery = true;


var expandedList = new ExpandedList();
expandedList.loadFromCookies();

function dropEvents(evt) {
    return false;
}

//alert("In dialog.js");
/*
 iframe - name of the container, could be a div, or can be the container object
 callback - method to call if exists (does not seem to be used)
 okMessage - message to display if ok (does not seem to be used)
 */
function initDialog(iframe, callback, okMessage, basepath) {
    if (typeof iframe == "string") {
        iframe = document.getElementById(iframe);
    }

    doc = document;
    g_top = iframe;
    g_basepath = basepath;

    returnCallback = callback;
    okmsg = okMessage;

    queryInitial(iframe);
}

/*
 sends the asynchronous request for initial directory contents, with specific directories being
 pre-expanded
 */
function queryInitial(container) {
    sendQuery(url, { 'e' : expandedList.getListAsString() }, container);
}

/*
 sends the asynchronous request for contents of directory dir
 */
function queryDirectory(dir, container) {
    sendQuery(url, { 'd' : dir }, container);
}

/*
 Sends the asynchronous query to given url.
 */
function sendQuery(url, data, container) {
    if (container)
        reqContainer = container;
    else
        reqContainer = doc.getElementById("autocomplete");
    $.post(url, data, process, "xml");
}

function getTarget(evt) {
    return (evt.target) ? evt.target :
            ((evt.srcElement) ? evt.srcElement : null);
}

function getFolderPath(target) {
    var path = "";
    if (target)
        if (target.tagName)
            if (target.className != "filelisttitle")
                if (target.tagName.toLowerCase() == "div")
                    path = getFolderPath(target.parentNode.previousSibling) + target.label + "/";
                else if (target.tagName.toLowerCase() == "img")
                    path = getFolderPath(target.parentNode.parentNode.previousSibling) + target.parentNode.label + "/";

    return path;
}

function displayContainer(container) {
    var dir = container.previousSibling;
    var target = dir.firstChild;
    target.src = collapse;
    target.nextSibling.src = foldIcon;
    container.style.display = "block";
}

/*
 used to expand a folder, target is the img associated with div containing the folder
 container for subtree is next sibling of the description node
 */
function toggleExpand(target) {
    if (!target) return;

    while (target.previousSibling)
        target = target.previousSibling;

    if (target.parentNode.nextSibling.style.display.match(/none/i)) {
        expandedList.expand(getFolderPath(target));
        expandedList.saveToCookies();
        target.src = spin;
        target.nextSibling.src = foldIcon;
        target.parentNode.nextSibling.style.display = "block";
        queryDirectory(getFolderPath(target), target.parentNode.nextSibling);
    } else {
        expandedList.collapse(getFolderPath(target));
        expandedList.saveToCookies();
        target.src = expand;
        target.nextSibling.src = unfoldIcon;
        target.parentNode.nextSibling.style.display = "none";

        // TODO: Do not remove the children, instead update the tree recursively after the query returns
        while (target.parentNode.nextSibling.childNodes.length > 0) {
            var child = target.parentNode.nextSibling.childNodes[0];
            if (child.className && child.className.match(/(file|folder)/i)) {
                select(child, false);
            }
            target.parentNode.nextSibling.removeChild(child);
        }
    }
}
/*
 handle click on the image representing folder
 */

var singleClickTimeout = null;
var singleClickEvent = null;

function onFolderClick(evt) {
    var e = (evt) ? evt : ((event) ? event : null);
    if (e) {
        var target = getTarget(e);
        toggleExpand(target);
    }
}

function onFolderNameSingleClick() {
    clearTimeout(singleClickTimeout);
    // Folders should behave just like files when just clicked at
    if (singleClickEvent) {
        var target = getTarget(singleClickEvent);
        if (target.className.match(/itemLabel/i)) {
            onFileNameClick(singleClickEvent);
        }
    }
}

/*
 handle double click on the folder name object, remember have
 <div>
 <img>
 </img>
 foldername
 </div>
 <div>
 container for subtree
 </div>
 */
function onFolderNameDblClick(evt) {
    // Stop the single-click from happening.
    clearTimeout(singleClickTimeout);
    singleClickEvent = null;
    var e = (evt) ? evt : ((event) ? event : null);
    if (e) {
        var target = getTarget(e);
        toggleExpand(target.previousSibling);
        e.cancelBubble = true;
        return false;
    }
    return true;
}

function onFileNameDblClick(evt) {
    var e = (evt) ? evt : ((event) ? event : null);
    if (e) {
        var target = getTarget(e);
        e.cancelBubble = true;
        return false;
    }
    return true;
}

function deselectAll() {
    for (var i = 0; i < selection.length; i++) {
        select(selection[i], false, true);
    }
    selection = new Array();
}
/*
 called when click on a folder name
 toggles the selection display
 */
function onFolderNameClick(evt) {
    var e = (evt) ? evt : ((event) ? event : null);
    if (e) {
        // Copy important properties of e to new object.
        singleClickEvent = new Object();
        singleClickEvent.target = getTarget(e);
        singleClickEvent.shiftKey = e.shiftKey;
        singleClickEvent.ctrlKey = e.ctrlKey;
        singleClickEvent.metaKey = e.metaKey;
        singleClickTimeout = window.setTimeout("onFolderNameSingleClick();", 400);
    }
}

function toggleSelection(target) {
    var selected = !isselected(target);
    select(target, selected);

    if (!selected) {
        var element = findFileDirElement(target);
        while (element.parentNode.previousSibling) {
            element = element.parentNode.previousSibling;
            element = findFileDirElement(element);
            if (element)
                select(findElementToSelect(element), false, false, true);
        }
    }
}

//	called when click on a file name toggles the selection display
function onFileNameClick(evt) {
    var e = (evt) ? evt : ((event) ? event : null);
    if (e) {
        var target = getTarget(e);
        target = findElementToSelect(target);
        if (e.shiftKey && lastSelected != null) {
            // User wants to select a range
            var parentElement = findParentDirElement(target);
            var nowSelected = findFileDirElement(target);

            // Range can be only within the same parent, otherwise the click is completely ignored
            if (parentElement == selectionParent) {
                if (!e.ctrlKey && !e.metaKey) {
                    deselectAll();
                    select(findElementToSelect(lastSelected), true);
                }
                // Select everything between lastSelected and nowSelected.
                var doSelecting = false;
                for (var i = 0; i < parentElement.childNodes.length; i++) {
                    var child = parentElement.childNodes[i];
                    if (doSelecting) {
                        if (!child.className.match(/dircontents/i)) {
                            select(child, true);
                        }
                    }
                    if (child == nowSelected || child == lastSelected)
                        doSelecting = !doSelecting;
                }

                select(nowSelected, true);
            }
        } else if (e.ctrlKey || e.metaKey) {
            toggleSelection(target);
        } else {
            deselectAll();
            select(target, true);
        }
        // We redefine the "lastselected" entry only if the user does not hold shift key
        // (so they can shift-click several times in a row with identical starting point).
        if (!e.shiftKey) {
            selectionParent = findParentDirElement(target);
            lastSelected = findFileDirElement(target);
        }
    }
}

// When given an element the user clicked, this function returns the part that is supposed to be selected.
// In this case it is the span with 'itemLabel' class
function findElementToSelect(element) {
    element = findFileDirElement(element);

    if (element) {
        for (var i = 0; i < element.childNodes.length; i++) {
            var child = element.childNodes[i];
            if (child.className.match(/itemLabel/i))
                return child;
        }
    }
    return null;
}

// When given an element, this function finds its file/directory container
function findFileDirElement(element) {
    while (element && !element.tagName.match(/div/i))
        element = element.parentNode;
    return element;
}

// When given an element, this function finds the directory element that contains it.
function findParentDirElement(element) {
    element = findFileDirElement(element);
    if (element) element = element.parentNode;
    return element;
}

// Returns the directory contents container for given element
function getDirContents(element) {
    return element.nextSibling;
}

function select(element, doSelect, skipSelectionUpdate, skipRecursion) {
    element = findElementToSelect(element);

    var selMatch = doSelect ? /\bselected\b/i : /\bdeselected\b/i;
    if (!element.className.match(selMatch)) {
        if (!skipSelectionUpdate) {
            if (doSelect)
                selection.push(element);
            else {
                var newSelection = new Array;
                for (var i = 0; i < selection.length; i++)
                    if (selection[i] != element)
                        newSelection.push(selection[i]);
                selection = newSelection;
            }
        }

        var regexp = new RegExp(' ?(|de)selected');
        var newClass = element.className.replace(regexp, '');
        element.className = newClass + (doSelect ? " selected" : " deselected");

        // Recursively select/deselect all children
        var dirElement = findFileDirElement(element);
        if (!skipRecursion) {
            if (dirElement.className.match(/folder/i)) {
                var dirContents = getDirContents(dirElement);
                // We are a folder
                for (var j = 0; j < dirContents.childNodes.length; j++) {
                    var child = dirContents.childNodes[j];
                    if (child.className.match(/(folder|file)/i) && child.style.display != 'none') {
                        select(child, doSelect, skipSelectionUpdate);
                    }
                }
            }
        }

        // If deselecting, make sure all parents are deselected as well
    }
}
/*
 folder gets represented by
 <div class="folder">
 <img>
 // this accepts the click event to call onFolderClick
 </img>
 <span> // note span has the select
 // takes the text node as child
 // accepts the double click event to call onFolderNameDblClick
 </span>
 </div>
 */
function createDirEntry(text, isLast) {
    var item = doc.createElement("div");
    var folder = doc.createElement("img");
    folder.src = expand;
    folder.width = folderWidth;
    folder.height = folderHeight;

    var icon = doc.createElement("img");
    icon.src = unfoldIcon;
    icon.width = folderWidth;
    icon.height = folderHeight;

    item.appendChild(folder);
    item.appendChild(icon);
    folder.onclick = onFolderClick;
    folder.ondblclick = null;
    icon.onclick = onFolderClick;
    icon.ondblclick = null;

    var lab = doc.createElement("span");
    lab.appendChild(doc.createTextNode(text));
    lab.className = "itemlabel";
    // disable text selection in Mozilla
    item.onmousedown = dropEvents;
    // and IE
    item.onselectstart = dropEvents;
    item.ondragstart = dropEvents;
    item.onclick = onFolderNameClick;
    item.ondblclick = onFolderNameDblClick;
    item.appendChild(lab);
    item.label = text;
    item.className = (isLast ? "folder folder-last" : "folder");

    return item;
}
/*
 create the directory container
 <div class="dircontents">
 </div>
 */
function createDirContainer(isLast) {
    var item = doc.createElement("div");
    item.className = isLast ? "dircontents dircontents-last" : "dircontents";
    item.style.display = "none";
    return item;
}

function getIconImage(text) {
    if (/\.RAW$/i.test(text)) {
        return rawFileIcon;
    } else if (/\.mgf$/i.test(text)) {
        return mgfFileIcon;
    } else if (/\.d$/i.test(text)) {
        return agilentFileIcon;
    } else
        return fileIcon;
}

/*
 create the file entry
 <div class="file">
 <img>
 <span>
 text
 </span>
 </div>
 handles events click - onFileNameClick
 dbl click - onFileNameDblClick
 */
function createFileEntry(text, isLast) {
    var item = doc.createElement("div");

    var fileel = doc.createElement("img");
    fileel.src = childless;
    fileel.width = folderWidth;
    fileel.height = folderHeight;
    item.appendChild(fileel);

    var icon = doc.createElement("img");
    icon.src = getIconImage(text);
    icon.width = folderWidth;
    icon.height = folderHeight;
    item.appendChild(icon);

    var lab = doc.createElement("span")
    lab.appendChild(doc.createTextNode(text));
    lab.label = text;
    lab.className = "itemlabel";
    item.appendChild(lab);
    // disable text selection in Mozilla
    item.onmousedown = dropEvents;
    // and IE
    item.onselectstart = dropEvents;
    item.ondragstart = dropEvents;
    item.onclick = onFileNameClick;
    item.ondblclick = onFileNameDblClick;
    item.className = (isLast ? "file file-last" : "file");
    item.label = text;

    return item;
}

function createTextEntry(text) {
    var item = doc.createElement("div");
    var fileel = doc.createElement("img");
    fileel.src = blankSrc;
    fileel.width = folderWidth;
    fileel.height = folderHeight;
    item.appendChild(fileel);

    var lab = doc.createElement("span")
    lab.appendChild(doc.createTextNode(text));
    lab.label = text;
    lab.className = "itemlabel";
    item.appendChild(lab);
    // disable text selection in Mozilla
    item.onmousedown = dropEvents;
    // and IE
    item.onselectstart = dropEvents;
    item.ondragstart = dropEvents;
    return item;
}
/*
 add the nodes to the container with the DOM
 node - root node of the XML result.
 */
function processResults(container, node) {
    var selected = false;
    if (container.className.match(/dircontents/i)) {
        var c = container.childNodes.length;
        for (var i = 0; i < c; i++) {
            container.removeChild(container.firstChild);
        }
        selected = isselected(findElementToSelect(container.previousSibling));
    }
    if (!node) {
        hasNoChildren(container);
    }
    while (node) {
        var nextNode = node.nextSibling;
        while (nextNode && nextNode.nodeType != 1) {
            var text = node.getAttribute('name');
            nextNode = nextNode.nextSibling;
        }

        if (node.nodeType == 1) {

            var text = node.getAttribute('name');
            if (!text) {
                text = "[null]";
            }
            var child = null;
            var dirContainer = null;
            if (node.tagName == "dir") {
                child = createDirEntry(text, !nextNode);
                container.appendChild(child);
                dirContainer = createDirContainer(!nextNode);
                container.appendChild(dirContainer);
            } else if (node.tagName == "file") {
                child = createFileEntry(text, !nextNode);
                container.appendChild(child);
            } else {
                container.appendChild(createTextEntry("Unknown item: " + node + ": " + node.tagName + ": " + text));
            }
            if (selected)
                select(child, selected, false, true);
            // Handle recursive dir contents
            if (node.tagName == "dir") {
                if (node.firstChild) {
                    processResults(dirContainer, node.firstChild);
                    displayContainer(dirContainer);
                }
            }
        }
        node = nextNode;
    }
}

function stopAnimation(container) {
    var i = container.previousSibling;
    if (i) {
        if (i.firstChild) {
            if (i.firstChild.tagName) {
                if (i.firstChild.tagName.toLowerCase() == "img") {
                    i.firstChild.src = collapse;
                }
            }
        }
    }
}


function hasNoChildren(container) {
    var i = container.previousSibling;
    if (i)
        if (i.firstChild)
            if (i.firstChild.tagName)
                if (i.firstChild.tagName.toLowerCase() == "img") {
                    i.firstChild.src = childless;
                }
}

/*
 processes the xmlhttprequest result
 expected format is xml with content
 <results>
 <dir>scx run 1</dir>
 <dir>scx run 2</dir>
 <file>some file</file>
 </results>

 */
function process(data, textstatus) {
    if (textstatus == "success") {
        var container = reqContainer;
        stopAnimation(container);

        var dom = data;
        if (dom) {
            var docroot = dom.documentElement;
            var type = docroot.tagName;

            var node;
            if (type == "results") {
                node = docroot.firstChild;
                processResults(container, node);
            } else if (type == "error") {
                node = docroot.firstChild;
                container.innerHTML = "Error: " + node.nodeValue;

            } else {
                container.innerHTML = "Unknown result type: " + type;
            }
        } else {
            container.innerHTML = "No XML returned";
        }
    } else {
        //	"There was a problem retrieving data:<br>"+req.statusText+"<br>"+req.responseText;
    }
}


/*
 returns the selected files and folders in the tree
 each item returned contains the full path
 returns Array of String

 note that ...
 folder gets represented by
 <div class="folder">
 <img>
 // this accepts the click event to call onFolderClick
 </img>
 <span>
 // takes the text node as child
 // accepts the double click event to call onFolderNameDblClick
 </span>
 </div>
 followed by the directory container
 <div class="dircontents">
 </div>
 otherwise have
 the file entry
 <div class="file">
 <img>
 <span>
 text
 </span>
 </div>
 */
function getSelectedFilesAndFolders() {
    var paths = new Array();
    // start at top
    // if node is file get its text content
    // if node is a directory gets its name and recurse
    getfilesanddirsnextlevel(g_top, g_basepath, paths);
    return paths;
//	var concatpaths = "";
    //	// return the paths as a concatenated string with a \n separator between folder and file names
    //	for (var i = 0; i < paths.length; i++) {
    //		concatpaths = concatpaths + paths[i] + "\n";
    //	}
    //	return concatpaths;

}

function getfilesanddirsnextlevel(container, path, selpaths) {
    // for each child
    var thechildren = container.childNodes;
    var c = container.childNodes.length;
    for (var i = 0; i < c; i++) {
        var curr = thechildren[i];
        if (curr.className && curr.className.match(/folder/i)) {
            if (isfolderselected(curr)) {
                notefolderselected(curr, path, selpaths);
            } else {
                // call its next sibling to recurse
                if (thechildren[i + 1].className.match(/dircontents/i)) {
                    getfilesanddirsnextlevel(thechildren[i + 1], path + "/" + getfoldertext(curr), selpaths);
                }
            }
        }
        // file case
        if (curr.className && curr.className.match(/file/i)) {
            if (isfolderselected(curr)) {
                notefolderselected(curr, path, selpaths);
            }
        }
    }

    // if it is a folder
}

function notefolderselected(folder, path, selpaths) {
    // find its text and add to path
    var text = getfoldertext(folder);
    // put path in the list
    selpaths.push(path + "/" + text);
}

function getfoldertext(folder) {
    if (folder) {
        return folder.label;
    }
    return "";
}

function isselected(container) {
    var classname = container.className;
    return classname && (classname.indexOf(" selected") != -1);
}

// is folder selected
function isfolderselected(container) {
    // see if its span child selected
    var dirElement = findFileDirElement(container);
    if (dirElement) {
        return isselected(findElementToSelect(dirElement));
    }
    return false;
}
