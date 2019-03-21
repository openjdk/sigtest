/*
 * Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.signaturetest.remote;

import com.sun.tdk.signaturetest.loaders.ReflClassDescrLoader;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * @author Sergey Borodin
 */
public class RemoteLoadManager {

    // prevent creating this utility class
    private RemoteLoadManager() {
    }

    public static void writeClassDescription(String name, OutputStream out) {
        ReflClassDescrLoader loader = new ReflClassDescrLoader();
        Object descr;
        try {
            descr = loader.load(name);
        } catch (ClassNotFoundException | RuntimeException e) {
            descr = e;
        }

        try {
            ObjectOutputStream objOut = new ObjectOutputStream(out);
            objOut.writeObject(descr);
            objOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
