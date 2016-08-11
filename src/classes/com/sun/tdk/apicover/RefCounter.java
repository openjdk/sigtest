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
package com.sun.tdk.apicover;

import com.sun.tdk.apicover.markup.Adapter;
import com.sun.tdk.signaturetest.core.Erasurator;
import com.sun.tdk.signaturetest.model.ClassDescription;
import com.sun.tdk.signaturetest.model.MemberDescription;
import com.sun.tdk.signaturetest.util.SwissKnife;
import java.util.*;

public class RefCounter {

    private HashMap<String, ClassDescription> api = new HashMap<>();

    private enum MODE {

        REAL, WORST
    }
    private MODE mode = MODE.WORST;
    private Erasurator erasurator = new Erasurator();
    private Map<String, ClassDescription> ts = new HashMap<>();
    Map<String, Integer> results = new HashMap<>();

    public RefCounter() {
        super();

        // set default values
    }

    public void addClass(ClassDescription cd) {
        ArrayList<MemberDescription> modified = new ArrayList<>();
        boolean hasTracked = false;
        Iterator i = cd.getMembersIterator();
        boolean hasMembers = i.hasNext();
        while (i.hasNext()) {
            MemberDescription md = (MemberDescription) i.next();
            if (md.hasModifier(Adapter.coverIgnore)) {
                i.remove();
                continue;
            }
            hasTracked = true;
            MemberDescription md2 = (MemberDescription) md.clone();
            if (mode.equals(MODE.WORST) && !md.getDeclaringClassName().equals(
                    cd.getQualifiedName()) && !md.isFinal()) {
                md2.setDeclaringClass(cd.getQualifiedName());
            }
            i.remove();
            modified.add(md2);
        }
        for (MemberDescription md : modified) {
            cd.add(md);
        }
        if (hasTracked || !hasMembers) {
            api.put(cd.getQualifiedName(), cd);
        }
    }

    public void addTSClass(ClassDescription cd, boolean fromAPI) {
        if (fromAPI) {
            ts.put(cd.getQualifiedName(), erasurator.erasure(cd));
            return;
        }

        String parent = findSuper(cd);
        if (parent.equals(cd.getQualifiedName())
                && cd.getInterfaces().length == 0) {
            return;
        }

        ts.put(cd.getQualifiedName(), erasurator.erasure(cd));
    }

    public void addRef(MemberDescription call) {
        Set<String> calledClasses = Collections.emptySet();
        if (ts.get(call.getDeclaringClassName()) == null) {
            return;
        }
        try {
            calledClasses = findDecl(ts.get(call.getDeclaringClassName()), call);
            //calledClass = findDecl(ts.get(calledClass), call);
        } catch (Exception e) {
            SwissKnife.reportThrowable(e);
        }

        for (String calledCl : calledClasses) {
            ClassDescription apiClass = api.get(calledCl);
            if (apiClass != null) {
                erasurator.parseTypeParameters(apiClass);
                for (Iterator j = apiClass.getMembersIterator(); j.hasNext();) {
                    MemberDescription orig = (MemberDescription) j.next();
                    MemberDescription erased = erasurator.processMember(orig);
                    if (erased.equals(call)) {
                        Integer count = results.get(orig.toString());
                        if (count == null) {
                            count = 0;
                        }
                        count++;
                        results.put(orig.toString(), count);
                    }
                }
            }
        }
    }

    private Set<String> findDecl(ClassDescription tsClass, MemberDescription md) {
        boolean foundSuper = true;
        Set<String> result = new TreeSet<>();

        if (mode == MODE.WORST) {
            result.add(tsClass.getQualifiedName());
        }

        while (foundSuper) {
            // contain Collection or not
            if (tsClass.getMembersIterator().hasNext()) {
                if (tsClass.containsMember(md)) {
                    String theClass = tsClass.findMember(md).getDeclaringClassName();
                    result.add(theClass);
                    return result;
                }
            } else {
                for (MemberDescription decl : tsClass.getDeclaredConstructors()) {
                    if (decl.equals(md)) {
                        result.add(tsClass.getQualifiedName());
                        return result;
                    }
                }
                for (MemberDescription decl : tsClass.getDeclaredFields()) {
                    if (decl.equals(md)) {
                        result.add(tsClass.getQualifiedName());
                        return result;
                    }
                }
                for (MemberDescription decl : tsClass.getDeclaredMethods()) {
                    if (decl.equals(md)) {
                        result.add(tsClass.getQualifiedName());
                        return result;
                    }
                }
                if (mode == MODE.WORST) {
                    result.add(tsClass.getQualifiedName());
                }
            }

            foundSuper = false;
            if (tsClass.getSuperClass() != null
                    && ts.get(tsClass.getSuperClass().getQualifiedName()) != null) {
                tsClass = ts.get(tsClass.getSuperClass().getQualifiedName());
                foundSuper = true;
            }
        }
        return result;
    }

    boolean isCovered(MemberDescription md) {
        return results.get(md.toString()) != null;
    }

    int getCoverCount(MemberDescription md) {
        Integer res = results.get(md.toString());
        if (res == null) {
            return 0;
        } else {
            return res;
        }
    }

    private String findSuper(ClassDescription tsClass) {
        while (tsClass.getSuperClass() != null) {
            if (ts.get(tsClass.getSuperClass().getQualifiedName()) != null) {
                tsClass = ts.get(tsClass.getSuperClass().getQualifiedName());
            } else {
                return tsClass.getQualifiedName();
            }
        }
        return tsClass.getQualifiedName();
    }

    public void setMode(String mode) {
        this.mode = "r".equals(mode) ? MODE.REAL : MODE.WORST;
    }

    private void clearInherited() {
        for (ClassDescription cd : api.values()) {
            for (Iterator i = cd.getMembersIterator(); i.hasNext();) {
                MemberDescription md = (MemberDescription) i.next();
                if (!(md.isConstructor() || md.isField() || md.isMethod())) {
                    i.remove();
                    continue;
                }
                if (mode.equals(MODE.REAL) && !md.getDeclaringClassName().equals(
                        cd.getQualifiedName())) {
                    i.remove();
                    continue;
                }
            }
        }
    }

    public Collection<ClassDescription> getClasses() {
        clearInherited();
        return api.values();
    }
}
