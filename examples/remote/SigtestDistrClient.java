/*
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

import com.sun.tck.cldc.lib.Status;
import com.sun.tck.j2me.services.messagingService.J2MEDistributedTest;
import com.sun.tdk.signaturetest.remote.RemoteLoadManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

/**
 * @author Sergey Borodin
 */
public class SigtestDistrClient extends J2MEDistributedTest {

    public SigtestDistrClient() {
        //IMPORTANT: you should invoke super constructor with unique name for each
        //test, cause ME-Framework use this as test id in message exchange
        super("SigtestDistrClient");
    }

    /**
     * Entry point for distributed test execution
     */
    protected void runTestCases() {
        System.out.println("runTestCases Started");
        if (isSelected("sigtetstDistributed")) {
            addStatus(startMsgLoop());
        }
    }

    private Status startMsgLoop() {
        System.out.println("Start Msg Loop");

        Status s = Status.passed("OK");

        //This initiates "dialog" with server side
        try {
            send("SigtestDistrServer", new String[]{"requestForClassName"});
            System.out.println("Request Sent");

        } catch (Exception e) {
            e.printStackTrace();
            return Status.failed("failed: " + e);
        }

        //This suspends current thread until final message from server
        try {
            monitor.waitUntilFinished();
        } catch (InterruptedException e) {
            s = Status.failed("FAILED :" + e.toString());
        } finally {
            terminate();
        }

        return s;
    }

    /**
     * Invokes by message handling thread each time as new message comes from
     * server
     */
    public void handleMessage(String from, String[] args) {
        handleMessage(from, args, null);
    }

    public void handleMessage(final String from, final String[] args, byte[] bytes) {
        int length = args.length;
        if ((2 < length) && args[2] != null && args[2].startsWith("className")) {
            try {
                try {
                    AccessController.doPrivileged(new PrivilegedExceptionAction() {
                        public Object run() {
                            send(from, new String[]{"classLoaded", args[3]}, loadClassDescription(args[3]));
                            return null;
                        }
                    });
                } catch (PrivilegedActionException ex) {
                    throw ex.getException();
                }
            } catch (IOException e) {
            }
        } else if (2 >= length) {
            if (args[0] != null && args[0].startsWith("finish")) {
                System.out.println("Now notify");
                synchronized (monitor) {
                    monitor.notifyAll();
                }
            }
        }
    }

    /**
     * Loads class description using SigTest's ReflClassDescriptionLoader
     * remote.jar containing this loader should be in classpath when starting
     * agent
     */
    private byte[] loadClassDescription(String className) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        RemoteLoadManager.writeClassDescription(className, out);

        byte[] array = out.toByteArray();
        return array;
    }

    private Monitor monitor = new Monitor();

    private class Monitor {

        public synchronized void waitUntilFinished()
                throws InterruptedException {
            wait();
        }
    }
}
