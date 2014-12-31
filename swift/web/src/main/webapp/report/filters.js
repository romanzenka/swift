// Filtering of a table - UI for buttons, dropdowns

//======================================================================================================================
// Filter button - a <th> element that contains column title + small icon + can produce dropdown menu

function FilterButton(id, title, dropdown) {
    this.id = id;
    this.title = title;
    this.dropdown = dropdown;
    if (this.dropdown)
        this.dropdown.filterButton = this;
    this.manager = null;
}

FilterButton.prototype.displayDropdown = function(evt) {
    var t = evt.data.obj;
    if (t.dropdown) {
        var id = t.id;
        var pos = $(t.root).offset();
        pos.left += $(t.root).outerWidth() - $(t.dropdown.toplevel).outerWidth();
        pos.top += $(t.root).outerHeight();

        t.dropdown.display(pos.left + "px", pos.top + "px");
    }

    evt.stopPropagation();
};

FilterButton.prototype.render = function() {
    this.root = document.createElement('th');
    this.root.className = "column";
    this.root.id = this.id;

    if (this.dropdown) {
        this.filterButton = document.createElement("a");
        this.filterButton.className = "filter_button";
        this.filterButton.href = "#";
        $(this.filterButton).on('click', {"obj": this}, this.displayDropdown);
    }

    if (this.dropdown) {
        this.sortButton = document.createElement("a");
        this.sortButton.href = "#";
    } else {
        this.sortButton = document.createElement("span");
    }
    this.sortButton.className = "sort_button" + (this.dropdown ? "" : " no_dropdown");
    $(this.sortButton).append(document.createTextNode(this.title));

    var div = document.createElement("div");
    div.style.position = "relative";
    div.style.height = "1.2em";
    this.root.appendChild(div);
    if (this.filterButton) {
        div.appendChild(this.filterButton);
    }
    div.appendChild(this.sortButton);

    return this.root;
};

FilterButton.prototype.dropdownSettingsChanged = function(isFiltered, sortOrder) {
    this.filterButton.className = "filter_button" + (isFiltered ? " filter" : "") + (sortOrder != 0 ? (sortOrder == 1 ? " atoz" : " ztoa") : "");
    if (sortOrder != 0 && this.manager) {
        this.manager.filterSortSet(this);
    }
};

FilterButton.prototype.removeSort = function() {
    if (this.dropdown)
        this.dropdown.removeSort();
};

//======================================================================================================================
// FilterdDropDown: A group of checkboxes/radio buttons/etc
function FilterDropDown(id) {
    this.id = id;
    this.toplevel = document.createElement("div");
    this.toplevel.className = "dropdown";
    this.toplevel.id = this.id + "_dropdown";

    this.form = document.createElement("form");
    this.root = document.createElement('ul');

    this.form.appendChild(this.root);
    this.toplevel.appendChild(this.form);
}

FilterDropDown.prototype.getRoot = function() {
    return this.toplevel;
};

FilterDropDown.prototype.whereSelectAll = function (evt) {
    var whereGroupId = evt.data.id;
    var t = evt.data.obj;
    for (var i = 0; i < t[whereGroupId].numOptions; i++) {
        $('#' + t.id + '_' + whereGroupId + '_' + i)[0].checked = t[whereGroupId].selectAll.checked;
    }
};

FilterDropDown.prototype.checkSelectAll = function (evt) {
    var whereGroupId = evt.data.id;
    var t = evt.data.obj;
    var allChecked = true;
    for (var i = 0; i < t[whereGroupId].numOptions; i++) {
        if (!$('#' + t.id + '_' + whereGroupId + '_' + i)[0].checked) {
            allChecked = false;
            break;
        }
    }
    t[whereGroupId].selectAll.checked = allChecked;
};

// Automatically creates a "select all" checkbox as the first one of the group
// The id is an id for the group (unique within this dropdown)
// The titleArray parameter is an array of strings to be displayed for the user.
// The sqlArray parameter is an array of sql where clause parts, such as column = 'hello'
// allChecked - when true - all the checkboxes are checked
FilterDropDown.prototype.addCheckboxes = function(id, type, titleArray, sqlArray, allChecked) {
    var groupId = this.id + '_' + id;
    var checked = allChecked ? 'checked' : '';
    var selectAll = document.createElement('li');
    $(selectAll).append('<label for="' + groupId + '_all"><input type="checkbox" ' + checked + ' value="All" id="' + groupId + '_all">(Select all)</label>');
    var selectAllButton = $(selectAll).find('input')[0];
    this[id] = {
        'type': type,
        'selectAll' : selectAllButton,
        'numOptions' : titleArray.length,
        'isFiltering' : function() {
            return type == "where" && !selectAllButton.checked;
        },
        'getFilterValue' : function() {
            var checkedList = new Array();
            if (type != "where") return checkedList;
            for (var i = 0; i < this.checkboxes.length; i++)
                if (this.checkboxes[i].checked) {
                    checkedList.push(this.checkboxes[i].value);
                }
            return checkedList;
        }
    };
    $(selectAllButton).on('click', {"id": id, "obj": this}, this.whereSelectAll);

    $(this.root).append(selectAll);

    this[id].checkboxes = new Array(titleArray.length);
    this[id].storeValues = function() {
        this.storedValues = new Array(this.numOptions + 1);
        this.storedValues[0] = this.selectAll.checked;
        for (var i = 0; i < this.numOptions; i++) this.storedValues[i + 1] = this.checkboxes[i].checked;
    };
    this[id].restoreValues = function() {
        this.selectAll.checked = this.storedValues[0];
        for (var i = 0; i < this.numOptions; i++) this.checkboxes[i].checked = this.storedValues[i + 1];
    };
    this[id].saveToCookie = function() {
        var result = new Object();
        for (var i = 0; i < this.numOptions; i++) {
            if (this.checkboxes[i].checked) {
                result[this.checkboxes[i].value] = 1;
            }
        }
        return cookieHashToString(result);
    };
    this[id].loadFromCookie = function(cookie) {
        var hash = cookieStringToHash(cookie);
        if (hash == null) {
            return;
        }
        for (var i = 0; i < this.checkboxes.length; i++) {
            this.checkboxes[i].checked = false;
        }
        for (var i = 0; i < this.checkboxes.length; i++) {
            if (hash[this.checkboxes[i].value] == 1) {
                this.checkboxes[i].checked = true;
            }
        }
        var allSelected = true;
        for (var i = 0; i < this.checkboxes.length; i++) {
            if (!this.checkboxes[i].checked) {
                allSelected = false;
                break;
            }
        }
        this.selectAll.checked = allSelected;
    };

    for (var i = 0; i < titleArray.length; i++) {
        var checkboxId = groupId + '_' + i;
        $(this.root).append('<li><input type="checkbox" ' + checked + ' value="' + sqlArray[i] + '" id="' + checkboxId + '"><label for="' + checkboxId + '">' + titleArray[i] + '</label></li>');
        var checkboxes = $(this.root).find('input');
        var checkbox = checkboxes[checkboxes.length - 1];
        this[id].checkboxes[i] = checkbox;
        $(checkbox).on('click', {"id": id, "obj": this}, this.checkSelectAll);
    }
};

// The id is an id for the group (unique within this dropdown).
// You can create several groups that share one id.
// The titleArray parameter is an array of strings to be displayed for the user.
// The sqlArray parameter is an array of sql where clause parts, such as column = 'hello'
// indexChecked - index of the initially checked item (or -1 if none is checked)
FilterDropDown.prototype.addRadioButtons = function(id, type, titleArray, sqlArray, indexChecked) {
    var groupId = this.id + '_' + id;
    var offset = 0;
    if (!this[id]) {
        this[id] = {
            'type': type,
            'numOptions' : titleArray.length,
            'isFiltering' : function() {
                if (type != "where") return false;
                for (var i = 0; i < this.radios.length; i++) if (this.radios[i].checked && this.radios[i].value != "") return true;
                return false;
            },
            'getSortOrder' : function() {
                if (type != "order") return 0;
                for (var i = 0; i < this.radios.length; i++)
                    if (this.radios[i].checked)
                        return this.radios[i].value;
                return 0;
            },
            'removeS' : function() {
                if (type != "order") return false;
                var removed = false;
                for (var i = 0; i < this.radios.length; i++)
                    if (this.radios[i].checked) {
                        this.radios[i].checked = false;
                        removed = true;
                    }
                return removed;
            }};
        this[id].radios = new Array(titleArray.length);
    }
    else {
        offset = this[id].numOptions;
        this[id].numOptions += titleArray.length;
    }

    this[id].storeValues = function() {
        if (!this.storedValues)
            this.storedValues = new Array(this.numOptions);
        for (var i = 0; i < this.numOptions; i++) this.storedValues[i] = this.radios[i].checked;
    };
    this[id].restoreValues = function() {
        for (var i = 0; i < this.numOptions; i++) this.radios[i].checked = this.storedValues[i];
    };
    this[id].saveToCookie = function() {
        var checkedOption = "";
        for (var i = 0; i < this.numOptions; i++) {
            if (this.radios[i].checked) {
                checkedOption = this.radios[i].value;
                break;
            }
        }
        return checkedOption;
    };
    this[id].loadFromCookie = function(cookie) {
        for (var i = 0; i < this.numOptions; i++) {
            if (this.radios[i].value == cookie) {
                this.radios[i].checked = true;
                break;
            }
        }
    };

    for (var i = 0; i < titleArray.length; i++) {
        var realId = offset + i;
        var checked = realId == indexChecked ? 'checked' : '';
        $(this.root).append('<li><input type="radio" name="' + groupId + '" ' + checked + ' value="' + sqlArray[i] + '" id="' + groupId + '_' + realId + '"><label for="' + groupId + '_' + realId + '">' + titleArray[i] + '</label></li>');
        var radios = $(this.root).find('input');
        this[id].radios[realId] = radios[radios.length - 1];
    }
};

if (!String.prototype.trim) {
  String.prototype.trim = function () {
    return this.replace(/^\s+|\s+$/g, '');
  };
}

// The id is an id for the textbox (unique within this dropdown).
FilterDropDown.prototype.addTextBox = function(id) {
    var itemId = this.id + '_' + id;
    var offset = 0;
    this[id] = {
        'type': "where",
        'isFiltering' : function() {
            if(this.textbox.value) {
                return this.textbox.value.trim() != "";
            } else {
                return false;
            }
        },
        'getFilterValue' : function() {
            return this.textbox.value;
        },
        'removeS' : function() {
            return false;
        }};
    this[id].storeValues = function() {
        if (!this.storedValues)
            this.storedValues = this.textbox.value;
    };
    this[id].restoreValues = function() {
        this.textbox.value = this.storedValues;
    };
    this[id].saveToCookie = function() {
        return this.textbox.value;
    };
    this[id].loadFromCookie = function(cookie) {
        this.textbox.value = cookie;
    };

    $(this.root).append('<li><input type="text" name="' + itemId + '" value="" id="' + itemId + '"></li>');
    var inputs = $(this.root).find('input');
    this[id].textbox = inputs[inputs.length - 1];
};

FilterDropDown.prototype.addText = function(text) {
    $(this.root).append('<li>' + text + '</li>');
};

FilterDropDown.prototype.display = function(left, top) {
    this.storeValues();
    $('#popupMask').css("display", 'block');
    $(this.toplevel)
        .css("left", left)
        .css("top", top)
        .css("display", "block");
};

FilterDropDown.prototype.hide = function() {
    $('#popupMask').css("display", 'none');
    $(this.toplevel)
        .css("display", 'none');
};

FilterDropDown.prototype.cancel = function(evt) {
    evt.data.obj.hide();
    evt.data.obj.restoreValues();
    evt.stopPropagation();
};

FilterDropDown.prototype.onSubmitCallback = null;

FilterDropDown.prototype.submit = function(evt) {
    var t = evt.data.obj;
    t.hide();
    t.storeValues();
    t.updateFilterButton();

    evt.stopPropagation();
    if (t.onSubmitCallback) {
        t.onSubmitCallback();
    }
};

FilterDropDown.prototype.updateFilterButton = function() {
    this.filterButton.dropdownSettingsChanged(this.isFiltering(), this.getSortOrder());
};

FilterDropDown.prototype.storeValues = function() {
    for (var property in this) {
        if (this[property].storeValues) {
            this[property].storeValues();
        }
    }
};

FilterDropDown.prototype.restoreValues = function() {
    for (var property in this) {
        if (this[property].restoreValues) {
            this[property].restoreValues();
        }
    }
};

FilterDropDown.prototype.getCookieName = function() {
    return "fdd_" + this.id;
};

// Saves the current settings into a cookie 
FilterDropDown.prototype.saveToCookies = function() {
    var cookieName = this.getCookieName();
    var storedValues = new Object();
    for (var property in this) {
        if (this[property].saveToCookie) {
            storedValues[property] = this[property].saveToCookie();
        }
    }
    var cookieValue = cookieHashToString(storedValues);
    createCookie(cookieName, cookieValue, 365 /* 1 year cookie */);
};

// Loads the current settings from a cookie 
FilterDropDown.prototype.loadFromCookies = function() {
    var cookieName = this.getCookieName();
    var value = readCookie(cookieName);
    if (value == null || value == "") {
        return;
    }

    var storedValues = cookieStringToHash(value);
    if (storedValues == null) {
        return;
    }

    for (var property in storedValues) {
        if (this[property].loadFromCookie) {
            this[property].loadFromCookie(storedValues[property]);
        }
    }

    this.updateFilterButton();
};

FilterDropDown.prototype.isFiltering = function() {
    for (var property in this) {
        if (this[property].isFiltering && this[property].isFiltering()) {
            return true;
        }
    }
    return false;
};

FilterDropDown.prototype.getFilterValue = function() {
    var filterValue = "";
    for (var property in this) {
        if (this[property] && this[property].getFilterValue) {
            if (filterValue != "") filterValue = "," + filterValue;
            filterValue += this[property].getFilterValue();
        }
    }
    return filterValue;
};

// Sort order: 0 - none, 1 : ascending, -1 : descending
FilterDropDown.prototype.getSortOrder = function() {
    for (var property in this) {
        if (this[property] && this[property].getSortOrder) {
            var order = this[property].getSortOrder();
            if (order != 0) return order;
        }
    }
    return 0;
};

FilterDropDown.prototype.removeSort = function() {
    var sortRemoved = false;
    for (var property in this) {
        if (this[property].removeS) {
            sortRemoved = this[property].removeS() || sortRemoved;
        }
    }
    if (sortRemoved)
        this.filterButton.dropdownSettingsChanged(this.isFiltering(), this.getSortOrder());
};

FilterDropDown.prototype.addOkCancel = function() {
    $(this.root).append('<li><input type="submit" value="Ok" class="okbutton" class="okbutton"> <input type="button" value="Cancel" class="cancelbutton"></li>');

    $(this.root).find(".cancelbutton").on('click', {"obj": this}, this.cancel);
    $(this.form).on('submit', {"obj": this}, this.submit);
};

FilterDropDown.prototype.addSeparator = function() {
    $(this.root).append('<li class="hr">&nbsp;</li>');
};

FilterDropDown.prototype.getRequestString = function() {
    return "sort=" + this.getSortOrder() + ";filter=" + this.getFilterValue();
};

//======================================================================================================================
// Filter manager - operates an array of filters and makes sure only one column is sorted at the time

function FilterManager(filterArray) {
    this.filterArray = filterArray;
    for (var i = 0; i < this.filterArray.length; i++) {
        var filter = this.filterArray[i];
        filter.manager = this;
    }
}

FilterManager.prototype.filterSortSet = function(filter) {
    for (var i = 0; i < this.filterArray.length; i++) {
        var f = this.filterArray[i];
        if (f.id != filter.id)
            f.removeSort();
    }
};

