var fields = [
    "Protein Combined Score",
    "Protein Mass",
    "Protein XCorr",
    "Protein Mowse",
    "Protein Name",
    "Protein Sequence Coverage %",
    "Peptides Id'ed in Protein",
    "Peptide XCorr",
    "Peptide Mowse",
    "Peptide Charge State",
    "Peptide Mass",
    "Peptide Length",
];

var types = "nnnntnnnnnnn"; // n/t: numeric/text
var optionDefaults = "111111111311";
var value1Defaults = [
    "5",
    "500",
    "2",
    "20",
    "Keratin",
    "15",
    "2",
    "2",
    "20",
    "1",
    "500",
    "5"
]

var value2Defaults = [
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    "2",
    null,
    null
]

var numericSecondArg = "hhhv"; // h/v: hidden/visible

var numericOptions = [ "is equal to",
    "is greater than",
    "is less than",
    "is between" ];

var textOptions = [ "contains",
    "does not contain" ];

var clickToCreateRow = null;

function getTarget(evt) {
    return elem = (evt.target) ? evt.target :
            ((evt.srcElement) ? evt.srcElement : null);
}

function getParent(el, pTagName) {
    if (el == null) return null;
    else if (el.nodeType == 1 && el.tagName.toLowerCase() == pTagName.toLowerCase()) // Gecko bug, supposed to be uppercase
        return el;
    else
        return getParent(el.parentNode, pTagName);
}

function changeField(evt) {
    evt = (evt) ? evt : ((event) ? event : null);
    var elem = getTarget(evt);
    var index = elem.selectedIndex;
    var td = getParent(elem, "td");
    internalChangeField(td, index, value1Defaults[index], value2Defaults[index]);
}

function internalChangeField(td, index, value1, value2) {
    if (td.firstChild.selectedIndex != index)
        td.firstChild.selectedIndex = index;
    var type = types.charAt(index);
    var tr = getParent(td, "tr");
    td = td.nextSibling;
    if (type != td.firstChild.className.charAt(0)) {
        td.removeChild(td.firstChild);
        var chooser = (type == "t") ? createTextOperatorChooser() : createNumericOperatorChooser();
        td.appendChild(chooser);
        td = td.nextSibling;
        if (type == "t") {
            td.colSpan = 3;
            td.firstChild.style.width = "11em";
            tr.removeChild(td.nextSibling);
            tr.removeChild(td.nextSibling);
        } else {
            td.colSpan = 1;
            td.firstChild.style.width = "";
            tr.insertBefore(createNumericCell(), td.nextSibling);
            tr.insertBefore(createAndLabelCell(), td.nextSibling);
        }
    }
    // set defaults
    td = tr.firstChild.nextSibling;
    var option = optionDefaults.charAt(index);
    td.firstChild.selectedIndex = option;
    td = td.nextSibling;
    td.firstChild.value = value1;
    td = td.nextSibling;
    if (numericSecondArg.charAt(option) == "v") {
        td.style.visibility = "visible";
        td = td.nextSibling;
        td.firstChild.value = value2;
        td.style.visibility = "visible";
    } else {
        if (type != "t")
            td.style.visibility = td.nextSibling.style.visibility = "hidden";
    }
}

function changeNumericOperator(evt) {
    evt = (evt) ? evt : ((event) ? event : null);
    var elem = getTarget(evt);
    var visibility = numericSecondArg.charAt(elem.selectedIndex);
    var td = getParent(elem, "td");
    // Move left two cells, to the one with the text "and"
    td = td.nextSibling.nextSibling;
    if (visibility != td.style.visibility.charAt(0)) {
        if (visibility == "h")
            td.style.visibility = td.nextSibling.style.visibility = "hidden";
        else
            td.style.visibility = td.nextSibling.style.visibility = "visible";
    }
}

function createFieldChooser() {
    var chooser = document.createElement("select");
    for (var i = 0; i < fields.length; i++) {
        chooser.options[i] = new Option(fields[i], i, false, (false));
    }
    chooser.onChange = function (evt) {
        changeField(evt ? evt : window.event);
    };
    return chooser;
}

function createNumericOperatorChooser() {
    var chooser = document.createElement("select");
    chooser.className = "numeric";
    for (var i = 0; i < numericOptions.length; i++) {
        chooser.options[i] = new Option(numericOptions[i], i, false, (false));
    }
    chooser.onChange = function (evt) {
        changeNumericOperator(evt ? evt : window.event);
    };
    return chooser;
}

function createTextOperatorChooser() {
    var chooser = document.createElement("select");
    chooser.className = "text";
    for (var i = 0; i < textOptions.length; i++) {
        chooser.options[i] = new Option(textOptions[i], i, false, (false));
    }
    return chooser;
}

function createNumericInput() {
    var input = document.createElement("input");
    input.className = "numeric";
    return input;
}

function createSmallButton(text) {
    var input = document.createElement("input");
    input.type = "button";
    input.value = text;
    input.className = "smallButton";
    return input;
}

function createNumericCell() {
    var td = document.createElement("td");
    var input = createNumericInput();
    td.appendChild(input);
    td.style.visibility = 'hidden';

    return td;
}

function createAndLabelCell() {
    var td = document.createElement("td");
    var label = document.createTextNode("and");
    td.appendChild(label);
    td.className = "and";
    td.style.visibility = 'hidden';
    return td;
}

function populateRow(row, full) {
    var td = document.createElement("td");
    td.appendChild(createFieldChooser());
    row.appendChild(td);

    td = document.createElement("td");
    td.appendChild(createNumericOperatorChooser());
    row.appendChild(td);

    td = document.createElement("td");
    var input = createNumericInput();
    td.appendChild(input);
    row.appendChild(td);

    td = createAndLabelCell();
    row.appendChild(td);

    td = createNumericCell();
    row.appendChild(td);

    td = document.createElement("td");
    var input = createSmallButton("+");
    input.onclick = function (evt) {
        addRow(evt ? evt : window.event);
    };
    td.appendChild(input);
    row.appendChild(td);

    if (!full) return;

    td = document.createElement("td");
    var input = createSmallButton("-");
    input.onclick = function (evt) {
        removeRow(evt ? evt : window.event);
    };
    td.appendChild(input);
    row.appendChild(td);
}

function appendRow(elem, full) {
    var row = document.createElement("tr");
    populateRow(row, full);
    var tbody = getParent(elem, "tbody");
    if (clickToCreateRow == null) {
        clickToCreateRow = tbody.firstChild;
        tbody.removeChild(clickToCreateRow);
    }
    tbody.appendChild(row);
}

function addRow(evt) {
    evt = (evt) ? evt : ((event) ? event : null);
    if (evt) {
        var elem = getTarget(evt);
        appendRow(elem, true);
    }
}

function removeRow(evt) {
    evt = (evt) ? evt : ((event) ? event : null);
    if (evt) {
        var elem = getTarget(evt);
        var row = getParent(elem, "tr");
        var tbody = getParent(row, "tbody");
        tbody.removeChild(row);
        if (tbody.childNodes.length == 0) {
            tbody.appendChild(clickToCreateRow);
            clickToCreateRow = null;
        }
    }
}