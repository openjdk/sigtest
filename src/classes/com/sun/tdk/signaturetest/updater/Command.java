/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.signaturetest.updater;

import java.io.PrintWriter;

/**
 * @author Mikhail Ershov
 */
abstract class Command {

    String id;
    String comments;
    protected PrintWriter log;

    abstract boolean perform(Updater.SigList sl);

    abstract void validate() throws IllegalArgumentException;

    protected void trace() {
        if (id != null && !id.isEmpty()) {
            log.println("Applying " + id);
        }
    }
}

class RemoveClass extends Command {

    String className;

    RemoveClass(PrintWriter log) {
        this.log = log;
    }

    boolean perform(Updater.SigList sl) {
        trace();
        if (!sl.findClass(className)) {
            log.println("Can't find " + className);
            return false;
        } else {
            sl.removeCurrentClass();
            return true;
        }
    }

    void validate() throws IllegalArgumentException {
        if (className == null || className.isEmpty()) {
            throw new IllegalArgumentException("Class name should be specified");
        }
    }
}

class RemoveMember extends Command {

    String className;
    String memberName;

    RemoveMember(PrintWriter log) {
        this.log = log;
    }

    boolean perform(Updater.SigList sl) {
        trace();
        if (!sl.findClass(className)) {
            log.println("Can't find " + className);
            return false;
        } else {
            sl.removeMember(memberName);
            return true;
        }
    }

    void validate() throws IllegalArgumentException {
        if (className == null || className.isEmpty()) {
            throw new IllegalArgumentException("Class name should be specified");
        }
        if (memberName == null || memberName.isEmpty()) {
            throw new IllegalArgumentException("Member name should be specified");
        }
    }
}

class AddMember extends Command {

    String className;
    String memberName;

    AddMember(PrintWriter log) {
        this.log = log;
    }

    boolean perform(Updater.SigList sl) {
        trace();
        if (!sl.findClass(className)) {
            log.println("Can't find " + className);
            return false;
        } else {
            return sl.addMember(memberName);
        }
    }

    void validate() throws IllegalArgumentException {
        if (className == null || className.isEmpty()) {
            throw new IllegalArgumentException("Class name should be specified");
        }
        if (memberName == null || memberName.isEmpty()) {
            throw new IllegalArgumentException("Member name should be specified");
        }
    }
}

class ChangeMember extends Command {

    String className;
    String memberName;
    String newMemberName;

    ChangeMember(PrintWriter log) {
        this.log = log;
    }

    boolean perform(Updater.SigList sl) {
        trace();
        if (!sl.findClass(className)) {
            log.println("Can't find " + className);
            return false;
        } else {
            sl.changeMember(memberName, newMemberName);
            return true;
        }
    }

    void validate() throws IllegalArgumentException {
        if (className == null || className.isEmpty()) {
            throw new IllegalArgumentException("Class name should be specified");
        }
        if (memberName == null || memberName.isEmpty()) {
            throw new IllegalArgumentException("Member name should be specified");
        }
        if (newMemberName == null || newMemberName.isEmpty()) {
            throw new IllegalArgumentException("New member name should be specified");
        }

    }
}

class AddClass extends Command {

    String className;
    String body;

    AddClass(PrintWriter log) {
        this.log = log;
    }

    boolean perform(Updater.SigList sl) {
        trace();
        if (sl.findClass(className)) {
            log.println("Class " + className + " is already defined");
            return false;
        } else {
            sl.addText(body);
            return true;
        }
    }

    void validate() throws IllegalArgumentException {
        if (className == null || className.isEmpty()) {
            throw new IllegalArgumentException("Class name should be specified");
        }
        if (body == null || body.isEmpty()) {
            throw new IllegalArgumentException("Class definition should be specified");
        }
    }
}

class RemovePackage extends Command {

    String packageName;

    RemovePackage(PrintWriter log) {
        this.log = log;
    }

    boolean perform(Updater.SigList sl) {
        trace();
        while (sl.findPackageMember(packageName)) {
            sl.removeCurrentClass();
        }
        return true;
    }

    void validate() throws IllegalArgumentException {
        if (packageName == null || packageName.isEmpty()) {
            throw new IllegalArgumentException("Package name should be specified");
        }
    }
}
