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

import com.sun.tdk.signaturetest.model.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * APIVisitor is used to go through api.
 *
 *
 */
class APIVisitor {

    List<ClassDescription> api = new ArrayList<>();
    PackageDescr top = new PackageDescr("");

    public void visit(List<ClassDescription> api) {
        this.api = api;
        collectPackages();
        visit(top);
    }

    private void collectPackages() {
        List<PackageDescr> packages = new ArrayList<>();
        packages.add(top);
        for (ClassDescription cd : api) {
            String pname = cd.getPackageName();
            addPackage(pname, packages).add(cd);
        }
    }

    private PackageDescr addPackage(String pname, List<PackageDescr> packages) {
        PackageDescr newPackage = new PackageDescr(pname);
        PackageDescr parent = null;
        for (PackageDescr pd : packages) {
            if (pd.equals(newPackage)) {
                return pd;
            }
            if (ClassDescription.getPackageName(pname).equals(pd.getQualifiedName())) {
                parent = pd;
            }
        }
        if (parent == null) {
            parent = addPackage(ClassDescription.getPackageName(pname), packages);
        }
        packages.add(newPackage);
        parent.add(newPackage);
        return newPackage;
    }

    protected void visit(PackageDescr x) {
        //System.err.println("TRY ==================" + x);
        for (Object cd : x.getDeclaredClasses()) {
            //System.err.println("CD ===============" + cd);
            visit((ClassDescription) cd);
        }
        for (Object pd : x.getDeclaredPackages()) {
            //System.err.println("PD ===============" + pd);
            visit((PackageDescr) pd);
        }
    }

    protected void visit(ClassDescription x) {

        // XXX
        // hand-made sort of members
        ArrayList<MemberDescription> list = new ArrayList<>();
        for (Iterator i = x.getMembersIterator(); i.hasNext();) {
            boolean isInserted = false;
            MemberDescription md = (MemberDescription) i.next();
            for (int j = 0; j < list.size(); j++) {
                if ((list.get(j).getName() + list.get(j).getArgs())
                        .compareTo(md.getName() + md.getArgs()) > 0) {
                    list.add(j, md);
                    isInserted = true;
                    break;
                }
            }
            if (!isInserted) {
                list.add(md);
            }
        }
        for (MemberDescription md : list) {
            if (md instanceof ConstructorDescr) {
                ConstructorDescr constr = (ConstructorDescr) md;
                visit(constr);
            }
            if (md instanceof MethodDescr) {
                MethodDescr meth = (MethodDescr) md;
                visit(meth);
            }
        }
        for (MemberDescription md : list) {
            if (md instanceof FieldDescr) {
                FieldDescr field = (FieldDescr) md;
                visit(field);
            }

        }
    }

    protected void visit(MemberDescription x) {
    }
}
