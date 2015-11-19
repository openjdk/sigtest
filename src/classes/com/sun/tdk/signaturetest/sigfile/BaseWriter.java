package com.sun.tdk.signaturetest.sigfile;

import java.io.PrintWriter;

public interface BaseWriter {

    void init(PrintWriter out);

    void setApiVersion(String apiVersion);

    void addFeature(Format.Feature feature);

    void writeHeader();

    void close();

}
