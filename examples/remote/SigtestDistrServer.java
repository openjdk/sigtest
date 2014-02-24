/*
 * $Id$
 *
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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
package javasoft.sqe.tests.api.signaturetest.distributed;

import com.sun.tck.midp.lib.DistributedTest;

import java.io.PrintWriter;

import com.sun.javatest.Status;
import com.sun.tdk.signaturetest.model.ClassDescription;
import com.sun.tdk.signaturetest.SignatureTest;
import com.sun.tdk.signaturetest.core.ClassDescriptionLoader;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;

/**
 * @author Sergey Borodin
 * <p/>
 * <p/>
 * Server part of distributed test. Invokes SigTest and set custom
 * classDescriptionLoader for it, which constructs class descriptions on client
 * side of distributed test
 */
public class SigtestDistrServer extends DistributedTest {

    public SigtestDistrServer() {
        //IMPORTANT: you should invoke super constructor with unique name for each
        //test, cause ME-Framework use this as test id in message exchange
        super("SigtestDistrServer");
        loader = new DistrClassDescrLoader();
    }

    public Status run(String[] args, PrintWriter log, PrintWriter ref) {
        t = new SignatureTest();
        t.setClassDescrLoader(loader);

        String[] sigtestArgs = new String[args.length - 2];
        System.arraycopy(args, 2, sigtestArgs, 0, args.length - 2);

        String[] testArgs = new String[2];
        System.arraycopy(args, 0, testArgs, 0, 2);

        sigtestThread = new SigtestExecThread(t, sigtestArgs, log, ref);
        sigtestThread.start();

        setMsgStatus(super.run(testArgs, log, ref));

        Status status = Status.parse(t.toString().substring(7));

        return (status.getType() > sigtestExecStatus.getType())
                ? status : sigtestExecStatus;
    }

    /**
     * Invokes by message handling thread each time as new message comes from
     * client
     *
     * @see
     */
    protected void handleMessage(String from, String[] args, byte[] data,
            boolean invokeLegacyHandler) {
        try {
            if (args[0].startsWith("requestForClassName")
                    || args[0].startsWith("classLoaded")) {
                if (args[0].startsWith("classLoaded")) {
                    response.setNewResponse(args[1], data);
                }

                request.waitForRequest();

                if (!request.isFinishREquest()) {
                    send(from, new String[]{"className",
                        request.getRequestedClassName()});
                    request.clean();
                } else {
                    send(from, new String[]{"finish"});
                    terminate();
                }
            }
        } catch (IOException e) {
            setMsgStatus(Status.failed(e.getMessage()));
        }
    }

    private byte[] classDescr = null;
    private String newRequest;
    private boolean sigtestFinished = false;
    private String classToLoad;

    private ClassDescriptionLoader loader;
    private SignatureTest t;
    private SigtestExecThread sigtestThread;

    private static final Class[] handleXXXArgs = {String.class, String[].class, byte[].class};

    class DistrClassDescrLoader implements ClassDescriptionLoader {

        /**
         * SigTest use this re-defined classDescriptionLoader to load class
         * descriptions on client side of test Custom loader set for sigtest
         * using SigTest.setClassDescrLoader() method
         */
        public DistrClassDescrLoader() {
            //This uses to save time, cause SigTest tries to load same class descriptions
            //more then one time
            classDescriptions = new HashMap();
        }

        /**
         * Invokes by SigTest. Sets new request for message exchange mechanism;
         * wait while response for new request comes from client and returns it
         * as a result
         */
        public ClassDescription load(String name) throws ClassNotFoundException {

            System.out.println("Try to load " + name);

            if (classDescriptions.containsKey(name)) {
                return (ClassDescription) classDescriptions.get(name);
            } else {
                request.setNewRequest(name);

                response.waitForResponse();

                if (!response.isNewRequest()) {
                    ByteArrayInputStream bInp = new ByteArrayInputStream(response.getData());
                    try {
                        ObjectInputStream oInp = new ObjectInputStream(bInp);
                        Object obj = oInp.readObject();

                        oInp.close();
                        classDescr = null;

                        ClassDescription cd;

                        if (obj instanceof ClassDescription) {
                            System.out.println("ClassDescription");
                            cd = (ClassDescription) obj;
                            if (!cd.getQualifiedName().equals(name)) {
                                throw new Error("Server received wrong description:"
                                        + "required:" + name + ";" + "received:" + cd.getQualifiedName());
                            }

                            classDescriptions.put(name, cd);

                            return cd;
                        } else if (obj instanceof ClassNotFoundException) {
                            System.out.println("ClassNotFoundException");
                            throw (ClassNotFoundException) obj;
                        } else if (obj instanceof RuntimeException) {
                            System.out.println("RuntimeException");
                            throw (RuntimeException) obj;
                        }

                    } catch (IOException e) {
                        System.out.println(e.toString());
                    }
                }

                response.clean();

                return null;
            }
        }

        private HashMap classDescriptions;

    }

    class SigtestExecThread extends Thread {

        private SignatureTest sigTest;
        private String[] args;
        private Status status;
        private PrintWriter log;
        private PrintWriter ref;

        public SigtestExecThread(SignatureTest sigTest, String[] args,
                PrintWriter log, PrintWriter ref) {
            this.sigTest = sigTest;
            this.args = args;
            this.log = log;
            this.ref = ref;
        }

        public void run() {
            try {
                sigTest.run(args, log, ref);
            } catch (RuntimeException e) {
                System.out.println("SIGTEST RUNTIME EXCEPTION:");
                System.out.println(e.toString());
                e.printStackTrace();

                sigtestExecStatus = Status.failed(e.getMessage());
                request.setNewRequest(null);
            }

            request.setNewRequest(null);
        }

        public Status getExecStatus() {
            return Status.parse(sigTest.toString().substring(7));
        }
    }

    private Request request = new Request();
    private Response response = new Response();

    private Status sigtestExecStatus = Status.passed("OK");

    private class Request {

        public Request() {
            emptyRequest = true;
        }

        public synchronized void setNewRequest(String className) {
            emptyRequest = false;
            this.className = className;

            if (className != null) {
                finishRequest = false;
            } else {
                finishRequest = true;
            }

            notifyAll();
            System.out.println("request notify all");
        }

        public String getRequestedClassName() {
            return className;
        }

        public void clean() {
            emptyRequest = true;
        }

        public synchronized void waitForRequest() {
            if (emptyRequest) {
                try {
                    System.out.println("request wait");
                    wait();
                } catch (InterruptedException e) {
                }
            }
        }

        public boolean isFinishREquest() {
            return finishRequest;
        }

        private boolean finishRequest;
        private String className;
        private boolean emptyRequest;
    }

    private class Response {

        public Response() {
        }

        public synchronized void setNewResponse(String className, byte[] data) {
            this.className = className;
            this.data = data;

            if (data == null) {
                isNewRequest = true;
            } else {
                isNewRequest = false;
            }

            System.out.println("response notify all");
            notifyAll();
        }

        public byte[] getData() {
            return data;
        }

        public boolean isNewRequest() {
            return isNewRequest;
        }

        public void clean() {
            System.out.println("response clean");
            data = null;
            isNewRequest = false;
        }

        public synchronized void waitForResponse() {
            try {
                System.out.println("response wait");
                wait();
            } catch (InterruptedException e) {
            }
        }

        private byte[] data;
        private String className;
        private boolean isNewRequest;
    }
}
