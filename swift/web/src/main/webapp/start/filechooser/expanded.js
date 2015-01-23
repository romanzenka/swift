/* A data structure that keeps a list of currently expanded folders and serializes them in a compact format
 into one (or several) cookies */

function ExpandedList() {
    this.list = new Array();
}

ExpandedList.prototype.list = null;
ExpandedList.prototype.maxCookieLength = 3800;
ExpandedList.prototype.maxNumCookies = 10;
ExpandedList.prototype.cookiePrefix = "exp";
ExpandedList.prototype.cookieExpirationDays = 366;

// Return name of the n-th cookie
ExpandedList.prototype.getCookieName = function(n) {
    return this.cookiePrefix + n;
};

// Make sure the path ends with a slash
ExpandedList.prototype.addTerminatingSlash = function(path) {
    if (path.match(/[^\/]$/))
        return path + '/';
    return path;
};

// Returns true if prefix is a prefix of path (can be also identical to path)
ExpandedList.prototype.isPrefix = function(prefix, path) {
    return path.substring(0, prefix.length) == prefix;
};

// Adds a given path into the list of expanded paths.
ExpandedList.prototype.expand = function(newPath) {
    newPath = this.addTerminatingSlash(newPath);
    var newList = new Array();
    for (var i = 0; i < this.list.length; i++) {
        var listPath = this.list[i];

        // If newPath is prefix of some path already listed, our job is done.
        if (this.isPrefix(newPath, listPath))
            return;

        // Discard all listPaths that are prefix to the newPath
        if (!this.isPrefix(listPath, newPath))
            newList.push(listPath);
    }
    newList.push(newPath);
    this.list = newList;
};

// Removes given path (and all subpaths) from the expanded list.
ExpandedList.prototype.collapse = function(newPath) {
    newPath = this.addTerminatingSlash(newPath);
    var newList = new Array();
    for (var i = 0; i < this.list.length; i++) {
        var listPath = this.list[i];

        // All paths that are prefixed by newPath are discarded during the collapse
        if (!this.isPrefix(newPath, listPath))
            newList.push(listPath);
    }
    this.list = newList;
};

// Stores the list into one (or several) cookies. If there is not enough cookies, some data can be lost.
ExpandedList.prototype.saveToCookies = function() {
    this.list.sort();
    var data = "";
    var maxLen = this.maxCookieLength * this.maxNumCookies;
    for (var i = 0; i < this.list.length; i++) {
        var listPath = escape(this.list[i]);
        if (data.length + 1 + listPath.length <= maxLen) {
            if (i != 0) data += "|";
            data += listPath;
        }
    }
    // Now we have data filled with the list of all paths (except those that cannot be stored).
    // Let us chop it into pieces and store into cookies.
    var cookieNumber = 0;
    while (data.length > 0) {
        createCookie(this.getCookieName(cookieNumber), data.substring(0, Math.min(this.maxCookieLength, data.length)), this.cookieExpirationDays);
        data = data.length > this.maxCookieLength ? data.substring(this.maxCookieLength) : "";
        cookieNumber++;
    }
    for (; cookieNumber < this.maxNumCookies; cookieNumber++) {
        eraseCookie(this.getCookieName(cookieNumber));
    }
};

// Loads the list from the cookies.
ExpandedList.prototype.loadFromCookies = function() {
    var data = "";
    for (var i = 0; i < this.maxNumCookies; i++) {
        var cookie = readCookie(this.getCookieName(i));
        if (cookie) data += cookie;
    }
    this.list = data.split("|");
    if (!this.list) this.list = new Array();
    for (var i = 0; i < this.list.length; i++) {
        this.list[i] = unescape(this.list[i]);
    }
};

// Returns the list of expanded paths as a | separated string.
ExpandedList.prototype.getListAsString = function() {
    var data = "";
    for (var i = 0; i < this.list.length; i++) {
        if (i != 0) data += "|";
        data += this.list[i];
    }
    return data;
};