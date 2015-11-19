package com.sun.tdk.signaturetest.sigfile.f43;

import com.sun.tdk.signaturetest.sigfile.FeaturesHolder;
import com.sun.tdk.signaturetest.sigfile.Reader;
import com.sun.tdk.signaturetest.sigfile.Writer;
import com.sun.tdk.signaturetest.sigfile.f42.F42Format;

public class F43Format extends F42Format {

    static final String MODULE = "module";
    static final String NAME = "name";
    static final String VERSION = "version";
    static final String MAIN_CLASS = "main-class";
    static final String PACKAGE = "package";
    static final String CONCEAL = "conceal";
    static final String EXPORTS = "exports";
    static final String SOURCE = "source";
    static final String TARGET = "target";
    static final String REQUIRES = "requires";
    static final String TRUE = "true";
    static final String PROVIDES = "provides";
    static final String SERVICE = "service";
    static final String PROVIDER = "provider";
    static final String USES = "uses";


    public F43Format() {
        addSupportedFeature(FeaturesHolder.ModuleInfo);
    }

    public Reader getReader() {
        return new F43Reader(this);
    }

    public Writer getWriter() {
        return new F43Writer();
    }

    public String getVersion() {
        return "#Signature file v4.3";
    }


}
