/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.signaturetest.model;

import com.sun.tdk.signaturetest.util.SwissKnife;
import java.util.*;

/**
 * @author Serguei Ivashin (isl@nbsp.nsk.su)
 */
public class AnnotationItem implements Comparable {

    public static final String ANNOTATION_PREFIX = "anno";
    public static final String ANNOTATION_INHERITED = "java.lang.annotation.Inherited";
    public static final String ANNOTATION_DOCUMENTED = "java.lang.annotation.Documented";
    public static final String ANNOTATION_REPEATABLE = "java.lang.annotation.Repeatable";
    private final static String INTF = "interface ";

    public AnnotationItem(int target) {
        setTarget(target);
    }

    public AnnotationItem(int target, String name) {
        setTarget(target);
        setName(name);
    }

    public AnnotationItem() {
    }

    public int compareTo(Object o) {
        AnnotationItem that = (AnnotationItem) o;
        int diff = getSpecificData().compareTo(that.getSpecificData());
        if (diff == 0) {
            diff = name.compareTo(that.name);

            if (diff == 0) {
                if (members == that.members) {
                    return 0;
                }

                if (members == null) {
                    return -1;
                }

                if (that.members == null) {
                    return 1;
                }

                diff = members.size() - that.members.size();
                if (diff == 0) {

                    Iterator it = members.iterator();
                    Iterator that_it = that.members.iterator();

                    while (it.hasNext() && diff == 0) {
                        Member m = (Member) it.next();
                        diff = m.compareTo(that_it.next());
                    }
                }
            }
        }

        return diff;
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AnnotationItem other = (AnnotationItem) obj;
        return compareTo(other) == 0;
    }

    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + this.target;
        hash = 79 * hash + (this.inheritable ? 1 : 0);
        hash = 79 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 79 * hash + (this.members != null ? this.members.hashCode() : 0);
        return hash;
    }
    public final static AnnotationItem[] EMPTY_ANNOTATIONITEM_ARRAY = new AnnotationItem[0];
    // If this annotation imposed on a method/constructor parameter, then target
    // is number of the parameter + 1, otherwise 0.
    private int target;
    // True if this annotation is marked with the 'Inherited' meta-annotation
    private boolean inheritable = false;
    //  Type name of the annotation.
    private String name;
    //  List of the member/value pairs.
    private SortedSet/*Member*/ members = null;

    protected Set getMembers() {
        return members;
    }

    public final String getName() {
        return name;
    }

    public final void setName(String name) {
        this.name = name.intern();
    }

    public final int getTarget() {
        return target;
    }

    public final void setTarget(int t) {
        target = t;
    }

    public void addMember(Member m) {
        if (members == null) {
            members = new TreeSet();
        }
        members.add(m);
    }

    public boolean isInheritable() {
        return inheritable;
    }

    public void removeMember(Member m) {
        members.remove(m);
    }

    public static void normaliazeAnnotation(AnnotationItem an, Set orderImportant) {
        // orderImportant can be null for some unit-tests
        if (orderImportant != null && !orderImportant.contains(an.name) && an.members != null) {
            Iterator it = an.members.iterator();
            while (it.hasNext()) {
                normAnnMember((Member) it.next());
            }
        }
    }

    private static Member normAnnMember(Member m) {
        //System.out.println("AnnItem.normAnnMember was=" + m.value);
        if (m.value.startsWith("[") && m.value.endsWith("]")) {
            // sort them
            String sValues = m.value.substring(1, m.value.length() - 1);
            StringTokenizer st = new StringTokenizer(sValues, ",");
            ArrayList ts = new ArrayList();
            while (st.hasMoreTokens()) {
                ts.add(st.nextToken().trim());
            }
            StringBuffer newV = new StringBuffer("[");
            Collections.sort(ts);
            Iterator it = ts.iterator();
            while (it.hasNext()) {
                newV.append((String) it.next());
                if (it.hasNext()) {
                    newV.append(",");
                }
            }
            newV.append(']');
            m.value = newV.toString();
        }
        //System.out.println("AnnItem.normAnnMember is=" + m.value);
        return m;
    }

    public static class Member implements Comparable {

        public String type;
        public String name;
        public String value;

        public Member(String type, String name, Object value) {
            this.type = type;
            this.name = name;
            setValue(value);
        }

        public Member(String name, Object value) {
            this.name = name;
            setValue(value);
        }

        public Member() {
        }

        public int compareTo(Object x) {

            Member that = (Member) x;

            int result = 0;

            if (type != null && that.type != null) {
                result = type.compareTo(that.type);
            } else if (type == null && that.type == null) {
                // nothing to do
            } else if (type == null) {
                result = -1;
            } else {
                result = 1;
            }

            if (result == 0) {
                result = name.compareTo(that.name);
                if (result == 0) {
                    result = value.compareTo(that.value);
                }
            }
            return result;
        }

        public int hashCode() {
            int hash = 7;
            hash = 37 * hash + (type == null ? 0 : type.hashCode());
            hash = 37 * hash + (name == null ? 0 : name.hashCode());
            hash = 37 * hash + (value == null ? 0 : value.hashCode());
            return hash;
        }

        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Member other = (Member) obj;
            if (!SwissKnife.equals(this.type, other.type)) {
                return false;
            }
            if (!SwissKnife.equals(this.name, other.name)) {
                return false;
            }
            if (!SwissKnife.equals(this.value, other.value)) {
                return false;
            }
            return true;
        }

        public void setType(String type) {
            this.type = type;
        }

        public void setValue(Object value) {
            this.value = value == null ? null : MemberDescription.valueToString(value);
            if (value instanceof Class && this.value.startsWith(INTF)) {
                this.value = "class " + this.value.substring(INTF.length());
            }
        }
    }

    public static class ValueWrap {

        String value;

        public ValueWrap(String s) {
            value = s;
        }

        public String toString() {
            return value;
        }
    }

    public Member findByName(String name) {
        if (members != null) {
            for (Iterator it = members.iterator(); it.hasNext();) {
                Member m = (Member) it.next();
                if (m.name.equals(name)) {
                    return m;
                }
            }
        }

        return null;
    }

    // for extensions
    protected String getSpecificData() {
        return "" + target;
    }

    protected String getPrefix() {
        return ANNOTATION_PREFIX;
    }
    // -----

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getPrefix()).append(" ");

        if (!"".equals(getSpecificData())) {
            sb.append(getSpecificData()).append(" ");
        }

        sb.append(name).append('(');
        int i = 0;
        if (members != null) {
            for (Iterator it = members.iterator(); it.hasNext();) {
                Member m = (Member) it.next();
                if (i++ != 0) {
                    sb.append(", ");
                }
                sb.append(m.type).append(' ').append(m.name).append("=");
                sb.append(m.value);
            }
        }
        sb.append(')');

        return sb.toString();
    }

    public void setInheritable(boolean inh) {
        inheritable = inh;
    }

    public static AnnotationItem[] toArray(List/*AnnotationItem*/ alist) {
        if (alist == null || alist.size() == 0) {
            return EMPTY_ANNOTATIONITEM_ARRAY;
        }

        final int asize = alist.size();
        AnnotationItem[] tmp = new AnnotationItem[asize];
        for (int i = 0; i < asize; ++i) {
            tmp[i] = (AnnotationItem) alist.get(i);
        }
        return tmp;
    }
}
