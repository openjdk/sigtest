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

package com.sun.tdk.signaturetest.classpath;

import com.sun.tdk.signaturetest.core.AppContext;
import com.sun.tdk.signaturetest.core.context.BaseOptions;
import com.sun.tdk.signaturetest.core.context.Option;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * @author Mike Ershov
 */
public class JimageFileEntry extends DirectoryEntry {

    private Path td;
    private BaseOptions bo;

    public JimageFileEntry(ClasspathEntry previous, String name) throws IOException {
        super(previous);
        bo = (BaseOptions) AppContext.getContext().getBean(BaseOptions.ID);
        assert bo != null;
        init(name);
    }

    @Override
    public void init(String jimageName) throws IOException {

        if (!jimageName.endsWith(".jimage")) {
            throw new IOException("Wrong jimage file: " + jimageName);
        }

        // extract to tmp
        td = Files.createTempDirectory("st_");
        String tempd = td.toAbsolutePath().toString();

        assert bo != null;
        String util = bo.getValue(Option.X_JIMAGE_OPTION);

        if (util == null) {
            throw new IOException("JIMAGE_EXE is not defined");
        }

        Process process = new ProcessBuilder(util, "expand", "--dir", tempd, jimageName).start();

        try {
            int ret = process.waitFor();
            if (ret != 0) {
                throw new IOException("jimage error");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        super.init(tempd);

    }


    @Override
    public void close() {

        try {
            removeRecursive(td);
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.close();

    }

    public static void removeRecursive(Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc == null) {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                } else {
                    throw exc;
                }
            }
        });
    }


}
