/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Mike Ershov
 */
public class JimageJakeEntry extends ClasspathEntry {

    private List<DirectoryEntry> module_homes = new ArrayList<>();
    private int cur_module_index = -1;
    private Path td;
    private BaseOptions bo = AppContext.getContext().getBean(BaseOptions.class);;

    public JimageJakeEntry(ClasspathEntry previous, String name) throws IOException {
        super(previous);
        init(name);
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

    @Override
    public void init(String jimageName) throws IOException {

        if (!jimageName.endsWith(".jimage")) {
            throw new IOException("Wrong jimage file: " + jimageName);
        }

        // extract to tmp
        td = Files.createTempDirectory("st_");
        String tempd = td.toAbsolutePath().toString();

        assert bo != null;
        String util = bo.getValue(Option.X_JIMAGE);

        if (util == null) {
            throw new IOException("JIMAGE_EXE is not defined");
        }


        try {
            Process process = new ProcessBuilder(util, "extract", "--dir", tempd, jimageName).start();
            int ret = process.waitFor();
            if (ret != 0) {
                throw new IOException("jimage error");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        File baseDir = new File(tempd);

        DirectoryEntry prevEntry = null;
        for (File f : baseDir.listFiles()) {
            if (f.isDirectory()) {
                DirectoryEntry de = new DirectoryEntry(prevEntry, f.getAbsolutePath());
                module_homes.add(de);
                prevEntry = de;
            }
        }
        setFirstModule();

    }

    @Override
    public InputStream findClass(String name) throws IOException, ClassNotFoundException {
        for (DirectoryEntry module : module_homes) {
            try {
                return module.findClass(name);
            } catch (ClassNotFoundException | IOException e) {
                // just skip to the next
            }
        }
        throw new ClassNotFoundException(name);
    }

    @Override
    public boolean hasNext() {
        if (isEmpty()) {
            return false;
        }
        do {
            if (getCurrentModule().hasNext()) {
                return true;
            }
        } while (nextModule() != null);
        return false;
    }

    @Override
    public String nextClassName() {
        if (!hasNext()) {
            return null;
        }
        return getCurrentModule().nextClassName();
    }

    @Override
    public void setListToBegin() {
        setFirstModule();
        getCurrentModule().setListToBegin();
    }

    @Override
    protected boolean contains(String className) {
        for (DirectoryEntry module : module_homes) {
            if (!module.contains(className)) {
                return true;
            }
        }
        // not found? refer back
        return (previousEntry != null && previousEntry.contains(className));
    }

    @Override
    public boolean isEmpty() {
        for (DirectoryEntry module : module_homes) {
            if (!module.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private DirectoryEntry getCurrentModule() {
        return module_homes.get(cur_module_index);
    }

    private DirectoryEntry nextModule() {
        if (cur_module_index == module_homes.size() - 1) {
            return null;
        }
        DirectoryEntry res = module_homes.get(++cur_module_index);
        res.setListToBegin();
        return res;
    }

    private void setFirstModule() {
        if (module_homes.isEmpty()) {
            cur_module_index = -1;
        } else {
            cur_module_index = 0;
        }
    }


    @Override
    public void close() {

        try {
            removeRecursive(td);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
