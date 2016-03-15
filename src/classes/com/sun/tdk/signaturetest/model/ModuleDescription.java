package com.sun.tdk.signaturetest.model;

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
    private Set<String> conceals;

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
                ", conceals=" + conceals +
                ", version='" + version + '\'' +
                '}';
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

    public Set<String> getConceals() {
        return conceals;
    }

    public void setConceals(Set<String> conceals) {
        this.conceals = conceals;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }


    public static class Provides {
        public Set<String> providers = Collections.EMPTY_SET; //Returns the set of provider names.
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
             * The dependence was implicitly declared in the source of the module declaration.
             */
            MANDATED,

            /**
             * The dependence causes any module which depends on the current module to have
             * an implicitly declared dependence on the module named by the Requires.
             */
            PUBLIC,

            /**
             * The dependence was not explicitly or implicitly declared
             * in the source of the module declaration.
             */
            SYNTHETIC
        }

        public String name;
        public Set<ModuleDescription.Requires.Modifier> modifiers = Collections.EMPTY_SET;

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
        public Set<String> targets = Collections.EMPTY_SET; // optional

        @Override
        public String toString() {
            return "Exports{" +
                    "source='" + source + '\'' +
                    ", targets=" + targets +
                    '}';
        }

    }

}
