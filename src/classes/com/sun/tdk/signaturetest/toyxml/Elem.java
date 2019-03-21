/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class Elem {
    private String nodeName;
    private final TreeMap<String, String> attributes = new TreeMap<>();
    private final List<Elem> children = new ArrayList<>();

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        assert nodeName != null && !nodeName.isEmpty();
        this.nodeName = nodeName;
    }

    public String getAttribute(String name) {
        assert name != null && !name.isEmpty();
        return attributes.get(name);
    }

    public List<Elem> getElementsByTagName(String name) {
        assert name != null && !name.isEmpty();
        List<Elem> res = new ArrayList<>();
        getElementsByTagName(this, name, res);
        return res;
    }

    private void getElementsByTagName(Elem e, String name, List<Elem> res) {
        if (e.getNodeName().equals(name)) {
            res.add(e);
        }
        for (Elem ch : e.getChildren()) {
            getElementsByTagName(ch, name, res);
        }
    }

    private List<Elem> getChildren() {
        assert children != null;
        return children;
    }

    public void addAttribute(String name, String val) {
        assert name != null && !name.isEmpty();
        assert val != null && !val.isEmpty();
        attributes.put(name, val);
    }

    public void addChild(Elem el) {
        assert el != null;
        children.add(el);
    }
}
