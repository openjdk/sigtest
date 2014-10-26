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

import com.sun.tdk.signaturetest.SigTest;
import com.sun.tdk.signaturetest.Version;
import com.sun.tdk.signaturetest.errors.MessageType;
import com.sun.tdk.signaturetest.model.*;
import com.sun.tdk.signaturetest.plugin.*;
import com.sun.tdk.signaturetest.sigfile.FeaturesHolder;
import com.sun.tdk.signaturetest.sigfile.Format;
import com.sun.tdk.signaturetest.sigfile.Writer;
import com.sun.tdk.signaturetest.sigfile.f42.F42Format;
import com.sun.tdk.signaturetest.sigfile.f42.F42Writer;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Mikhail Ershov
 */
class SerUtil {

    static Logger logger = Logger.getLogger(SerUtil.class.getName());
    private static final String SER_INT = "java.io.Serializable";
    static final String serVerUID = "serialVersionUID";

    void initFormat(SigTest st) {

        final F42Writer w = new F42Writer() {
            @Override
            protected boolean isMemberAccepted(MemberDescription mr, String clsName) {
                return !mr.isSuperClass() && !mr.isSuperInterface();
            }
        };
        final Format f = new F42Format() {
            {
                removeSupportedFeature(FeaturesHolder.ListOfHiders);
            }

            @Override
            public String getVersion() {
                return "#Serialization test v" + Version.Number;
            }

            @Override
            public Writer getWriter() {
                return w;
            }
        };
        w.setFormat(f);
        st.setFormat(f);
    }
    private static Filter serClsFilter = new Filter() {
        public boolean accept(ClassDescription cls) {
            return isSerialized(cls);
        }
    };

    static boolean isSerialized(ClassDescription cls) {

        /*
         * Skip enums -
         * see http://docs.oracle.com/javase/7/docs/platform/serialization/spec/serial-arch.html#6469
         */
        if (cls.hasModifier(Modifier.ENUM)) {
            return false;
        }

        for (Iterator i = cls.getMembersIterator(); i.hasNext();) {
            MemberDescription md = (MemberDescription) i.next();
            if (md.isSuperInterface()) {
                if (SER_INT.equals(((SuperInterface) md).getQualifiedName())) {
                    return true;
                }
            }
        }
        return false;
    }
    private static Transformer emptyTransformer = new Transformer() {
        public ClassDescription transform(ClassDescription cls) throws ClassNotFoundException {
            return cls;
        }
    };
    private static Transformer serializationTransformer = new Transformer() {
        public ClassDescription transform(ClassDescription cls) {
            MemberCollection cleaned = new MemberCollection();
            for (Iterator e = cls.getMembersIterator(); e.hasNext();) {
                MemberDescription mr = (MemberDescription) e.next();
                if (!mr.isField() && !mr.isSuperClass() && !mr.isSuperInterface()) {
                    // not a field
                    continue;
                }
                if (mr.hasModifier(Modifier.TRANSIENT)) {
                    // ignore transient
                    continue;
                }
                if (mr.hasModifier(Modifier.STATIC) && !serVerUID.equals(mr.getName())) {
                    // statics except serialVersionUID
                    continue;
                }
                cleaned.addMember(mr);
            }
            cls.setMembers(cleaned);
            return cls;
        }
    };
    private static Transformer onlyFieldsTransformer = new Transformer() {
        public ClassDescription transform(ClassDescription cls) {
            MemberCollection cleaned = new MemberCollection();
            for (Iterator e = cls.getMembersIterator(); e.hasNext();) {
                MemberDescription mr = (MemberDescription) e.next();
                if (mr.isField()) {
                    cleaned.addMember(mr);
                }
            }
            cls.setMembers(cleaned);
            return cls;
        }
    };

    static {
        PluginAPI.BEFORE_WRITE.setFilter(serClsFilter);
        PluginAPI.AFTER_BUILD_MEMBERS.setTransformer(serializationTransformer);
        PluginAPI.CLASS_CORRECTOR.setTransformer(emptyTransformer);
        // test
        PluginAPI.BEFORE_TEST.setFilter(serClsFilter);
        PluginAPI.BEFORE_TEST.setTransformer(onlyFieldsTransformer);

        Modifier.TRANSIENT.setTracked(true);
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Initialized");
        }
    }
}
