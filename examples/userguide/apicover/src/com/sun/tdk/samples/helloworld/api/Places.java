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
package com.sun.tdk.samples.helloworld.api;

import java.util.*;

/**
 * Defines well known places or countries with their "default" timezones
 *
 * @author Mikhail Ershov
 */
public enum Places {

    /**
     * America, based on New York time
     */
    USA("America/New_York"),
    /**
     * China, based on Hong Kong time
     */
    China("Asia/Hong_Kong"),
    /**
     * Russia, based on Moscow time
     */
    Russia("Europe/Moscow"),
    /**
     * India, based on Calcutta time
     */
    India("Asia/Calcutta"),
    /**
     * UK, based on London time
     */
    UK("Europe/London", "United Kingdom"),
    /**
     * Brazil, based on East Brazilian time
     */
    Brazil("Brazil/East"),
    /**
     * Czech, based on Prague time
     */
    Czech("Europe/Prague", "Czech Republic"),
    /**
     * Mali, based on Timbuktu time
     */
    Mali("Africa/Timbuktu");

    private TimeZone tz;
    private String country;
    private String tzId;

    private Places(String timeZoneID) {
        this.tzId = timeZoneID;
        this.country = this.toString();
    }

    private Places(String timeZoneID, String country) {
        this.tzId = timeZoneID;
        this.country = country;
    }

    /**
     * Return TimeZone for the place
     *
     * @see java.util.TimeZone
     * @return a TimeZone
     */
    public TimeZone getTz() {
        if (tz == null) {
            tz = TimeZone.getTimeZone(tzId);
        }
        return tz;
    }

    /**
     * Return enum values in random order
     *
     * @return a shuffled array of all enum values
     */
    public static Places[] shuffledValues() {
        List<Places> result = Arrays.asList(values());
        Collections.shuffle(result);
        return result.toArray(new Places[]{});
    }

    /**
     * Gets country name
     *
     * @return a country name
     */
    public String getCountry() {
        return country;
    }
}
