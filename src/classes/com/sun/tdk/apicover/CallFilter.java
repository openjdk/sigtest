/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tdk.apicover;

import com.sun.tdk.signaturetest.core.AppContext;
import com.sun.tdk.signaturetest.core.context.Option;
import com.sun.tdk.signaturetest.model.ClassDescription;
import com.sun.tdk.signaturetest.model.MemberDescription;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Filters invocations based on -FilterMap rules
 *
 * @author Mikhail Ershov
 */
public class CallFilter {

    private final String PKG_PATTERN = "API_PACKAGE";
    private final String CLS_PATTERN = "API_CLASS";
    private ApicovOptions ao = AppContext.getContext().getBean(ApicovOptions.class);
    private List<String> filters = null;

    public boolean init() {

        if (ao.isSet(Option.FILTERMAP)) {
            filters = ao.getValues(Option.FILTERMAP);
        }

        return true;
    }

    public List<MemberDescription> filterCalls(List<MemberDescription> foundCalls, String testClassName) {
        if (filters == null || filters.isEmpty()) {
            return foundCalls;
        }

        List<MemberDescription> filteredCalls = new ArrayList<>();

        for (String filter : filters) {
            Iterator<MemberDescription> mi = foundCalls.iterator();
            while (mi.hasNext()) {
                MemberDescription md = mi.next();
                String apiClass = md.getDeclaringClassName();
                String pkg = ClassDescription.getPackageName(apiClass);
                String cls = ClassDescription.getClassShortName(apiClass);

                if (accept(filter, pkg, cls, testClassName)) {
                    filteredCalls.add(md);
                    mi.remove();
                }
            }
        }

        return filteredCalls;
    }

    private boolean accept(String pattern, String pkg, String cls, String testClassName) {
        pattern = simpleReplaceAll(pattern, PKG_PATTERN, pkg);
        pattern = simpleReplaceAll(pattern, CLS_PATTERN, cls);
        return testClassName.startsWith(pattern);
    }

    private String simpleReplaceAll(String string, String search, String replace) {
        int p = 0;
        while ((p = string.indexOf(search, p)) >= 0) {
            String s1 = string.substring(0, p);
            int newP = p + search.length();
            String s2 = string.substring(newP);
            string = s1 + replace + s2;
            p = ++newP;
        }
        return string;
    }

}
