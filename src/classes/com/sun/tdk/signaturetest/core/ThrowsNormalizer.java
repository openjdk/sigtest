/*
 * Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.signaturetest.core;

import com.sun.tdk.signaturetest.model.ClassDescription;
import com.sun.tdk.signaturetest.model.MemberDescription;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Normalize the throws list completely for 'src' mode
 *
 * @author Maxim Sokolnikov
 *
 * @author Mikhail Ershov
 * @author Roman Makarchuk
 */
public class ThrowsNormalizer {

    public void normThrows(ClassDescription c, boolean removeJLE) throws ClassNotFoundException {
        normThrows(c, removeJLE, false);
    }

    public void normThrows(ClassDescription c, boolean removeJLE, boolean allowMissingTh) throws ClassNotFoundException {
        ClassHierarchy h = c.getClassHierarchy();

        for (Iterator<MemberDescription> e = c.getMembersIterator(); e.hasNext();) {
            MemberDescription mr = e.next();
            if (mr.isMethod() || mr.isConstructor()) {
                normThrows(h, mr, removeJLE, allowMissingTh);
            }
        }
    }

    private boolean checkException(ClassHierarchy h, String candidate, String matchedException) throws ClassNotFoundException {
        return candidate.equals(matchedException) || h.isSubclass(candidate, matchedException);
    }

    private void normThrows(ClassHierarchy h, MemberDescription mr, boolean removeJLE, boolean allowMissingTh) throws ClassNotFoundException {
        assert mr.isMethod() || mr.isConstructor();

        String throwables = mr.getThrowables();

        if (throwables.length() != 0) {

            xthrows.clear();

            {
                int startPos = 0, pos;
                do {
                    pos = throwables.indexOf(MemberDescription.THROWS_DELIMITER, startPos);
                    if (pos != -1) {
                        xthrows.add(throwables.substring(startPos, pos));
                        startPos = pos + 1;
                    } else {
                        xthrows.add(throwables.substring(startPos));
                    }

                } while (pos != -1);
            }

            int superfluousExceptionCount = 0;

            //  Scan over all throws ...
            for (int i = 0; i < xthrows.size(); i++) {
                String s = xthrows.get(i);

                if (s == null) {
                    continue;
                }

                if (s.charAt(0) != '{' /* if not generic */ && allowMissingTh) {
                    try {
                        h.getSuperClasses(s);
                    } catch (ClassNotFoundException cnfe) {
                        xthrows.set(i, null);
                        superfluousExceptionCount++;
                        continue;
                    }
                }

                if (s.charAt(0) != '{' /* if not generic */) {

                    if (checkException(h, s, "java.lang.RuntimeException")
                            || (removeJLE && checkException(h, s, "java.lang.Error"))) {
                        xthrows.set(i, null);
                        superfluousExceptionCount++;
                    } else {
                        for (int k = i + 1; k < xthrows.size(); ++k) {
                            String anotherThrowable = xthrows.get(k);

                            if (anotherThrowable == null) {
                                continue;
                            }

                            if (checkException(h, s, anotherThrowable)) {
                                xthrows.set(i, null);
                                superfluousExceptionCount++;
                                break;
                            }

                            if (checkException(h, anotherThrowable, s)) {
                                xthrows.set(k, null);
                                superfluousExceptionCount++;
                            }
                        }
                    }
                }
            }

            //  Should the throws list be updated ?
            if (superfluousExceptionCount != 0) {
                int count = 0;
                sb.setLength(0);

                for (String s : xthrows) {
                    if (s != null) {
                        if (count++ != 0) {
                            sb.append(MemberDescription.THROWS_DELIMITER);
                        }
                        sb.append(s);
                    }
                }

                if (count == 0) {
                    mr.setThrowables(MemberDescription.EMPTY_THROW_LIST);
                } else {
                    mr.setThrowables(sb.toString());
                }
            }
        }
    }
    private final List<String> xthrows = new ArrayList<>();
    private final StringBuffer sb = new StringBuffer();
}
