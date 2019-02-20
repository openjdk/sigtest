/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tdk.signaturetest.model;

import com.sun.tdk.signaturetest.core.context.ModFeatures;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Reflection of java.lang.module.ModuleDescriptor
 */
public class ModuleDescription {

    private String name;

    // optional
    private String mainClass = "";
    private Set<String> packages;
    private Set<ModuleDescription.Exports> exports;
    private Map<String, Provides> provides;
    private Set<ModuleDescription.Requires> requires;
    private Set<String> uses;
    private Set<ModFeatures> features;

    // optional
    private String version = "";

    public ModuleDescription() {
    }

    @Override
    public String toString() {
        return "ModuleDescription{" +
                "name='" + name + '\'' +
                ", mainClass='" + mainClass + '\'' +
                ", packages=" + packages +
                ", exports=" + exports +
                ", provides=" + provides +
                ", requires=" + requires +
                ", uses=" + uses +
                ", version='" + version + '\'' +
                '}';
    }

    public Set<ModFeatures> getFeatures() {
        return features;
    }

    public void setFeatures(Set<ModFeatures> features) {
        this.features = features;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public Set<String> getPackages() {
        return packages;
    }

    public void setPackages(Set<String> packages) {
        this.packages = packages;
    }

    public Set<Exports> getExports() {
        return exports;
    }

    public void setExports(Set<Exports> exports) {
        this.exports = exports;
    }

    public Map<String, Provides> getProvides() {
        return provides;
    }

    public void setProvides(Map<String, Provides> provides) {
        this.provides = provides;
    }

    public Set<Requires> getRequires() {
        return requires;
    }

    public void setRequires(Set<Requires> requires) {
        this.requires = requires;
    }

    public Set<String> getUses() {
        return uses;
    }

    public void setUses(Set<String> uses) {
        this.uses = uses;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }


    public static class Provides {
        public Set<String> providers = Collections.emptySet(); //Returns the set of provider names.
        public String service;

        @Override
        public String toString() {
            return "Provides{" +
                    "providers=" + providers +
                    ", service='" + service + '\'' +
                    '}';
        }
    }

    public static class Requires {

        public enum Modifier {


            /**
             * The dependence causes any module which depends on the <i>current
             * module</i> to have an implicitly declared dependence on the module
             * named by the {@code Requires}.
             */
            TRANSITIVE,

            /**
             * The dependence is mandatory in the static phase, during compilation,
             * but is optional in the dynamic phase, during execution.
             */
            STATIC,

            /**
             * The dependence was not explicitly or implicitly declared in the
             * source of the module declaration.
             */
            SYNTHETIC,

            /**
             * The dependence was implicitly declared in the source of the module
             * declaration.
             */
            MANDATED

        }

        public String name;
        public Set<ModuleDescription.Requires.Modifier> modifiers = Collections.emptySet();

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return "Requires{" +
                    "name='" + name + '\'' +
                    ", modifiers=" + modifiers +
                    '}';
        }
    }

    public static class Exports {
        public String source;
        public Set<String> targets = Collections.emptySet(); // optional

        @Override
        public String toString() {
            return "Exports{" +
                    "source='" + source + '\'' +
                    ", targets=" + targets +
                    '}';
        }

        public boolean isPublic() {
            return targets.isEmpty();
        }

    }

}
