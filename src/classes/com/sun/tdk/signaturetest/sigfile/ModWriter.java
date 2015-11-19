package com.sun.tdk.signaturetest.sigfile;

import com.sun.tdk.signaturetest.model.ModuleDescription;

public interface ModWriter extends BaseWriter {

    void write(ModuleDescription moduleDescription);

}
