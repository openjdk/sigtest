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
package com.sun.tdk.samples.helloworld.api;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Defines time of day.
 *
 * <p>
 * This class contains design mistake. It overrides toString but does not
 * override default implementation of valueOf(String). As result
 * TimeOfDay.valueOf(NIGHT.toString()) will not be equals NIGHT (actually
 * java.lang.IllegalArgumentException will be thrown) This leak must be detected
 * by unit tests.
 *
 * @see com.sun.tdk.samples.helloworld.api.TimeOfDay#valueOf(String)
 * @see com.sun.tdk.samples.helloworld.api.TimeOfDay#toString()
 * @author Mikhail Ershov
 */
public enum TimeOfDay {

    /**
     * The morning
     */
    MORNING,
    /**
     * The afternoon
     */
    AFTERNOON,
    /**
     * The evening
     */
    EVENING,
    /**
     * The night
     */
    NIGHT;

    /**
     * Returns current local TimeOfDay for specified TimeZone according the
     * following rules:
     * <li>if hours less then 7 this is night
     * <li>else if it less then 12 this is morning
     * <li>else if it less than 18 this is afternoon
     * <li>else this is evening
     *
     * @param tz given TimeZone
     * @return a TimeOfDay
     */
    public static TimeOfDay getCurrentTime(TimeZone tz) {
        int hours = Calendar.getInstance(tz).get(Calendar.HOUR_OF_DAY);
        if (hours < 7) {
            return NIGHT;
        } else if (hours < 12) {
            return MORNING;
        } else if (hours < 18) {
            return AFTERNOON;
        } else {
            return EVENING;
        }
    }

    /**
     * Represents a TimeOfDay value as String
     *
     * @return String representation of the TimeOfDay value
     */
    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
