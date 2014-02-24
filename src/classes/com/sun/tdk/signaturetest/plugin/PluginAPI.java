/*
 * $Id$
 *
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.signaturetest.plugin;

import com.sun.tdk.signaturetest.core.ClassDescriptionLoader;
import com.sun.tdk.signaturetest.sigfile.Format;

/**
 * @author Roman Makarchuk
 */
public interface PluginAPI {

    // Injections that have default implementation
    static final InjectionPoint IS_CLASS_ACCESSIBLE = new InjectionPoint(true, false);
    static final InjectionPoint AFTER_BUILD_MEMBERS = new InjectionPoint(false, true);
    static final InjectionPoint ON_CLASS_LOAD = new InjectionPoint(false, true);
    static final InjectionPoint BEFORE_WRITE = new InjectionPoint(true, true);
    // Injections only for transformers
    static final InjectionPoint BEFORE_TEST = new InjectionPoint(true, true);
    static final InjectionPoint AFTER_CLASS_CORRECTOR = new InjectionPoint(false, true);
    static final InjectionPoint CLASS_CORRECTOR = new InjectionPoint(false, true);
    static final InjectionPoint BEFORE_MESSAGE_SORT = new InjectionPoint(true);

    Context getContext();

    Filter getFilter(InjectionPoint injectionPoint);

    void setFilter(InjectionPoint injectionPoint, Filter filter);

    Transformer getTransformer(InjectionPoint injectionPoint);

    void setTransformer(InjectionPoint injectionPoint, Transformer transformer);

    // add new format to read/write signature files
    void addFormat(Format format, boolean useByDefault);

    // set format to read/write signature files
    void setFormat(Format format);

    // set own classDesription loader
    void setClassDescrLoader(ClassDescriptionLoader loader);

    void setMessageTransformer(InjectionPoint where, MessageTransformer messageTransformer);

    class InjectionPoint {

        boolean filterAccepted;  // transform otherwise
        boolean transformAccepted;
        boolean messageAccepted;
        Filter filter = null;
        Transformer transformer = null;
        MessageTransformer messageTransformer = null;

        private InjectionPoint(boolean filterAccepted, boolean transformAccepted) {
            this.filterAccepted = filterAccepted;
            this.transformAccepted = transformAccepted;
        }

        private InjectionPoint(boolean messageAccepted) {
            this.messageAccepted = messageAccepted;
        }

        public Filter getFilter() {
            return filter;
        }

        public void setFilter(Filter filter) {
            if (!filterAccepted) {
                throw new UnsupportedOperationException();
            }
            this.filter = filter;
        }

        public Transformer getTransformer() {
            return transformer;
        }

        public void setTransformer(Transformer transformer) {
            if (!transformAccepted) {
                throw new UnsupportedOperationException();
            }
            this.transformer = transformer;
        }

        public void setMessageTransformer(MessageTransformer t) {
            if (!messageAccepted) {
                throw new UnsupportedOperationException();
            }
            messageTransformer = t;
        }

        public MessageTransformer getMessageTransformer() {
            return messageTransformer;
        }
    }
}
