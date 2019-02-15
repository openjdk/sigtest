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
import com.sun.tdk.signaturetest.model.*;

import java.io.PrintWriter;
import java.util.*;

/**
 * Filters invocations based on -FilterMap rules
 *
 * @author Mikhail Ershov
 */
public class CallFilter {

    private final String PKG_PATTERN = "API_PACKAGE";
    private final String CLS_PATTERN = "API_CLASS";
    private ApicovOptions ao = AppContext.getContext().getBean(ApicovOptions.class);
    private List<String> mapFilters = null;
    private List<String> sigFilters = null;
    private PrintWriter pw;

    public boolean init(PrintWriter printWriter) {

        if (ao.getValue(Option.FILTERMAP) != null) {
            mapFilters = ao.getValues(Option.FILTERMAP);
        }
        if (ao.getValue(Option.FILTERSIG) != null) {
            sigFilters = ao.getValues(Option.FILTERSIG);
        }

        pw = printWriter;

        return true;
    }


    private boolean hasNoFilter() {
        return (mapFilters == null || mapFilters.isEmpty()) &&
                (sigFilters == null || sigFilters.isEmpty());
    }

    public List<MemberDescription> filterCalls(List<MemberDescription> foundCalls, String testClassName) {
        if (hasNoFilter()) {
            return foundCalls;
        }

        if (mapFilters != null && !mapFilters.isEmpty()) {
            List<MemberDescription> filteredCalls = new ArrayList<>();
            for (String filter : mapFilters) {
                Iterator<MemberDescription> mi = foundCalls.iterator();
                while (mi.hasNext()) {
                    MemberDescription md = mi.next();
                    String apiClass = md.getDeclaringClassName();
                    int dollar = apiClass.indexOf('$');
                    if (dollar >= 0) {
                        apiClass = apiClass.substring(0, dollar);
                    }
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

        if (sigFilters != null && !sigFilters.isEmpty()) {
            TreeSet<String> filteredSigs = new TreeSet<>();
            for (String filter : sigFilters) {
                if (filter == null || filter.isEmpty()) {
                    continue;
                }
                for (MemberDescription md : foundCalls) {
                    String sig = getMemberSignature(md);
                    if (sig.matches(filter)) {
                        filteredSigs.add(sig);
                    }
                }
            }
            if (!filteredSigs.isEmpty()) {
                pw.println(testClassName);
                for (String sig : filteredSigs) {
                    pw.println("    " + sig);
                }
            }
            return Collections.emptyList();
        }

        return null;

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

    private String getMemberSignature(MemberDescription md) {
        if (md instanceof ConstructorDescr) {
            ConstructorDescr cd = (ConstructorDescr) md;
            return cd.getSignature();
        } else if (md instanceof MethodDescr) {
            MethodDescr mthd = (MethodDescr) md;
            return mthd.getDeclaringClassName() + "." + mthd.getSignature();
        } else if (md instanceof FieldDescr) {
            FieldDescr fd = (FieldDescr) md;
            return fd.getQualifiedName();
        }
        return "???";
    }

}
