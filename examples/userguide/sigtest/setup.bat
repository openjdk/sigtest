@echo off
rem
rem $Id$
rem
rem Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
rem DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
rem
rem This code is free software; you can redistribute it and/or modify it
rem under the terms of the GNU General Public License version 2 only, as
rem published by the Free Software Foundation.  Oracle designates this
rem particular file as subject to the "Classpath" exception as provided
rem by Oracle in the LICENSE file that accompanied this code.
rem
rem This code is distributed in the hope that it will be useful, but WITHOUT
rem ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
rem FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
rem version 2 for more details (a copy is included in the LICENSE file that
rem accompanied this code).
rem
rem You should have received a copy of the GNU General Public License version
rem 2 along with this work; if not, write to the Free Software Foundation,
rem Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
rem
rem Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
rem or visit www.oracle.com if you need additional information or have any
rem questions.
rem
rem

rem Wrapper script for the Setup command
rem 
rem Usage: 
rem setup.bat <impl-classes> <package> <sig-file>
rem     <impl-classes> - path to implementation classes
rem     <package> - package to be tested along with subpackages
rem     <sig-file> - name of sig file to be generated
rem Environment settings:
rem CLASSPATH is set to contain the "sigtestdev.jar"
rem JAVA_HOME is set to the base directory of the Java runtime environment installation

java com.sun.tdk.signaturetest.Setup ^
    -classpath %1;"%JAVA_HOME%"\jre\lib\rt.jar ^
    -static ^
    -package %2 ^
    -filename %3
