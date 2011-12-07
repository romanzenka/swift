/*
 * Sizzle CSS Selector Engine - v1.0
 *  Copyright 2009, The Dojo Foundation
 *  Released under the MIT, BSD, and GPL Licenses.
 *  More information: http://sizzlejs.com/
 */
(function() {
    var O = /((?:\((?:\([^()]+\)|[^()]+)+\)|\[(?:\[[^[\]]*\]|['"][^'"]*['"]|[^[\]'"]+)+\]|\\.|[^ >+~,(\[\\]+)+|[>+~])(\s*,\s*)?/g,H = 0,D = Object.prototype.toString,M = false;
    var B = function(d, T, a, V) {
        a = a || [];
        var Q = T = T || document;
        if (T.nodeType !== 1 && T.nodeType !== 9) {
            return[];
        }
        if (!d || typeof d !== "string") {
            return a;
        }
        var b = [],c,Y,g,f,Z,S,R = true,W = N(T);
        O.lastIndex = 0;
        while ((c = O.exec(d)) !== null) {
            b.push(c[1]);
            if (c[2]) {
                S = RegExp.rightContext;
                break;
            }
        }
        if (b.length > 1 && I.exec(d)) {
            if (b.length === 2 && E.relative[b[0]]) {
                Y = F(b[0] + b[1], T);
            } else {
                Y = E.relative[b[0]] ? [T] : B(b.shift(), T);
                while (b.length) {
                    d = b.shift();
                    if (E.relative[d]) {
                        d += b.shift();
                    }
                    Y = F(d, Y);
                }
            }
        } else {
            if (!V && b.length > 1 && T.nodeType === 9 && !W && E.match.ID.test(b[0]) && !E.match.ID.test(b[b.length - 1])) {
                var h = B.find(b.shift(), T, W);
                T = h.expr ? B.filter(h.expr, h.set)[0] : h.set[0];
            }
            if (T) {
                var h = V ? {expr:b.pop(),set:A(V)} : B.find(b.pop(), b.length === 1 && (b[0] === "~" || b[0] === "+") && T.parentNode ? T.parentNode : T, W);
                Y = h.expr ? B.filter(h.expr, h.set) : h.set;
                if (b.length > 0) {
                    g = A(Y);
                } else {
                    R = false;
                }
                while (b.length) {
                    var U = b.pop(),X = U;
                    if (!E.relative[U]) {
                        U = "";
                    } else {
                        X = b.pop();
                    }
                    if (X == null) {
                        X = T;
                    }
                    E.relative[U](g, X, W);
                }
            } else {
                g = b = [];
            }
        }
        if (!g) {
            g = Y;
        }
        if (!g) {
            throw"Syntax error, unrecognized expression: " + (U || d);
        }
        if (D.call(g) === "[object Array]") {
            if (!R) {
                a.push.apply(a, g);
            } else {
                if (T && T.nodeType === 1) {
                    for (var e = 0; g[e] != null; e++) {
                        if (g[e] && (g[e] === true || g[e].nodeType === 1 && G(T, g[e]))) {
                            a.push(Y[e]);
                        }
                    }
                } else {
                    for (var e = 0; g[e] != null; e++) {
                        if (g[e] && g[e].nodeType === 1) {
                            a.push(Y[e]);
                        }
                    }
                }
            }
        } else {
            A(g, a);
        }
        if (S) {
            B(S, Q, a, V);
            B.uniqueSort(a);
        }
        return a;
    };
    B.uniqueSort = function(R) {
        if (C) {
            M = false;
            R.sort(C);
            if (M) {
                for (var Q = 1; Q < R.length; Q++) {
                    if (R[Q] === R[Q - 1]) {
                        R.splice(Q--, 1);
                    }
                }
            }
        }
    };
    B.matches = function(Q, R) {
        return B(Q, null, null, R);
    };
    B.find = function(X, Q, Y) {
        var W,U;
        if (!X) {
            return[];
        }
        for (var T = 0,S = E.order.length; T < S; T++) {
            var V = E.order[T],U;
            if ((U = E.match[V].exec(X))) {
                var R = RegExp.leftContext;
                if (R.substr(R.length - 1) !== "\\") {
                    U[1] = (U[1] || "").replace(/\\/g, "");
                    W = E.find[V](U, Q, Y);
                    if (W != null) {
                        X = X.replace(E.match[V], "");
                        break;
                    }
                }
            }
        }
        if (!W) {
            W = Q.getElementsByTagName("*");
        }
        return{set:W,expr:X};
    };
    B.filter = function(a, Z, d, T) {
        var S = a,f = [],X = Z,V,Q,W = Z && Z[0] && N(Z[0]);
        while (a && Z.length) {
            for (var Y in E.filter) {
                if ((V = E.match[Y].exec(a)) != null) {
                    var R = E.filter[Y],e,c;
                    Q = false;
                    if (X == f) {
                        f = [];
                    }
                    if (E.preFilter[Y]) {
                        V = E.preFilter[Y](V, X, d, f, T, W);
                        if (!V) {
                            Q = e = true;
                        } else {
                            if (V === true) {
                                continue;
                            }
                        }
                    }
                    if (V) {
                        for (var U = 0; (c = X[U]) != null; U++) {
                            if (c) {
                                e = R(c, V, U, X);
                                var b = T ^ !!e;
                                if (d && e != null) {
                                    if (b) {
                                        Q = true;
                                    } else {
                                        X[U] = false;
                                    }
                                } else {
                                    if (b) {
                                        f.push(c);
                                        Q = true;
                                    }
                                }
                            }
                        }
                    }
                    if (e !== undefined) {
                        if (!d) {
                            X = f;
                        }
                        a = a.replace(E.match[Y], "");
                        if (!Q) {
                            return[];
                        }
                        break;
                    }
                }
            }
            if (a == S) {
                if (Q == null) {
                    throw"Syntax error, unrecognized expression: " + a;
                } else {
                    break;
                }
            }
            S = a;
        }
        return X;
    };
    var E = B.selectors = {order:["ID","NAME","TAG"],match:{ID:/#((?:[\w\u00c0-\uFFFF_-]|\\.)+)/,CLASS:/\.((?:[\w\u00c0-\uFFFF_-]|\\.)+)/,NAME:/\[name=['"]*((?:[\w\u00c0-\uFFFF_-]|\\.)+)['"]*\]/,ATTR:/\[\s*((?:[\w\u00c0-\uFFFF_-]|\\.)+)\s*(?:(\S?=)\s*(['"]*)(.*?)\3|)\s*\]/,TAG:/^((?:[\w\u00c0-\uFFFF\*_-]|\\.)+)/,CHILD:/:(only|nth|last|first)-child(?:\((even|odd|[\dn+-]*)\))?/,POS:/:(nth|eq|gt|lt|first|last|even|odd)(?:\((\d*)\))?(?=[^-]|$)/,PSEUDO:/:((?:[\w\u00c0-\uFFFF_-]|\\.)+)(?:\((['"]*)((?:\([^\)]+\)|[^\2\(\)]*)+)\2\))?/},attrMap:{"class":"className","for":"htmlFor"},attrHandle:{href:function(Q) {
        return Q.getAttribute("href");
    }},relative:{"+":function(X, Q, W) {
        var U = typeof Q === "string",Y = U && !/\W/.test(Q),V = U && !Y;
        if (Y && !W) {
            Q = Q.toUpperCase();
        }
        for (var T = 0,S = X.length,R; T < S; T++) {
            if ((R = X[T])) {
                while ((R = R.previousSibling) && R.nodeType !== 1) {
                }
                X[T] = V || R && R.nodeName === Q ? R || false : R === Q;
            }
        }
        if (V) {
            B.filter(Q, X, true);
        }
    },">":function(W, R, X) {
        var U = typeof R === "string";
        if (U && !/\W/.test(R)) {
            R = X ? R : R.toUpperCase();
            for (var S = 0,Q = W.length; S < Q; S++) {
                var V = W[S];
                if (V) {
                    var T = V.parentNode;
                    W[S] = T.nodeName === R ? T : false;
                }
            }
        } else {
            for (var S = 0,Q = W.length; S < Q; S++) {
                var V = W[S];
                if (V) {
                    W[S] = U ? V.parentNode : V.parentNode === R;
                }
            }
            if (U) {
                B.filter(R, W, true);
            }
        }
    },"":function(T, R, V) {
        var S = H++,Q = P;
        if (!R.match(/\W/)) {
            var U = R = V ? R : R.toUpperCase();
            Q = L;
        }
        Q("parentNode", R, S, T, U, V);
    },"~":function(T, R, V) {
        var S = H++,Q = P;
        if (typeof R === "string" && !R.match(/\W/)) {
            var U = R = V ? R : R.toUpperCase();
            Q = L;
        }
        Q("previousSibling", R, S, T, U, V);
    }},find:{ID:function(R, S, T) {
        if (typeof S.getElementById !== "undefined" && !T) {
            var Q = S.getElementById(R[1]);
            return Q ? [Q] : [];
        }
    },NAME:function(S, V, W) {
        if (typeof V.getElementsByName !== "undefined") {
            var R = [],U = V.getElementsByName(S[1]);
            for (var T = 0,Q = U.length; T < Q; T++) {
                if (U[T].getAttribute("name") === S[1]) {
                    R.push(U[T]);
                }
            }
            return R.length === 0 ? null : R;
        }
    },TAG:function(Q, R) {
        return R.getElementsByTagName(Q[1]);
    }},preFilter:{CLASS:function(T, R, S, Q, W, X) {
        T = " " + T[1].replace(/\\/g, "") + " ";
        if (X) {
            return T;
        }
        for (var U = 0,V; (V = R[U]) != null; U++) {
            if (V) {
                if (W ^ (V.className && (" " + V.className + " ").indexOf(T) >= 0)) {
                    if (!S) {
                        Q.push(V);
                    }
                } else {
                    if (S) {
                        R[U] = false;
                    }
                }
            }
        }
        return false;
    },ID:function(Q) {
        return Q[1].replace(/\\/g, "");
    },TAG:function(R, Q) {
        for (var S = 0; Q[S] === false; S++) {
        }
        return Q[S] && N(Q[S]) ? R[1] : R[1].toUpperCase();
    },CHILD:function(Q) {
        if (Q[1] == "nth") {
            var R = /(-?)(\d*)n((?:\+|-)?\d*)/.exec(Q[2] == "even" && "2n" || Q[2] == "odd" && "2n+1" || !/\D/.test(Q[2]) && "0n+" + Q[2] || Q[2]);
            Q[2] = (R[1] + (R[2] || 1)) - 0;
            Q[3] = R[3] - 0;
        }
        Q[0] = H++;
        return Q;
    },ATTR:function(U, R, S, Q, V, W) {
        var T = U[1].replace(/\\/g, "");
        if (!W && E.attrMap[T]) {
            U[1] = E.attrMap[T];
        }
        if (U[2] === "~=") {
            U[4] = " " + U[4] + " ";
        }
        return U;
    },PSEUDO:function(U, R, S, Q, V) {
        if (U[1] === "not") {
            if (U[3].match(O).length > 1 || /^\w/.test(U[3])) {
                U[3] = B(U[3], null, null, R);
            } else {
                var T = B.filter(U[3], R, S, true ^ V);
                if (!S) {
                    Q.push.apply(Q, T);
                }
                return false;
            }
        } else {
            if (E.match.POS.test(U[0]) || E.match.CHILD.test(U[0])) {
                return true;
            }
        }
        return U;
    },POS:function(Q) {
        Q.unshift(true);
        return Q;
    }},filters:{enabled:function(Q) {
        return Q.disabled === false && Q.type !== "hidden";
    },disabled:function(Q) {
        return Q.disabled === true;
    },checked:function(Q) {
        return Q.checked === true;
    },selected:function(Q) {
        Q.parentNode.selectedIndex;
        return Q.selected === true;
    },parent:function(Q) {
        return !!Q.firstChild;
    },empty:function(Q) {
        return !Q.firstChild;
    },has:function(S, R, Q) {
        return !!B(Q[3], S).length;
    },header:function(Q) {
        return/h\d/i.test(Q.nodeName);
    },text:function(Q) {
        return"text" === Q.type;
    },radio:function(Q) {
        return"radio" === Q.type;
    },checkbox:function(Q) {
        return"checkbox" === Q.type;
    },file:function(Q) {
        return"file" === Q.type;
    },password:function(Q) {
        return"password" === Q.type;
    },submit:function(Q) {
        return"submit" === Q.type;
    },image:function(Q) {
        return"image" === Q.type;
    },reset:function(Q) {
        return"reset" === Q.type;
    },button:function(Q) {
        return"button" === Q.type || Q.nodeName.toUpperCase() === "BUTTON";
    },input:function(Q) {
        return/input|select|textarea|button/i.test(Q.nodeName);
    }},setFilters:{first:function(R, Q) {
        return Q === 0;
    },last:function(S, R, Q, T) {
        return R === T.length - 1;
    },even:function(R, Q) {
        return Q % 2 === 0;
    },odd:function(R, Q) {
        return Q % 2 === 1;
    },lt:function(S, R, Q) {
        return R < Q[3] - 0;
    },gt:function(S, R, Q) {
        return R > Q[3] - 0;
    },nth:function(S, R, Q) {
        return Q[3] - 0 == R;
    },eq:function(S, R, Q) {
        return Q[3] - 0 == R;
    }},filter:{PSEUDO:function(W, S, T, X) {
        var R = S[1],U = E.filters[R];
        if (U) {
            return U(W, T, S, X);
        } else {
            if (R === "contains") {
                return(W.textContent || W.innerText || "").indexOf(S[3]) >= 0;
            } else {
                if (R === "not") {
                    var V = S[3];
                    for (var T = 0,Q = V.length; T < Q; T++) {
                        if (V[T] === W) {
                            return false;
                        }
                    }
                    return true;
                }
            }
        }
    },CHILD:function(Q, T) {
        var W = T[1],R = Q;
        switch (W) {
            case"only":
            case"first":
                while (R = R.previousSibling) {
                    if (R.nodeType === 1) {
                        return false;
                    }
                }
                if (W == "first") {
                    return true;
                }
                R = Q;
            case"last":
                while (R = R.nextSibling) {
                    if (R.nodeType === 1) {
                        return false;
                    }
                }
                return true;
            case"nth":
                var S = T[2],Z = T[3];
                if (S == 1 && Z == 0) {
                    return true;
                }
                var V = T[0],Y = Q.parentNode;
                if (Y && (Y.sizcache !== V || !Q.nodeIndex)) {
                    var U = 0;
                    for (R = Y.firstChild; R; R = R.nextSibling) {
                        if (R.nodeType === 1) {
                            R.nodeIndex = ++U;
                        }
                    }
                    Y.sizcache = V;
                }
                var X = Q.nodeIndex - Z;
                if (S == 0) {
                    return X == 0;
                } else {
                    return(X % S == 0 && X / S >= 0);
                }
        }
    },ID:function(R, Q) {
        return R.nodeType === 1 && R.getAttribute("id") === Q;
    },TAG:function(R, Q) {
        return(Q === "*" && R.nodeType === 1) || R.nodeName === Q;
    },CLASS:function(R, Q) {
        return(" " + (R.className || R.getAttribute("class")) + " ").indexOf(Q) > -1;
    },ATTR:function(V, T) {
        var S = T[1],Q = E.attrHandle[S] ? E.attrHandle[S](V) : V[S] != null ? V[S] : V.getAttribute(S),W = Q + "",U = T[2],R = T[4];
        return Q == null ? U === "!=" : U === "=" ? W === R : U === "*=" ? W.indexOf(R) >= 0 : U === "~=" ? (" " + W + " ").indexOf(R) >= 0 : !R ? W && Q !== false : U === "!=" ? W != R : U === "^=" ? W.indexOf(R) === 0 : U === "$=" ? W.substr(W.length - R.length) === R : U === "|=" ? W === R || W.substr(0, R.length + 1) === R + "-" : false;
    },POS:function(U, R, S, V) {
        var Q = R[2],T = E.setFilters[Q];
        if (T) {
            return T(U, S, R, V);
        }
    }}};
    var I = E.match.POS;
    for (var K in E.match) {
        E.match[K] = new RegExp(E.match[K].source + /(?![^\[]*\])(?![^\(]*\))/.source);
    }
    var A = function(R, Q) {
        R = Array.prototype.slice.call(R);
        if (Q) {
            Q.push.apply(Q, R);
            return Q;
        }
        return R;
    };
    try {
        Array.prototype.slice.call(document.documentElement.childNodes);
    } catch(J) {
        A = function(U, T) {
            var R = T || [];
            if (D.call(U) === "[object Array]") {
                Array.prototype.push.apply(R, U);
            } else {
                if (typeof U.length === "number") {
                    for (var S = 0,Q = U.length; S < Q; S++) {
                        R.push(U[S]);
                    }
                } else {
                    for (var S = 0; U[S]; S++) {
                        R.push(U[S]);
                    }
                }
            }
            return R;
        };
    }
    var C;
    if (document.documentElement.compareDocumentPosition) {
        C = function(R, Q) {
            var S = R.compareDocumentPosition(Q) & 4 ? -1 : R === Q ? 0 : 1;
            if (S === 0) {
                M = true;
            }
            return S;
        };
    } else {
        if ("sourceIndex" in document.documentElement) {
            C = function(R, Q) {
                var S = R.sourceIndex - Q.sourceIndex;
                if (S === 0) {
                    M = true;
                }
                return S;
            };
        } else {
            if (document.createRange) {
                C = function(T, R) {
                    var S = T.ownerDocument.createRange(),Q = R.ownerDocument.createRange();
                    S.selectNode(T);
                    S.collapse(true);
                    Q.selectNode(R);
                    Q.collapse(true);
                    var U = S.compareBoundaryPoints(Range.START_TO_END, Q);
                    if (U === 0) {
                        M = true;
                    }
                    return U;
                };
            }
        }
    }
    (function() {
        var R = document.createElement("div"),S = "script" + (new Date).getTime();
        R.innerHTML = "<a name='" + S + "'/>";
        var Q = document.documentElement;
        Q.insertBefore(R, Q.firstChild);
        if (!!document.getElementById(S)) {
            E.find.ID = function(U, V, W) {
                if (typeof V.getElementById !== "undefined" && !W) {
                    var T = V.getElementById(U[1]);
                    return T ? T.id === U[1] || typeof T.getAttributeNode !== "undefined" && T.getAttributeNode("id").nodeValue === U[1] ? [T] : undefined : [];
                }
            };
            E.filter.ID = function(V, T) {
                var U = typeof V.getAttributeNode !== "undefined" && V.getAttributeNode("id");
                return V.nodeType === 1 && U && U.nodeValue === T;
            };
        }
        Q.removeChild(R);
    })();
    (function() {
        var Q = document.createElement("div");
        Q.appendChild(document.createComment(""));
        if (Q.getElementsByTagName("*").length > 0) {
            E.find.TAG = function(R, V) {
                var U = V.getElementsByTagName(R[1]);
                if (R[1] === "*") {
                    var T = [];
                    for (var S = 0; U[S]; S++) {
                        if (U[S].nodeType === 1) {
                            T.push(U[S]);
                        }
                    }
                    U = T;
                }
                return U;
            };
        }
        Q.innerHTML = "<a href='#'></a>";
        if (Q.firstChild && typeof Q.firstChild.getAttribute !== "undefined" && Q.firstChild.getAttribute("href") !== "#") {
            E.attrHandle.href = function(R) {
                return R.getAttribute("href", 2);
            };
        }
    })();
    if (document.querySelectorAll) {
        (function() {
            var Q = B,S = document.createElement("div");
            S.innerHTML = "<p class='TEST'></p>";
            if (S.querySelectorAll && S.querySelectorAll(".TEST").length === 0) {
                return;
            }
            B = function(W, V, T, U) {
                V = V || document;
                if (!U && V.nodeType === 9 && !N(V)) {
                    try {
                        return A(V.querySelectorAll(W), T);
                    } catch(X) {
                    }
                }
                return Q(W, V, T, U);
            };
            for (var R in Q) {
                B[R] = Q[R];
            }
        })();
    }
    if (document.getElementsByClassName && document.documentElement.getElementsByClassName) {
        (function() {
            var Q = document.createElement("div");
            Q.innerHTML = "<div class='test e'></div><div class='test'></div>";
            if (Q.getElementsByClassName("e").length === 0) {
                return;
            }
            Q.lastChild.className = "e";
            if (Q.getElementsByClassName("e").length === 1) {
                return;
            }
            E.order.splice(1, 0, "CLASS");
            E.find.CLASS = function(R, S, T) {
                if (typeof S.getElementsByClassName !== "undefined" && !T) {
                    return S.getElementsByClassName(R[1]);
                }
            };
        })();
    }
    function L(R, W, V, a, X, Z) {
        var Y = R == "previousSibling" && !Z;
        for (var T = 0,S = a.length; T < S; T++) {
            var Q = a[T];
            if (Q) {
                if (Y && Q.nodeType === 1) {
                    Q.sizcache = V;
                    Q.sizset = T;
                }
                Q = Q[R];
                var U = false;
                while (Q) {
                    if (Q.sizcache === V) {
                        U = a[Q.sizset];
                        break;
                    }
                    if (Q.nodeType === 1 && !Z) {
                        Q.sizcache = V;
                        Q.sizset = T;
                    }
                    if (Q.nodeName === W) {
                        U = Q;
                        break;
                    }
                    Q = Q[R];
                }
                a[T] = U;
            }
        }
    }

    function P(R, W, V, a, X, Z) {
        var Y = R == "previousSibling" && !Z;
        for (var T = 0,S = a.length; T < S; T++) {
            var Q = a[T];
            if (Q) {
                if (Y && Q.nodeType === 1) {
                    Q.sizcache = V;
                    Q.sizset = T;
                }
                Q = Q[R];
                var U = false;
                while (Q) {
                    if (Q.sizcache === V) {
                        U = a[Q.sizset];
                        break;
                    }
                    if (Q.nodeType === 1) {
                        if (!Z) {
                            Q.sizcache = V;
                            Q.sizset = T;
                        }
                        if (typeof W !== "string") {
                            if (Q === W) {
                                U = true;
                                break;
                            }
                        } else {
                            if (B.filter(W, [Q]).length > 0) {
                                U = Q;
                                break;
                            }
                        }
                    }
                    Q = Q[R];
                }
                a[T] = U;
            }
        }
    }

    var G = document.compareDocumentPosition ? function(R, Q) {
        return R.compareDocumentPosition(Q) & 16;
    } : function(R, Q) {
        return R !== Q && (R.contains ? R.contains(Q) : true);
    };
    var N = function(Q) {
        return Q.nodeType === 9 && Q.documentElement.nodeName !== "HTML" || !!Q.ownerDocument && Q.ownerDocument.documentElement.nodeName !== "HTML";
    };
    var F = function(Q, X) {
        var T = [],U = "",V,S = X.nodeType ? [X] : X;
        while ((V = E.match.PSEUDO.exec(Q))) {
            U += V[0];
            Q = Q.replace(E.match.PSEUDO, "");
        }
        Q = E.relative[Q] ? Q + "*" : Q;
        for (var W = 0,R = S.length; W < R; W++) {
            B(Q, S[W], T);
        }
        return B.filter(U, T);
    };
    window.Sizzle = B;
})();