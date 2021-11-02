# Let's try makefiles on Kotlin or JAVA	
# we will try to create one sample makefile to compile and create JAR files
# both java and kotlin are jvm - running compiler on single files is bad
# both java and kotlin are diffent from c as the compilers honor package structure not folders
#   so we compile in src/main/java but that gets used as base by compiler

# Project Artifacts - Starts with SRC_FILES
PNAME=jabba
ENTRYPOINT=com.reliancy.jabba.Router
DIR_COMPILE=build
DIR_PACKAGE=dist
DIR_SRC=src
DIR_LIB=target/lib
MANIFEST=
MANIFEST_AUTO=$(DIR_PACKAGE)/manifest.mf

ASSETS=%.txt %.html %.css %.js %.png %.jsp
# Params
KOTLIN_BASE=$(DIR_SRC)/main/kotlin
KOTLIN_EXT = kt
JAVA_BASE=$(DIR_SRC)/main/java
JAVA_EXT = java
OBJ_EXT = class
JFLAGS=-g
KFLAGS=
KOTLIN_TODO=kotlin_todo.txt
JAVA_TODO=java_todo.txt

# Tools
PRINT	:= @printf
RM		:= @rm -rf
COPY	:= @cp
MOVE	:= @mv
JVC		:= javac
KTC		:= java -jar $(subst \,/,$(KOTLIN_HOME))/lib/kotlin-compiler.jar
TEST_IF := @test -s
JAR		:= @jar -cvfm
MAKEDIR	:= @mkdir -p
CFIND	:= find
FIND    := @$(CFIND)

# File Sets
ALL_SRC=$(subst \,/,$(shell $(CFIND) $(DIR_SRC) -type f))
ASSETS_SRC=$(filter $(ASSETS),$(ALL_SRC))
ASSETS_JAVA:=$(subst $(JAVA_BASE),$(DIR_COMPILE),$(filter $(JAVA_BASE)/%,$(ASSETS_SRC)))
ASSETS_KOTLIN:=$(subst $(KOTLIN_BASE),$(DIR_COMPILE),$(filter $(KOTLIN_BASE)/%,$(ASSETS_SRC)))
SRC_DIRS ?=$(subst \,/,$(shell $(CFIND) $(DIR_SRC) -type d -print))
KOTLIN_SRC=$(foreach dir,$(SRC_DIRS),$(filter $(KOTLIN_BASE)/%,$(wildcard $(dir)/*.$(KOTLIN_EXT))))
KOTLIN_OBJ=$(KOTLIN_SRC:$(KOTLIN_BASE)/%.$(KOTLIN_EXT)=$(DIR_COMPILE)/%.$(OBJ_EXT))
JAVA_SRC=$(foreach dir,$(SRC_DIRS),$(filter $(JAVA_BASE)/%,$(wildcard $(dir)/*.$(JAVA_EXT))))
JAVA_OBJ=$(JAVA_SRC:$(JAVA_BASE)/%.$(JAVA_EXT)=$(DIR_COMPILE)/%.$(OBJ_EXT))
LIBS=$(foreach dir,$(DIR_LIB),$(wildcard $(dir)/*.jar))
SPACE=$() $()
CLASSPATH=$(subst $(SPACE),;,$(LIBS))
JFLAGS:= -cp '$(CLASSPATH)'
KFLAGS:= -cp '$(CLASSPATH)'

# Common Targets
.SUFFIXES:

# rule to build kotlin
$(DIR_COMPILE)/%.$(OBJ_EXT): $(KOTLIN_BASE)/%.$(KOTLIN_EXT)
	$(PRINT) '\tCollecting source: %s\n' $<
	$(PRINT) "$<\n" >> $(DIR_COMPILE)/$(KOTLIN_TODO)
# rule to build java
$(DIR_COMPILE)/%.$(OBJ_EXT): $(JAVA_BASE)/%.$(JAVA_EXT)
	$(PRINT) '\tCollecting source: %s\n' $<
	$(PRINT) "$<\n" >> $(DIR_COMPILE)/$(JAVA_TODO)
# rule to move assets - observe static pattern rules
$(ASSETS_KOTLIN): $(DIR_COMPILE)/% : $(KOTLIN_BASE)/%
	$(MAKEDIR) $(dir $@)
	$(COPY) --update $< $@ && printf "\tCopy asset:$< -> $@\n"
$(ASSETS_JAVA): $(DIR_COMPILE)/% : $(JAVA_BASE)/%
	$(MAKEDIR) $(dir $@)
	$(COPY) --update $< $@ && printf "\tCopy asset:$< -> $@\n"

#default: dist
package: compile | $(DIR_PACKAGE)
	$(PRINT) "Packaging\n"
	$(RM) $(DIR_COMPILE)/$(KOTLIN_TODO)
	$(RM) $(DIR_COMPILE)/$(JAVA_TODO)
#	$(FIND) $(BUILD_DIR) -name "META-INF" -exec rm -rf {} +
ifeq ($(MANIFEST),)
	$(PRINT) "Manifest-Version: 1.0\n" >> $(MANIFEST_AUTO)
	$(PRINT) "Created-By: Reliancy Makefile\n" >> $(MANIFEST_AUTO)
	$(PRINT) "Class-Path: $(foreach lib,$(LIBS),lib/$(notdir $(lib)))\n" >> $(MANIFEST_AUTO)
ifneq ($(ENTRYPOINT),)
	$(PRINT) "Main-Class: $(ENTRYPOINT)\n" >> $(MANIFEST_AUTO)
endif
else
	$(COPY) $(MANIFEST) $(MANIFEST_AUTO)
endif
	$(JAR) $(DIR_PACKAGE)/$(PNAME).jar $(MANIFEST_AUTO) -C $(DIR_COMPILE) .
compile: compile_kotlin compile_java
	$(PRINT) "Compiling done.\n"

compile_kotlin: $(KOTLIN_OBJ) | $(DIR_COMPILE) $(ASSETS_KOTLIN)
ifneq ($(KOTLIN_OBJ),)
#	$(PRINT) "KtSrc:$(KOTLIN_SRC)\n"
#	$(PRINT) "KtObj:$(KOTLIN_OBJ)\n"
	$(PRINT) 'Building Kotlin\n'
	$(KTC) $(KFLAGS) -d $(DIR_COMPILE) @$(DIR_COMPILE)/$(KOTLIN_TODO)
endif

compile_java: $(JAVA_OBJ) | $(DIR_COMPILE) $(ASSETS_JAVA)
ifneq ($(JAVA_OBJ),)
	$(PRINT) "JavaSrc:$(JAVA_SRC)\n"
	$(PRINT) "JavaObj:$(JAVA_OBJ)\n"
	$(PRINT) "Building Java\n"
	@$(JVC) $(JFLAGS) -d $(DIR_COMPILE) @$(DIR_COMPILE)/$(JAVA_TODO)
endif

$(DIR_COMPILE):
	$(PRINT) "prepare dir: $(DIR_COMPILE).\n"
	$(MAKEDIR) $(DIR_COMPILE)
	$(RM) $(DIR_COMPILE)/$(KOTLIN_TODO)
	$(RM) $(DIR_COMPILE)/$(JAVA_TODO)

$(DIR_PACKAGE):
	$(PRINT) "prepare dir: $(DIR_PACKAGE).\n"
	$(MAKEDIR) $(DIR_PACKAGE)
	$(MAKEDIR) $(DIR_PACKAGE)/lib
	$(COPY) $(LIBS) $(DIR_PACKAGE)/lib/

run:
	$(PRINT) "Running\n"
test:
	$(PRINT) "Testing\n"
clean:
	$(RM) $(DIR_COMPILE)
	$(RM) $(DIR_PACKAGE)
.PHONY: package compile dirs run test clean 
