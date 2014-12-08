/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.tdk.signaturetest.core.context.Option;
import com.sun.tdk.signaturetest.core.context.Options;

import java.util.EnumSet;

/*
 * This bean represents the api cover command options
 * @author Mikhail Ershov
 */
public class ApicovOptions extends Options {

    private EnumSet<Option> options = EnumSet.of(
            Option.X_JIMAGE,
            Option.API_INCLUDE,
            Option.API_EXCLUDE,
            Option.API,
            Option.TS,
            Option.TS_ICNLUDE,
            Option.TS_ICNLUDEW,
            Option.TS_EXCLUDE,
            Option.API_INCLUDEW,
            Option.EXCLUDE_INTERFACES,
            Option.EXCLUDE_ABSTRACT_CLASSES,
            Option.EXCLUDE_ABSTRACT_METHODS,
            Option.EXCLUDE_FIELDS,
            Option.INCLUDE_CONSTANT_FIELDS,
            Option.MODE,
            Option.DETAIL,
            Option.FORMAT,
            Option.REPORT,
            Option.EXCLUDE_LIST
            );

    @Override
    public EnumSet<Option> getOptions() {
        return options;
    }
}
