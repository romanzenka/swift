// This code allows dynamic updates of "sparse arrays" - large arrays just partially populated with values
// from the server.

//======================================================================================================================
// A sparse array is any object that follows one particular convention
// 1) there is a .total member containing the total length of the array
// 2) items are stored as members named _X, where X is the ordinal number of the item
//
// Any object can be turned into a sparse array using the turnIntoSparseArray function.
// The function enriches the object by assigning SparseArray-specific methods
// If the recursive parameter is set, the method attempts to enumerate all the object's members and enrich also those
function turnIntoSparseArray(obj, recursive) {
    if (obj == null) return null;
    obj.setTotalLength = SparseArray.prototype.setTotalLength;
    obj.getItemById = SparseArray.prototype.getItemById;
    obj.execute = SparseArray.prototype.execute;
    obj.remove = SparseArray.prototype.remove;
    obj.insert = SparseArray.prototype.insert;
    obj.update = SparseArray.prototype.update;
    obj.rewrite = SparseArray.prototype.rewrite;
    obj.clearAll = SparseArray.prototype.clearAll;
    obj.fireOnChange = SparseArray.prototype.fireOnChange;
    obj.findOffsetOfId = SparseArray.prototype.findOffsetOfId;
    obj.findId = SparseArray.prototype.findId;
    obj.turnPropertiesIntoSparseArray = SparseArray.prototype.turnPropertiesIntoSparseArray;
    obj.updateProperties = SparseArray.prototype.updateProperties;
    if (recursive) {
        for (var i = 0; i < obj.total; i++) {
            var subitem = obj.getItemById(i);
            // It is an array if it has the .total property set
            if (subitem != null && subitem.total != null)
                turnIntoSparseArray(subitem, true);
        }
    }
    return obj;
}

function SparseArray() {
}

SparseArray.prototype.setTotalLength = function (length) {
    if (length < this.total)
        for (var i = length; i < this.total; i++) this["_" + i] = null;
    this.total = length;
};

SparseArray.prototype.getItemById = function (id) {
    return this['_' + id];
};

SparseArray.prototype.execute = function (updateProgram) {
    eval(updateProgram);
};

SparseArray.prototype.remove = function (removeList) {
    removeList.sort(function (a, b) {
        return a < b ? -1 : (a > b ? 1 : 0);
    });

    // Make sure we are not removing elements after the end of the array
    while (removeList[removeList.length - 1] >= this.total)
        removeList.pop();

    var totalItemsToRemove = removeList.length;

    // To stop evaluation
    removeList.push(this.total);

    var toPos = removeList[0];
    var fromPos = removeList[0];
    var rPos = 0;
    while (rPos < removeList.length) {
        // Run until the next removal
        while (fromPos < removeList[rPos]) {
            // If there is anything to do (do not copy void to void)
            if (this["_" + toPos] || this["_" + fromPos])
                this["_" + toPos] = this["_" + fromPos];
            toPos++;
            fromPos++;
        }
        // Skip all the removed items
        while (rPos < removeList.length && removeList[rPos] == fromPos) {
            fromPos++;
            rPos++;
        }
    }
    this.setTotalLength(this.total - totalItemsToRemove);
};

SparseArray.prototype.turnPropertiesIntoSparseArray = function (item) {
    for (var property in item) {
        if (item[property] && item[property].total) {
            turnIntoSparseArray(item[property]);
            if (this.onchange) item[property].onchange = this.onchange;
        }
    }
    return item;
};

// insertList : array of objects to be inserted. The "_" property determines the index where to insert the new object.
// After the insert is done, each inserted object will be placed where the user requested and the rest of the
// array would be shifted accordingly. Example:
// A B C D + insert {_:0 X},{_:3 Y},{_:7:Z} = X A B Y C D _ Z
SparseArray.prototype.insert = function (insertList) {
    // Sort the inserts ascendingly
    insertList.sort(function (a, b) {
        return a._ < b._ ? -1 : (a._ > b._ ? 1 : 0);
    });

    // Filter out all the items that already exist in our array
    var newList = new Array();
    for (var i = 0; i < insertList.length; i++) {
        if (!this.findId(insertList[i].id))
            newList.push(insertList[i]);
    }
    insertList = newList;

    // The last element of the input array
    var fromPos = this.total - 1;
    // The item to be inserted now is the last one in the insert list
    var iPos = insertList.length - 1;
    // This is the expected length of the array after all inserts run
    var expectedLength = this.total + insertList.length;

    // Skip all the inserts that happen behind the expected length of the array
    var skippedInserts = 0;
    while (iPos >= 0 && insertList[iPos]._ >= expectedLength) {
        iPos--;
        expectedLength--;
        skippedInserts++;
    }

    // We start writing at the end of the array of expected length
    var toPos = expectedLength - 1;

    // Run while we have items to insert
    while (iPos >= 0) {
        // Move objects until the next insert position
        while (toPos > insertList[iPos]._) {
            // If there is anything to do (do not copy void to void)
            if (this["_" + toPos] || this["_" + fromPos])
                this["_" + toPos] = this["_" + fromPos];
            toPos--;
            fromPos--;
        }
        // Now the toPos points to the place where we should insert
        while (iPos >= 0 && insertList[iPos]._ == toPos) {
            this["_" + toPos] = this.turnPropertiesIntoSparseArray(insertList[iPos]);
            toPos--;
            iPos--;
        }
    }

    // Now simply copy the extraneous ending items to the end of the array
    for (var i = insertList.length - skippedInserts; i < insertList.length; i++) {
        var target = this["_" + insertList[i]._];
        target = this.turnPropertiesIntoSparseArray(insertList[i]);
    }

    // Total length of the array is either the expected one or determined by the last inserted position
    this.setTotalLength(Math.max(expectedLength, insertList[insertList.length - 1]._ + 1));
};

// @param list: array of objects to be rewritten. The "_" property determines the index, if this is not specified,
// "id" property determines the item id.
// The rewrite completely discards the original object properties, inserting a new set instead
SparseArray.prototype.rewrite = function (list) {
    var maxLength = 0;
    for (var i = 0; i < list.length; i++) {
        var pos = -1;
        if (list[i].id != null) {
            pos = this.findOffsetOfId(list[i].id);
        } else if (list[i]._ != null) {
            pos = list[i]._;
        }
        if (pos < 0) continue;
        if (pos + 1 > maxLength) maxLength = pos + 1;
        this["_" + pos] = list[i];
    }

    if (maxLength > this.total)
        this.setTotalLength(maxLength);
};

SparseArray.prototype.updateProperties = function (source, target) {
    for (var property in source) {
        if ((target[property] && target[property].total) &&
            (source[property] && source[property].total))
            this.updateProperties(source[property], target[property]);
        else
            target[property] = source[property];
    }
};

// @param list: array of objects containing the updates. The "_" property determines the index, if this is not specified,
// "id" property determines the item id.
// The update keeps original object properties, replacing only the specified ones with new values.
SparseArray.prototype.update = function (list) {
    var maxUpdate = 0;
    for (var i = 0; i < list.length; i++) {
        var target;
        if (list[i].id != null) {
            target = this.findId(list[i].id);
        } else if (list[i]._ != null) {
            target = this["_" + list[i]._];
        }
        if (target) {
            var source = list[i];
            this.updateProperties(source, target);
        }
    }
};

SparseArray.prototype.clearAll = function () {
    for (var i = 0; i < this.total; i++) {
        this["_" + i] = null;
    }
    this.setTotalLength(0);
    return this;
};

SparseArray.prototype.fireOnChange = function () {
    if (this.onchange) this.onchange();
    return this;
};

// Searches for a given ID
// TODO: Optimize this using hashmap (?)
SparseArray.prototype.findOffsetOfId = function (id) {
    for (var i = 0; i < this.total; i++) {
        if (this["_" + i].id == id) {
            return i;
        }
    }
    return -1;
};

// Searches for a given ID
SparseArray.prototype.findId = function (id) {
    var offset = this.findOffsetOfId(id);
    if (offset < 0) return null;
    return this["_" + offset];
};

//======================================================================================================================
// Utilities

function removeAllClicks(element) {
    if ($(element).nodeType !== Node.ELEMENT_NODE) {
        return;
    }
    try {
        var descendants = Element.descendants(element);
        for (var i = 0; i<descendants.length; i++) {
            Event.stopObserving(descendants[i]);
        }
        Event.stopObserving(element);
    } catch (err) {
        // Ignore
    }
}

// Removes all children of an element
function removeChildren(element) {
    while (element.childNodes.length > 0) {
        removeAllClicks(childNodes[0]);
        element.removeChild(childNodes[0]);
    }
}

// Removes all children of an element except those whose class contains string noRemove
function removeChildrenExcept(element, exceptClassRegex) {
    var exceptRegex = exceptClassRegex ? exceptClassRegex : /noRemove/i;
    var nodes = new Array();
    var len = element.childNodes.length;
    for (var j = 0; j < len; ++j) {
        nodes.push(element.childNodes[j]);
    }
    len = nodes.length;
    for (var i = 0; i < len; ++i) {
        if (!nodes[i].className || !nodes[i].className.match(exceptRegex)) {
            removeAllClicks(nodes[i]);
            element.removeChild(nodes[i]);
        }
    }
}
