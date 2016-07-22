/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.tdk.signaturetest.toyxml;

import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sun.tdk.signaturetest.toyxml.ToyParser.Kind.*;

public class ToyParser {

    private static final Pattern elm = Pattern.compile("<.+?>");
    private static final Pattern single = Pattern.compile("<(.+)/>");
    private static final Pattern close = Pattern.compile("</(.+)>");
    private static final Pattern open = Pattern.compile("<(.+)>");
    private static final Pattern name = Pattern.compile("\\w+\\s");
    private static final Pattern attrs = Pattern.compile("(\\w+=\"[^\"]+?\")+");
    private static final Pattern attrMask = Pattern.compile("(\\w+)=\"(.+)\"");
    private Elem root = null;

    public Elem parse(String in) {
        assert in != null && !in.isEmpty();
        Matcher matcher = elm.matcher(in);
        Stack<Elem> stack = new Stack<>();
        while (matcher.find()) {
            String el = matcher.group();
            Kind k = getLexKind(el);
            if (k == OPEN) {
                openElement(el, stack);
            } else if (k == SINGLE) {
                singleElement(el, stack);
            } else if (k == CLOSE) {
                closeElement(el, stack);
            }
        }

        return root;
    }

    private void closeElement(String def, Stack<Elem> stack) {
        Elem e = stack.peek();
        Matcher m = close.matcher(def);
        if (m.find()) {
            if (m.group(1).equals(e.getNodeName())) {
                stack.pop();
                return;
            }
        }
    }

    private void singleElement(String def, Stack<Elem> stack) {
        addElement(def, stack, single, false);
    }

    private void openElement(String def, Stack<Elem> stack) {
        addElement(def, stack, open, true);
    }

    private void addElement(String def, Stack<Elem> stack, Pattern pat, boolean add) {
        Elem el = new Elem();
        if (root == null) {
            root = el;
        }
        Matcher m = pat.matcher(def);
        if (m.find()) {
            makeElem(el, m.group(1));
        }
        Elem parent = stack.isEmpty() ? null : stack.peek();
        if (parent != null) {
            parent.addChild(el);
        }
        if (add) {
            stack.push(el);
        }
    }

    private void makeElem(Elem el, String txt) {

        Matcher mn = name.matcher(txt);
        if (mn.find()) {
            el.setNodeName(mn.group().trim());
        } else {
            throw new IllegalStateException(txt);
        }

        Matcher ma = attrs.matcher(txt);
        while (ma.find()) {
            String attrSt = ma.group();
            Matcher m = attrMask.matcher(attrSt);
            if (m.find()) {
                el.addAttribute(m.group(1), m.group(2));
            }
        }
    }

    private Kind getLexKind(String el) {
        if (close.matcher(el).find()) {
            return CLOSE;
        }
        if (single.matcher(el).find()) {
            return SINGLE;
        }
        if (open.matcher(el).find()) {
            return OPEN;
        }
        throw new IllegalArgumentException(el);
    }

    enum Kind {SINGLE, OPEN, CLOSE}
}
