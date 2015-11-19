package com.sun.tdk.signaturetest.core;

import com.sun.tdk.signaturetest.model.ModuleDescription;

import java.util.Set;

public interface ModuleDescriptionLoader {

    Set<ModuleDescription> loadBootModules();

}
