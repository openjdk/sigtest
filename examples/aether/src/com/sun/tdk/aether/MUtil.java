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
package com.sun.tdk.aether;

import com.sun.tdk.signaturetest.SigTest;
import com.sun.tdk.signaturetest.loaders.LoadingHints;
import com.sun.tdk.signaturetest.core.ClassDescriptionLoader;
import com.sun.tdk.signaturetest.model.ClassDescription;
import com.sun.tdk.signaturetest.model.MemberCollection;
import com.sun.tdk.signaturetest.model.MemberDescription;
import com.sun.tdk.signaturetest.model.Modifier;
import com.sun.tdk.signaturetest.plugin.Filter;
import com.sun.tdk.signaturetest.plugin.FormatAdapter;
import com.sun.tdk.signaturetest.plugin.PluginAPI;
import com.sun.tdk.signaturetest.plugin.Transformer;
import com.sun.tdk.signaturetest.sigfile.FeaturesHolder;

import java.util.Iterator;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * @author Mikhail Ershov
 */
class MUtil {

    static Logger logger = Logger.getLogger(MUtil.class.getName());

    void initFormat(SigTest st) {
        FormatAdapter f = new FormatAdapter("#Aether file v4.1");
        f.removeSupportedFeature(FeaturesHolder.ListOfHiders);
        st.setFormat(f);
    }

    private static Filter emptyFilter = new Filter() {
        public boolean accept(ClassDescription cls) {
            return true;
        }
    };

    private static Transformer emptyTransformer = new Transformer() {
        public ClassDescription transform(ClassDescription cls) throws ClassNotFoundException {
            return cls;
        }
    };

    private static Transformer cleanTransformer = new Transformer() {
        public ClassDescription transform(ClassDescription cls) {
            MemberCollection cleaned = new MemberCollection();
            for (Iterator e = cls.getMembersIterator(); e.hasNext();) {
                MemberDescription mr = (MemberDescription) e.next();
                if (!mr.getDeclaringClassName().equals(cls.getQualifiedName())) {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("Member removed: " + mr);
                    }
                    continue;
                }
                cleaned.addMember(mr);
            }
            cls.setMembers(cleaned);
            return cls;
        }
    };

    static void setUpLoader(ClassDescriptionLoader loader) {
        if (loader instanceof LoadingHints) {
            LoadingHints lh = (LoadingHints) loader;
            lh.addLoadingHint(LoadingHints.READ_BRIDGE);
            lh.addLoadingHint(LoadingHints.READ_SYNTETHIC);
            lh.addLoadingHint(LoadingHints.READ_ANY_ANNOTATIONS);
        }
    }

    static {
        PluginAPI.IS_CLASS_ACCESSIBLE.setFilter(emptyFilter);
        PluginAPI.AFTER_BUILD_MEMBERS.setTransformer(cleanTransformer);
        PluginAPI.CLASS_CORRECTOR.setTransformer(emptyTransformer);

        Modifier.ACC_SYNTHETIC.setTracked(true);
        Modifier.BRIDGE.setTracked(true);
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Initialized");
        }
    }

}
