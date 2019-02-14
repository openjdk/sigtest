/*
 * Copyright (c) 2006, 2015, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.tdk.signaturetest.core.ClassDescriptionLoader;
import com.sun.tdk.signaturetest.core.context.BaseOptions;
import com.sun.tdk.signaturetest.core.context.Option;
import com.sun.tdk.signaturetest.model.ClassDescription;
import com.sun.tdk.signaturetest.model.ExoticCharTools;
import com.sun.tdk.signaturetest.util.I18NResourceBundle;
import com.sun.tdk.signaturetest.util.SwissKnife;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.*;

/**
 * <b>ClasspathImpl</b> provides access to all classes placed inside directories
 * and/or jar-files listed in the classpath, which is given to the constructor
 * for new <b>ClasspathImpl</b> instance. </p>
 * <p>
 * The constructor searches every directory or jar-file listed and keeps
 * corresponding <b>ClasspathEntry</b>
 * element, which can provide access to a bytecode for each class found inside
 * the directory or jar-file. All classes found inside the listed directories
 * and jar-files are virtually enumerated in the same order as they are found.
 * The methods <code>nextClassName()</code> and <code>setListToBegin()</code>
 * provide access to this classes enumeration.
 * <p/>
 * <p>
 * Also, the method <code>findClass(name)</code> provides access to class
 * directly by its qualified name. Note however, that the names class must
 * belong to some directory or zip-file pointed to the <b>ClasspathImpl</b>
 * instance.
 *
 * @author Maxim Sokolnikov
 * @author Roman Makarchuk
 * @author Mikhail Ershov
 *
 * @see com.sun.tdk.signaturetest.classpath.ClasspathEntry
 */
public class ClasspathImpl implements Classpath {

    /**
     * Collector for errors and warnings occurring while <b>ClasspathImpl</b>
     * constructor searches archives of classes.
     */
    private List<String> errors;
    /**
     * Number of ignorable entries found in the path given to
     * <b>ClasspathImpl</b> constructor.
     */
    private int sizeIgnorables;
    /**
     * List of <b>ClasspathEntry</b> instances referring to directories and
     * zip-files found by <b>ClasspathImpl</b> constructor.
     *
     * @see com.sun.tdk.signaturetest.classpath.ClasspathEntry
     * @see com.sun.tdk.signaturetest.classpath.DirectoryEntry
     * @see JarFileEntry
     */
    private List<ClasspathEntry> entries;
    private Iterator<ClasspathEntry> iterator;
    /**
     * <I>Current</I> directory or zip-file entry, containing <I>current</I>
     * class. This field is used to organize transparent enumeration of all
     * classes found by this <b>ClasspathImpl</b> instance.
     *
     * @see #nextClassName()
     * @see #setListToBegin()
     */
    private ClasspathEntry currentEntry;
    /**
     * Path separator used by operating system. Note, that
     * <code>pathSeparator</code> is uniquely determined when JVM starts.
     */
    private static String pathSeparator;
    private static I18NResourceBundle i18n = I18NResourceBundle.getBundleForClass(ClasspathImpl.class);

    private BaseOptions bo = AppContext.getContext().getBean(BaseOptions.class);

    /**
     * Try to determine path separator used by operating system. Path separator
     * is found in <code>java.io.File</code>, or by
     * <code>System.getProperty()</code> invocation.
     *
     * @see #pathSeparator
     *
     * @see java.io.File#pathSeparator
     * @see System#getProperty(String)
     */
    static {
        try {
            // java.io.File is optional class and could be not implemented.
            Class c = Class.forName("java.io.File");
            Field f = c.getField("pathSeparator");
            pathSeparator = (String) f.get(null);
        } catch (Throwable t) {
            try {
                pathSeparator = System.getProperty("path.separator");
            } catch (SecurityException e) {
                SwissKnife.reportThrowable(e);
            }
        }
    }

    /**
     * This constructor finds all classes within the given classpath, and
     * creates a list of <b>ClasspathEntry</b> iterator - one element per each
     * directory or zip-file found. Classes found inside the listed directories
     * and zip files become available through the created <b>ClasspathImpl</b>
     * instance.
     *
     * @param classPath Path string listing directories and/or zip files.
     * @throws SecurityException The <code>classPath</code> string has invalid
     * format.
     * @see #findClass(String)
     * @see #nextClassName()
     * @see #setListToBegin()
     * @see #createPathEntry(ClasspathEntry, String)
     */
    public ClasspathImpl(String classPath) {
        init(classPath);
    }

    @Override
    public void init(String classPath) {
        entries = new ArrayList<ClasspathEntry>();
        errors = new ArrayList<String>();
        Set<String> unique = new HashSet<String>();
        String path = (classPath == null) ? "" : classPath;
        if (!path.equals("") && (pathSeparator == null)) {
            throw new SecurityException(i18n.getString("ClasspathImpl.error.notdefinepathsep"));
        }

        ClasspathEntry previosEntry = null;

        //creates Hashtable with ZipFiles and directories from path.
        while (path != null && path.length() > 0) {
            String s;
            int index = path.indexOf(pathSeparator);
            if (index < 0) {
                s = path;
                path = null;
            } else {
                s = path.substring(0, index);
                path = path.substring(index + pathSeparator.length());
            }

            if (unique.contains(s)) {
                errors.add(i18n.getString("ClasspathImpl.error.duplicate_entry_found", s));
                continue;
            }

            unique.add(s);

            ClasspathEntry entry = createPathEntry(previosEntry, s);
            if (entry != null && !entry.isEmpty()) {
                entries.add(entry);
                previosEntry = entry;
            }
        }

        setListToBegin();
    }

    @Override
    public void close() {
        if (entries != null) {
            for (ClasspathEntry ce : entries) {
                ce.close();
            }
            entries = null;
            iterator = null;
            currentEntry = null;
        }
    }

    @Override
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * Report about all errors occurred while construction of ClasspathImpl.
     *
     * @param out Where to println error messages.
     */
    @Override
    public void printErrors(PrintWriter out) {
        if (out != null) {
            for (String err : errors) {
                out.println(err);
            }
        }
    }

    /**
     * Return number of significand errors occurred when <b>ClasspathImpl</b>
     * constructor was being working. Ignorable path entries are not taken into
     * account here.
     *
     * @see #createPathEntry(ClasspathEntry, String)
     */
    public int getNumErrors() {
        return errors.size() - sizeIgnorables;
    }

    /**
     * Reset list of directories and/or zip-files found by <b>ClasspathImpl</b>.
     * This also resets transparent enumeration of classes found inside those
     * directories and zip-files, which are available with the methods
     * <code>nextClassName()</code>, <code>getCurrentClass()</code>, or
     * <code>findClass(name)</code>.
     *
     * @see #nextClassName()
     * @see #findClass(String)
     */
    @Override
    public void setListToBegin() {
        iterator = entries.iterator();
        currentEntry = null;
        if (iterator.hasNext()) {
            currentEntry = iterator.next();
        }
    }

    @Override
    public boolean hasNext() {
        if (currentEntry == null) {
            return false;
        }

        if (currentEntry.hasNext()) {
            return true;
        }

        currentEntry = null;
        if (iterator.hasNext()) {
            currentEntry = iterator.next();
            return hasNext();
        }

        return false;
    }

    /**
     * Search next class in the enumeration of classes found inside directories
     * and jar-files pointed to <code>this</code> <b>ClasspathImpl</b> instance.
     * You may invoke <code>setListToBegin()</code> method to restore classes
     * enumeration to its starting point.
     *
     * @return Class qualified name
     * @see #setListToBegin()
     * @see #findClass(String)
     */
    @Override
    public String nextClassName() {
        return currentEntry.nextClassName();
    }

    /**
     * Returns <b>FileInputStream</b> instance providing bytecode for the
     * required class. The class must be found by the given qualified name
     * inside some of <b>ClasspathEntry</b> iterator listed by <code>this</code>
     * <b>ClasspathImpl</b> instance.
     *
     * @param name Qualified name of the class requested.
     * @throws ClassNotFoundException Not found in any <b>ClasspathEntry</b> in
     * <code>this</code>
     * <b>ClasspathImpl</b> instance.
     * @see java.io.FileInputStream
     */
    @Override
    public InputStream findClass(String name) throws IOException, ClassNotFoundException {
        name = ExoticCharTools.decodeExotic(name);

        // generic names are no allowed here
        assert (name.indexOf('<') == -1 && name.indexOf('>') == -1);

        for (ClasspathEntry entry : entries) {
            try {
                return (entry).findClass(name);
            } catch (ClassNotFoundException exc) {
                // just skip this entry
            }
        }
        throw new ClassNotFoundException(name);
    }

    @Override
    public ClassDescription findClassDescription(String qualifiedClassName) throws ClassNotFoundException {
        for (ClasspathEntry ce : entries) {
            try {
                if (ce instanceof ClassDescriptionLoader) {
                    return ((ClassDescriptionLoader)ce).load(qualifiedClassName);
                }
            } catch (ClassNotFoundException cnfe) {

            }
        }
        throw new ClassNotFoundException(qualifiedClassName);
    }

    @Override
    public KIND_CLASS_DATA isClassPresent(String qualifiedClassName) {
        try {
            findClassDescription(qualifiedClassName);
            return KIND_CLASS_DATA.DESCRIPTION;
        } catch (Exception cnfe) {
            InputStream is = null;
            try {
                is = findClass(qualifiedClassName);
                return KIND_CLASS_DATA.BYTE_CODE;
            } catch (Exception e) {

            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        // Ok
                    }
                }
            }
        }
        return KIND_CLASS_DATA.NOT_FOUND;
    }

    /**
     * Check if the given name is directory or zip-file name, and create either
     * new <b>DirectoryEntry</b> or new <b>JarFileEntry</b> instance
     * correspondingly.
     *
     * @param name Qualified name of some directory or zip file or jimage.
     * @return New <b>ClasspathEntry</b> instance corresponding to the given
     * <code>name</code>.
     */
    protected ClasspathEntry createPathEntry(ClasspathEntry previosEntry, String name) {
        try {
            if (new File(name).isDirectory()) {
                return new DirectoryEntry(previosEntry, name);
            } else if ( new File(name).getName().equals("modules") || name.endsWith(".jimage")) {
                return new JimageJakeEntry(previosEntry, name);
            } else if (isSigFile(name)) {
                return new SigFileEntry(previosEntry, name);
            } else {
                return new JarFileEntry(previosEntry, name);
            }
        } catch (IOException ex) {
            // TODO - log it if -debug is specified in the context
            // ex.printStackTrace();
            return null;
        }
    }

    private boolean isSigFile(String fName) {
        // first version, later ti analise the content
        return fName.toLowerCase().endsWith(".sig");
    }
}
