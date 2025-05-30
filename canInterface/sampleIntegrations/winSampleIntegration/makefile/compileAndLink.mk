#
# Generic Makefile for GNU Make 3.82
#
# Compilation and linkage of C(++) code.
#
# Help on the syntax of this makefile is got at
# http://www.gnu.org/software/make/manual/make.pdf.
#
# Copyright (C) 2012-2024 Peter Vranken (mailto:Peter_Vranken@Yahoo.de)
#
# This program is free software: you can redistribute it and/or modify it
# under the terms of the GNU Lesser General Public License as published by the
# Free Software Foundation, either version 3 of the License, or any later
# version.
#
# This program is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
# for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this program. If not, see <http://www.gnu.org/licenses/>.
#
# Preconditions
# =============
#
# The makefile is intended to be executed by the GNU make utility 3.82.
#   The name of the project needs to be assigned to the makefile macro projectName, see
# heading part of the code section of this makefile.
# The system search path needs to contain the location of the GNU compiler/linker etc. This
# is the folder containing e.g. gcc or gcc.exe.
#   For your convenience, the Windows path should contain the location of the GNU make
# processor. If you name this file either makefile or GNUmakefile you will just have to
# type "make" in order to get your make process running.
#   This makefile does not handle blanks in any paths or file names. Please rename your
# paths and files accordingly prior to using this makefile.
#
# Targets
# =======
#
# The makefile provides several targets, which can be combined on the command line. Get
# some help on the available targets by invoking the makefile using
#   make help
#
# Options
# =======
#
# Options may be passed on the command line.
#   The follow options may be used:
#   CONFIG: The compile configuration is one out of DEBUG (default) or PRODUCTION. By
# means of defining or undefining macros for the C compiler, different code configurations
# can be produced. Please refer to the comments below to get an explanation of the meaning
# of the supported configurations and which according #defines have to be used in the C
# source code files.
#
# Input Files
# ===========
#
# The makefile compiles and links all source files which are located in a given list of
# source directories. The list of directories is hard coded in the makefile, please look
# for the setting of srcDirList below.
#   A second list of files is found as cFileListExcl. These C/C++ files are excluded from
# build.


# General settings for the makefile.
#$(info Makeprocessor in use is $(MAKE))

# Include some required makefile functionality.
include $(sharedMakefilePath)commonFunctions.mk
include $(sharedMakefilePath)locateTools.mk
include $(sharedMakefilePath)generateCode.mk # TODO: This is actually not a shared, common one!

# The name of the project is used for several build products. Should have been set in the
# calling makefile but we have a reasonable default.
project ?= appName

# The name of the executable file.
projectExe := $(project)$(dotExe)

# Access help as default target or by several names. This target needs to be the first one
# in this file.
.PHONY: h help targets usage
.DEFAULT_GOAL := help
h help targets usage:
	$(info Usage: make [-s] [-k] [MINGW_HOME=<pathToMingw>] [CONFIG=<configuration>] {<target>})
	$(info where <configuration> is one out of DEBUG (default) or PRODUCTION.)
	$(info Available targets are:)
	$(info   - build: Build the executable. Includes all others but help)
	$(info   - run: Build the executable and run it as configured in GNUmakefile)
	$(info   - compile: Compile all C(++) source files, but no linkage etc.)
	$(info   - clean: Delete all application files generated by the build process)
	$(info   - cleanDep: Delete all dependency files, e.g. after changes of #include statements)
	$(info   - rebuild: Same as clean and build together)
ifeq ($(osName),win)
	$(info   - bin/win<32Or64>/<configuration>/obj/<cFileName>.o: Compile a single C(++) module)
else
	$(info   - $(call binFolder)<configuration>/obj/<cFileName>.o: Compile a single C(++) module)
endif
	$(info   - <cFileName>.i: Preprocess a single C(++) module)
	$(info   - versionGCC: Print information about which compiler is used)
	$(info   - helpGCC: Print usage information of compiler)
	$(info   - builtinMacrosGCC: Print built-in #define's of compiler for given configuration)
	$(info   - help: Print this help)
	$(error)

# Concept of compilation configurations:
#
# Configuration PRODCUTION:
# - no self-test code
# - no debug output
# - no assertions
#
# Configuration DEBUG:
# + all self-test code
# + debug output possible
# + all assertions active
#
CONFIG ?= DEBUG
ifeq ($(CONFIG),PRODUCTION)
    $(info Compiling $(project) for production)
    cDefines := -D$(CONFIG) -DNDEBUG
else ifeq ($(CONFIG),DEBUG)
    $(info Compiling $(project) for debugging)
    cDefines := -D$(CONFIG)
else
    $(error Please set CONFIG to either PRODUCTION or DEBUG)
endif
#$(info $(CONFIG) $(cDefines))

# Where to place all generated products?
targetDir := $(call binFolder)$(CONFIG)/

# Ensure existence of target directory.
.PHONY: makeDir
makeDir: | $(targetDir)obj
$(targetDir)obj:
	-$(mkdir) -p $@

# Some core variables have already been set prior to reading this common part of the
# makefile. These variables are:
#   project: Name of the sub-project; used e.g. as name of the executable
#   CPU_TARGET_C: Selects the target hardware to be build for.
#   srcDirList: a blank separated list of directories holding source files
#   cFileListExcl: a blank separated list of source files (with extension but without path)
# excluded from the compilation of all *.c and *.cpp
#   incDirList: a blank separated list of directories holding header files. The directory
# names should end with a slash. The list must not comprise common, project independent
# directories and nor must the directories listed in srcDirList be included
#   sharedMakefilePath: The path to the common makefile fragments like this one

# Include directories common to all sub-projects are merged with the already set project
# specific ones.
incDirList := $(call w2u,$(incDirList)) .
#$(info incDirList := $(incDirList))

# Determine the list of files to be compiled.
cFileList := $(call rwildcard,$(srcDirList),*.c *.cpp)

# Remove the various paths. We assume unique file names across paths and will search for
# the files later. This strongly simplifies the compilation rules. (If source file names
# were not unique we could by the way not use a shared folder obj for all binaries.)
#   Before we remove the directories from the source file designations, we need to extract
# and keep these directories. They are needed for the VPATH search and for compiler include
# instructions. Note, $(sort) is applied to filter dublicates not for sorting.
srcDirList := $(sort $(dir $(cFileList)))
cFileList := $(notdir $(cFileList))
# Subtract each excluded file from the list.
cFileList := $(filter-out $(cFileListExcl), $(cFileList))
#$(info cFileList := $(cFileList)$(call EOL)srcDirList := $(srcDirList))
# Translate C source file names in target binary files by altering the extension and adding
# path information.
objList := $(cFileList:.cpp=.o)
objList := $(objList:.c=.o)
objListWithPath := $(addprefix $(targetDir)obj/, $(objList))
#$(info objListWithPath := $(objListWithPath))

# Include the dependency files. Do this with a failure tolerant include operation - the
# files are not available after a clean.
-include $(patsubst %.o,%.d,$(objListWithPath))

# Blank separated search path for source files and their prerequisites permit to use auto
# rules for compilation.
VPATH := $(srcDirList) $(targetDir)

# Pattern rules for compilation of C and C++ source files.
#   TODO You may need to add more include paths here.
cFlags += $(cDefines) -Wall -Wextra -Wstrict-overflow=4 -Wmissing-declarations              \
          -Wno-parentheses -Wno-unused-value -Werror=incompatible-pointer-types             \
          -fno-exceptions -ffunction-sections -fdata-sections -MMD                          \
          -Wa,-a=$(patsubst %.o,%.lst,$@) -std=c11                                          \
          $(foreach path,$(srcDirList) $(incDirList),-I$(path))                             \
          $(foreach def,$(defineList),-D$(def))
ifeq ($(CONFIG),DEBUG)
	cFlags += -ggdb3 -O0
else
	cFlags += -g -Ofast
endif
#$(info cFlags := $(cFlags))

$(targetDir)obj/%.o: %.c
	$(info Compiling C file $<)
	$(gcc) -c -fdiagnostics-show-option $(cFlags) -o $@ $<

#$(targetDir)obj/%.o: %.cpp
#	$(info Compiling C++ file $<)
#	$(gcc) -c $(cFlags) -o $@ $<

# Create a preprocessed source file, which is convenient to debug complex nested macro
# expansion.
%.i: %.c
	$(info Preprocessing C file $(notdir $<) to text file $(patsubst %.c,%.i,$<))
	$(gcc) -E $(filter-out -MMD,$(cFlags)) -o $(patsubst %.c,%.i,$<) $<

%.i: %.cpp
	$(info Preprocessing C++ file $(notdir $<) to text file $(patsubst %.cpp,%.i,$<))
	$(gcc) -E $(filter-out -MMD,$(cFlags)) -o $(patsubst %.cpp,%.i,$<) $<

# Windows only: A global resource file is compiled to a binary representation of the
# application's icons. The binary representation is linked with the executable. This makes
# Windows show the application with its own icon. Furthermore, the user can create an
# association of the file name extension of the application's input files with one of its
# icons.
#   No according code is supported for the other environments. Here, no icons are available
# as part of the executable file. The functionality (the actual code of the application) is
# not affected at all.
ifeq ($(osName),xxxInhibitRulexxx_win)
    # A single (compiled) resource file is demanded for the project if it is build under
    # Windows.
    projectResourceFile := $(targetDir)obj/$(project).res
    
    # A general auto rule for compiling resource files under Windows is added.
    #   TODO The rule is insufficient. The prerequisite is the *.rc file which references
    # external files, e.g. icon files. These external files should also be prerequisites.
    # Directly specifying these files in a rule would break the concept of a generic
    # makefile. We need a working hypothesis similar to the C/C++ code: Look for all icon
    # files in all input directories and add these as prerequisites. At the moment, a
    # change of an icon file won't be considered in the next build.
    $(targetDir)obj/%.res: %.rc
		$(info Compile Windows resource file $<)
		$(windres) $< -O coff -o $@
else
    # Empty variable: A (compiled) resource file is not known under this operating system.
    projectResourceFile :=
endif


## A general rule enforces rebuild if one of the configuration files changes
#$(objListWithPath): GNUmakefile ../shared/makefile/compileAndLink.mk                        \
#                    ../shared/makefile/locateTools.mk ../shared/makefile/commonFunctions.mk \
#                    ../shared/makefile/parallelJobs.mk


# 30 Years of DOS & Windows but the system still fails to handle long command lines. We
# write the names of all object files line by line into a simple text file and will only
# pass the name of this file to the linker.
$(targetDir)obj/listOfObjFiles.txt: $(objListWithPath) $(projectResourceFile)
	$(info Create linker input file $@)
	$(file >$@,$(sort $^))

# Let the linker create the Windows executable.
#   CAUTION: gcc 4.8.1 under MinGW-W64 produces a warning when producing a cross reference
# using switch --cref. The warning can be ignored, the executable build product is not
# affected. If a cross refenerce in the map file is of no particular use, one might
# simply remove the switch to avoid the warning.
lFlags = -Wl,--print-map,--cref,--warn-common
$(targetDir)$(projectExe): $(targetDir)obj/listOfObjFiles.txt
	$(info Linking project. Ouput is redirected to $(targetDir)$(project).map)
	$(gcc) $(lFlags) -o $@ @$< -lm > $(targetDir)$(project).map

# Delete all dependency files ignoring (-) the return code from Windows.
.PHONY: cleanDep
cleanDep:
	-$(rm) -f $(targetDir)obj/*.d

# Delete all application products ignoring (-) the return code from Windows.
.PHONY: clean
clean:
	-$(rm) -f $(targetDir)obj/*
	-$(rm) -f $(targetDir)$(project).*
