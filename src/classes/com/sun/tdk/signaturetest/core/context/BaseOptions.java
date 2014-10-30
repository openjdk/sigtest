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
package com.sun.tdk.signaturetest.core.context;

import java.io.File;

/*
 * This bean represents the core application options
 * associated with SigTest class
 * @author Mikhail Ershov
 */
public class BaseOptions {

    public static final String ID = BaseOptions.class.getName();

    public static final String X_JIMAGE_OPTION = "-xjimage";
    private String xjimage = null;

    public boolean readXJimageOption(String optionName, String[] optionValue) {
        assert optionName != null;
        if ( optionName.equalsIgnoreCase(X_JIMAGE_OPTION) ) {
            assert optionValue != null;
            assert optionValue.length > 0;
            assert !optionValue[0].isEmpty();
            assert new File(optionValue[0]).isFile();
            xjimage = optionValue[0];
            return true;
        }
        return false;
    }


    public String getXjimage() {
        return xjimage;
    }

    public void setXjimage(String xjimage) {
        this.xjimage = xjimage;
    }
}
