#
# $Id$
#
# Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.  Oracle designates this
# particular file as subject to the "Classpath" exception as provided
# by Oracle in the LICENSE file that accompanied this code.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.

#
# Wrapper script for the SignatureTest command
# 
# Usage: 
# sh sigtest.sh <impl-classes> <package> <sig-file>
#     <impl-classes> - path to implementation classes
#     <package> - package to be tested along with subpackages
#     <sig-file> -  signature file name
# Environment settings:
# CLASSPATH is set to contain the "sigtestdev.jar" or "sigtest.jar"
# JAVA_HOME is set to the base directory of the Java runtime environment installation

java com.sun.tdk.signaturetest.SignatureTest \
    -classpath $1:$JAVA_HOME/jre/lib/rt.jar \
    -static \
    -package $2 \
    -filename $3 \
    -out report.txt
