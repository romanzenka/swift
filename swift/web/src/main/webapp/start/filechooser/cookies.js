/* Simple cookie-handling functions */
function createCookie(name, value, days) {
    var expires;
    if (days) {
        var date = new Date();
        date.setTime(date.getTime() + (days * 24 * 60 * 60 * 1000));
        expires = "; expires=" + date.toGMTString();
    } else {
        expires = "";
    }

    document.cookie = name + "=" + escape(value) + expires + "; path=/";
}

function readCookie(name) {
    var nameEQ = name + "=";
    var ca = document.cookie.split(';');
    for (var i = 0; i < ca.length; i++) {
        var c = ca[i];
        while (c.charAt(0) == ' ') {
            c = c.substring(1, c.length);
        }
        if (c.indexOf(nameEQ) == 0) {
            return unescape
                    (c.substring(nameEQ.length, c.length));
        }
    }
    return null;
}

function eraseCookie(name) {
    createCookie(name, "", -1);
}

// Returns index of next occurence of character c in s, which is not escaped by preceding backslash. 
// The search starts and startIndex.
function findNextNotEscaped(s, c, startIndex) {
    var escapeMode = false;
    var foundIndex = -1;
    for (var i = startIndex; i < s.length; i++) {
        if (s.charAt(i) == '\\') {
            escapeMode = !escapeMode;
            continue;
        }
        if (s.charAt(i) == c && !escapeMode) {
            foundIndex = i;
            break;
        }
        escapeMode = false;
    }
    return foundIndex;
}

// Single backslash - removed.
// Double backslashes -> single backslash.
function unescapeBackslashes(s) {
    var result = '';
    var escapeMode = false;
    for (var i = 0; i < s.length; i++) {
        if ('\\' == s.charAt(i)) {
            escapeMode = !escapeMode;
        } else {
            escapeMode = false;
        }
        if (!escapeMode) {
            result += s.charAt(i);
        }
    }
    return result;
}

function cookieTupleToString(key, value) {
    var newKey = key.replace(/\\/g, '\\\\');
    newKey = newKey.replace(/=/g, '\\=');
    return newKey + "=" + value;
}

function cookieStringToTuple(s) {
    if (s == null) {
        return null;
    }
    var pos = findNextNotEscaped(s, '=', 0);
    var result = null;
    if (pos != -1) {
        result = new Object();
        var escapedKey = s.substring(0, pos);
        result.key = unescapeBackslashes(escapedKey);
        result.value = s.substring(pos + 1);
    }
    return result;
}

function cookieArrayToString(array) {
    var result = "";
    for (var i = 0; i < array.length; i++) {
        var value = array[i];
        var newValue = value.replace(/\\/g, '\\\\');
        newValue = newValue.replace(/;/g, '\\;');
        if (result != "") {
            result = result + ';';
        }
        result = result + newValue;
    }
    return result;
}

function cookieStringToArray(s) {
    if (s == null) {
        return null;
    }
    var result = new Array();
    var index = 0;
    var arrayIndex = 0;

    while (index < s.length) {
        var nextSemicolon = findNextNotEscaped(s, ';', index);
        if (nextSemicolon == -1) {
            nextSemicolon = s.length;
        }

        var token = s.substring(index, nextSemicolon);
        result[arrayIndex] = unescapeBackslashes(token);

        index = nextSemicolon + 1;
        arrayIndex++;
    }
    return result;
}

function cookieHashToString(hash) {
    var result = new Array();
    var i = 0;
    for (var property in hash) {
        result[i] = cookieTupleToString(property, hash[property]);
        i++;
    }
    return cookieArrayToString(result);
}

function cookieStringToHash(hash) {
    if (hash == null) {
        return null;
    }
    var result = new Object();
    var array = cookieStringToArray(hash);
    for (var i = 0; i < array.length; i++) {
        var tuple = cookieStringToTuple(array[i]);
        if (tuple != null) {
            result[tuple.key] = tuple.value;
        }
    }
    return result;
}