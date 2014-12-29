/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.sertest;

import com.sun.tdk.signaturetest.SignatureTest;
import com.sun.tdk.signaturetest.core.context.Option;
import com.sun.tdk.signaturetest.model.ClassDescription;
import com.sun.tdk.signaturetest.model.MemberCollection;
import com.sun.tdk.signaturetest.model.MemberDescription;

import java.io.PrintWriter;
import java.util.Iterator;

/**
 * @author Mikhail Ershov
 */
public class SerTest extends SignatureTest {

    public static void main(String[] args) {
        SerTest t = new SerTest();
        t.run(args, new PrintWriter(System.err, true), null);
        t.exit();
    }

    public SerTest() {
        super();
        new SerUtil().initFormat(this);
    }

    @Override
    protected String getComponentName() {
        return "Serilaization test";
    }

    @Override
    protected boolean allowMissingSuperclasses() {
        return true;
    }

    @Override
    protected void verifyClass(ClassDescription required, ClassDescription found) {
        // allow no serialVersionUID for tested classes
        if (!SerUtil.hasSVUID(found) && SerUtil.hasSVUID(required)) {
            MemberCollection noSVUID = new MemberCollection();
            for (Iterator e = required.getMembersIterator(); e.hasNext();) {
                MemberDescription mr = (MemberDescription) e.next();
                if (!SerUtil.isSVUID(mr, required)) {
                    noSVUID.addMember(mr);
                }
            }
            required.setMembers(noSVUID);
        }
        super.verifyClass(required, found);
    }

    @Override
    public void run(String[] args, PrintWriter log, PrintWriter ref) {
        args = SerUtil.addParam(args, Option.FORMATHUMAN.getKey());
        args = SerUtil.addParam(args, MODE_OPTION, BINARY_MODE);
        args = SerUtil.addParam(args, XNOTIGER_OPTION);
        super.run(args, log, ref);
    }
}
