/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.exclude;

import com.sun.tdk.signaturetest.core.Exclude;
import com.sun.tdk.signaturetest.core.ExcludeException;
import com.sun.tdk.signaturetest.model.ClassDescription;
import com.sun.tdk.signaturetest.model.MemberDescription;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * This is a sample realization of Exclude extension.
 *
 * @author Leonid Mesnik
 */
public class ExcludeList implements Exclude {

    private List<Pattern> excludeList;

    public ExcludeList() {
        excludeList = new ArrayList<>();
    }


    /* (non-Javadoc)
     * @see com.sun.tdk.exclude.Exclude#addSignature(java.lang.String)
     */
    public void addSignature(String name) {
        // escape .(){}{}%$
        String regpack1 = name.replaceAll("(\\.|\\(|\\)|\\{|\\}|\\[|\\]|\\%|\\$)", "\\\\$1");
        String regpack = regpack1.replaceAll("\\\\\\\\", "");
        try {
            excludeList.add(Pattern.compile(regpack));
        } catch (PatternSyntaxException e) {
            System.err.println("Error in -ExcludeSig: " + e.getMessage());
        }
    }

    /* (non-Javadoc)
     * @see com.sun.tdk.exclude.Exclude#excluded(java.lang.String)
     */
    public void check(ClassDescription testedClass, MemberDescription name) throws ExcludeException {
        String signature = name.getQualifiedName().replaceAll("<[^<>]+>", "");
        if (name.isMethod() || name.isConstructor()) {
            signature += '(';
            signature += name.getArgs().replaceAll("<[^<>]+>", "");
            signature += ')';
        }
        for (Object o : excludeList) {
            Pattern p = (Pattern) o;
            Matcher m = p.matcher(signature);
            if (m.matches()) {
                throw new ExcludeException(p.pattern());
            }
        }
    }

    public String[] parseParameters(String[] args) {
        Set<String> rest = new HashSet<>();
        List<String> parameters = new ArrayList<>(Arrays.asList(args));
        Iterator<String> i = parameters.iterator();
        while (i.hasNext()) {
            String parameter = i.next();
            if (parameter.equalsIgnoreCase("-excludesig")) {
                rest.add(parameter);
                parameter = i.next();
                addSignature(parameter);
                rest.add(parameter);
            }
        }
        parameters.removeAll(rest);
        return parameters.toArray(new String[0]);
    }

    public String report() {
        return "finished";
    }
}
