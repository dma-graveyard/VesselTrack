
/**
 * Returns the IE version. Tested for IE11 and earlier.
 */
function isIE () {
    var match = navigator.userAgent.match(/(?:MSIE |Trident\/.*; rv:)(\d+)/);
    return match ? parseInt(match[1]) : undefined;
}

String.prototype.endsWith = function (s) {
    return this.length >= s.length && this.substr(this.length - s.length) == s;
};

String.prototype.contains = function (s) {
    return this.indexOf(s) > -1;
};

/**
 * Function count the occurrences of substring in a string;
 * @param {String} subString    Required. The string to search for;
 */
String.prototype.occurrences = function(subString){

    subString+="";
    if(subString.length<=0) return this.length+1;

    var n=0, pos=0;

    while(true){
        pos=this.indexOf(subString,pos);
        if(pos>=0){ n++; pos+=subString.length; } else break;
    }
    return(n);
};

/**
 * Converts plain text to HTML
 * @param text the plain text to convert
 * @returns the HTML version of the text
 */
function plain2html(text) {
    text = (text || "");
    return text
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/\t/g, "    ")
        .replace(/ /g, "&#8203;&nbsp;&#8203;")
        .replace(/\r\n|\r|\n/g, "<br />");
}

/**
 * formats the series identifier.
 * If the number is undefined, the blank string is returned
 * @param msg the series identifier
 */
function formatSeriesIdentifier(msg) {
    if (msg && msg.seriesIdentifier && msg.seriesIdentifier.number) {
        var id = msg.seriesIdentifier.fullId;
        if (msg.type == 'TEMPORARY_NOTICE') {
            id += '(T)';
        } else if (msg.type == 'PRELIMINARY_NOTICE') {
            id += '(P)';
        }
        id += ': ';
        return id;
    }
    return '';
}

