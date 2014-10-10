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

This directory contains sample sources and wrapper scripts for each SigTest
command.

This Directory Contains:
-------------------
 - Implementation sources for:
      o example.test class: V1.0/, V2.0/
      o x.A class:          1/, 2/, 3/

 - UNIX and Windows versions of wrapper scripts for:
      o Setup command: setup.sh, setup.bat
      o SignatureTest command: sigtest.sh, sigtest.bat
      o SetupAndTest command: setupAndTest.sh, setupAndTest.bat
      o Merge command: merge.sh, merge.bat


Running SigTest Tool With Scripts:
------------------------------------

Because the wrapper scripts are designed to show how SigTest tool commands
can be run, they use the minimal set of options. For detailed documentation
consult the SigTest Tool User's Guide.

Required options are described inside of the scripts as comments.

The scripts assume that:
 - Sources are compiled
 - The CLASSPATH environment variable is set to include the required JAR 
   file (see the comments in the scripts)
 - The JAVA_HOME variable is set to the base directory of the Java platform 
   runtime environment installation 

