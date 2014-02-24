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

import com.sun.tdk.samples.helloworld.api.Places;
import com.sun.tdk.samples.helloworld.api.TimeOfDay;
import junit.framework.TestCase;

import java.util.TimeZone;

public class TestGreet2 extends TestCase {

    public void testPlaces() {
        Places[] pls = Places.values();
        assertTrue(pls.length > 0);
        assertTrue(Places.USA.getTz().getRawOffset() != Places.Russia.getTz().getRawOffset());
        Places[] shuffled = Places.shuffledValues();
        assertTrue(pls.length == shuffled.length);
        boolean diff = false;
        for (int i = 0; i < pls.length; i++) {
            diff = shuffled[i].ordinal() != i;
            if (diff) {
                break;
            }
        }
        assertTrue(diff);
    }

    public void testTimeOfDay() {
        TimeOfDay[] tms = TimeOfDay.values();
        assertTrue(tms.length > 0);
        TimeZone usTz = Places.USA.getTz();
        TimeZone ruTz = Places.Russia.getTz();
        assertTrue(TimeOfDay.getCurrentTime(usTz) != TimeOfDay.getCurrentTime(ruTz));
        for (TimeOfDay tof : TimeOfDay.values()) {
            String s = tof.toString();
            TimeOfDay tofFound = TimeOfDay.valueOf(s);
            assertEquals(tof, tofFound);
            assertTrue(tof == tofFound);
        }
    }

}
