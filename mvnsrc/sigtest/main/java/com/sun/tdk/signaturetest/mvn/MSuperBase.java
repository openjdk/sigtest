/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.signaturetest.mvn;

import java.lang.reflect.Field;
import org.apache.maven.plugin.AbstractMojo;

public abstract class MSuperBase extends AbstractMojo {

    /**
     * @parameter default-value="true"
     */
    protected boolean failOnError;
    /**
     * @parameter default-value="false"
     */
    protected boolean negative;

    // ------------------------------------------
    protected void dumpMe() {
        dump(MSuperBase.class, this);
    }

    protected void dump(Class cl, Object o) {
        getLog().debug("  class " + cl.getName());
        try {
            for (Field f : cl.getDeclaredFields()) {
                getLog().debug("    param " + f.getName() + " val=" + getStrinVal(f, o));
            }
        } catch (IllegalArgumentException ex) {
            getLog().error(ex);
        }
    }

    private String getStrinVal(Field f, Object o) {

        String tname = f.getType().toString();
        Object r;
        try {
            if ("boolean".equals(tname)) {
                return Boolean.toString(f.getBoolean(o));
            }
            if ("int".equals(tname)) {
                return Integer.toString(f.getInt(o));
            }
            if ("long".equals(tname)) {
                return Long.toString(f.getLong(o));
            }
            if ("char".equals(tname)) {
                return Character.toString(f.getChar(o));
            }
            if ("short".equals(tname)) {
                return Short.toString(f.getShort(o));
            }
            if ("byte".equals(tname)) {
                return Byte.toString(f.getByte(o));
            }
            if ("double".equals(tname)) {
                return Double.toString(f.getDouble(o));
            }
            if ("float".equals(tname)) {
                return Float.toString(f.getFloat(o));
            }

            if (Object.class.isAssignableFrom(f.getType())) {
                r = f.get(o);
                return r == null ? "null" : r.toString();
            }

        } catch (Exception ex) {
            getLog().error(ex);
        }
        return "?? " + tname;
    }
}
