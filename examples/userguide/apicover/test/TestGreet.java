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

import com.sun.tdk.samples.helloworld.api.Greet;
import com.sun.tdk.samples.helloworld.api.GreetFactory;
import com.sun.tdk.samples.helloworld.api.Places;
import com.sun.tdk.samples.helloworld.api.TimeOfDay;
import junit.framework.TestCase;

import java.io.StringWriter;

public class TestGreet extends TestCase {

    public void testGreetWorld() {
        StringWriter sw = new StringWriter();
        Greet gr = GreetFactory.getInstance();
        gr.setWriter(sw);
        gr.greetWorld();
        String out = sw.toString();
        assertNotNull(out);
        assertFalse("".equals(out));
        assertTrue(out.contains("Hello"));
        assertTrue(out.contains("world"));
        assertTrue(out.contains("!"));
    }

    public void testGreetPlaces() {
        StringWriter sw = new StringWriter();
        Greet gr = GreetFactory.getInstance();
        gr.setWriter(sw);
        gr.greetPlaces();
        String out = sw.toString();
        assertNotNull(out);
        assertFalse("".equals(out));
        assertTrue(out.contains("US"));
        assertFalse(out.contains("Moscow"));
        assertTrue(out.contains("Good"));

        boolean found = false;
        for (TimeOfDay tod : TimeOfDay.values()) {
            if (out.contains(tod.toString())) {
                found = true;
                break;
            }
        }

        assertTrue(found);
    }

    public void testPlacesSimple() {
        Places[] pls = Places.values();
        assertTrue(pls.length > 0);
        assertNotNull(Places.Brazil);
    }

}
