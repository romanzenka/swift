// The cookie expires after this many days
var cookieExpirationTime = 366;

// =======================================================
$(document).ready(function() {
    if ($('#filelist')) {
        $('#filelist').scroll(function() {
            createCookie("scrollTop", $('#filelist').scrollTop, cookieExpirationTime);
        });
    }
});
