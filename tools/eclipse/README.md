# Programmable RISC-V IDE (PRIDE) Developer Guide

*Authors: Brayden Mendoza (brayjmendoza), Nina Luo (niluo-shiqi)*
*Last Edited: June 11th, 2026*

## Overview

PRIDE is implemented using [Xtext](https://eclipse.dev/Xtext/documentation/), an Eclipse framework used for developing domain-specific languages (DSL). Thus, contributing to this project requires using the Eclipse IDE with Xtext installed.

With Xtext, we just have to specify a grammar and all of the IDE features will be generated for us (in particular, syntax highlighting, syntax errors, and cross-referencing). However, we can further customize what Xtext generates with some additional code. It should be noted that Xtext by default only generates IDE features for Eclipse. Luckily, Xtext is compliant with the Language Server Protocol (LSP), meaning that we can very easily generate a language server, which can then be used to support other IDEs (we currently support VSCode with our [UDB Schema Editor extension](https://marketplace.visualstudio.com/items?itemName=HarveyMuddClinicTeam.udb-schema-editor)).

So, at a high level, developing PRIDE involves modifying the files in the Xtext project (primarily the grammar file and validator) to create new features. Then, you would then generate a new language server to update support in other IDEs. The rest of this markdown file will go into much greater detail of everything involved in this project.

## Table of Contents

- [Getting Started](#getting-started)
- [The Grammar](#the-grammar)
  - [Structure](#structure)
  - [Whitespace Awareness](#whitespace-awareness)
- [The Validator](#the-validator)
- [Customizing Xtext Components](#customizing-xtext-components)
  - [Cross-Referencing](#cross-referencing)
  - [Hex and Binary](#hexadecimal-and-binary)
- [Maven](#maven)
- [JUnit Testing](#junit-testing)
- [The Language Server](#the-language-server)
- [Converting Between YAML and UDB](#converting-between-udb-and-yaml)
- [Other Notes & Quirks](#other-notes--quirks)

---

## Getting Started

To begin development with this project, first download Eclipse with Xtext. For a fresh install, download the "Eclipse IDE for Java and DSL Developers" found [here](https://www.eclipse.org/downloads/packages/). If you already have Eclipse installed, you can find more instructions [here](https://eclipse.dev/Xtext/download.html). Be sure to have GitHub set up with Eclipse.

Now, in a fresh workspace, import this repository. To do so, click Import projects (found under Package or Project Explorer), then click Git -> Projects with Git (with smart import). Then choose your repository source. Before clicking finish, I would recommend deselecting the root of this repository (i.e., `riscv-unified-db`). This is because the entire Xtext project is contained within `tools/eclipse/dev/org.xtext.udb.parent`. We've personally had issues with Eclipse when we imported the entire `riscv-unified-db` repository, and found that excluding the root prevented them.

Next, navigate to the [GenerateUdb.mwe2](dev/org.xtext.udb.parent/org.xtext.udb/src/org/xtext/example/udb/GenerateUdb.mwe2) file found in the `org.xtext.udb` package. Right click this file in the project explorer and press Run As -> MWE2 Workflow. This workflow is what Xtext uses to generate all of the artifacts of the project, including the IDE features. When you first run it, Eclipse will mention how there are errors in the project. This is expected, press Proceed. This process may take some time. If you run into issues, in the project explorer right click on the `org.xtext.udb.parent` directory and press Maven -> Update Project (this will definitely take some time). Now, run the MWE2 workflow again.

 Once everything has generated, you should now be able to use the IDE features in Eclipse! In the project explorer, right click on `org.xtext.example.udb` (the package that contains the MWE2 workflow), and press Run As -> Eclipse Application. This will open a new instance of Eclipse that you can use to test out the features of PRIDE. First, create a new general project, and then create a new file with the `.udb` file extension. Eclipse will then ask you if you would like to convert the project into an Xtext project. Click yes, and you're all set!

---
## The Grammar

The grammar is a `.xtext` file that defines allowable syntax. This defined syntax is YAML-like, to match the `.yml` files of RISC-V specifications. Thus, the Xtext grammar serves as a YAML parser for RISC-V specifications.

#### Brief Notes on Current Implementation 

- Conditions are currently defined as strings (see [note on conditions](#note-on-conditions))
- IDL is defined separately in a Ruby Treetop grammar, which is ported into the Xtext project with JRuby (see [IDL](#isa-description-language))

#### Location

The grammar file is can be found [here](dev/org.xtext.udb.parent/org.xtext.udb/src/org/xtext/example/udb/Udb.xtext) (`tools/eclipse/dev/org.xtext.udb.parent/org.xtext.udb/src/org/xtext/example/udb/Udb.xtext`).

#### Structure

The start of the file begins with the parent rule of the grammar, `Model`, which lists all of the currently supported schemas. Then, we have rules for each of these schemas, which serve as the parent rule for their own respective grammars. Next, there's a chunk of code that contains grammar snippets that are commonly used across multiple schemas. 

Then, we have the grammars for each of the schema's, written in the order they are listed in `Model`. This is the bulk of the file. To modify the grammars of existing schemas, you will want to change/add code in this section.

Finally, we have the grammar for conditions, which is currently commented out for reasons explained below. The rest of the file contains terminals and helper rules which are not tied to any particular schema.

In general, if you would like to add support for a new schema, just follow the examples found in the `.xtext` file. Using the CSR schema as an example, we can see that the it is listed in `Model`, and then later the `CsrModel` rule is defined (this is the parent rule for the schema). This rule uses a bunch of CSR-specific grammar rules that are all defined further down the file. These are obvious as they are all prefixed with `Csr`, except for the commonly used rules. When developing new schemas or modifying existing ones, please follow this structure and maintain the existing code style.

**IMPORTANT NOTE:** The first two grammar rules of every schema must be Schema and Kind (in that order). Using CSR as an example, you can see that in the definition for `CsrModel`, the first rule is `Schema` and the second is `CsrKind`. This is important as this is what allows the resulting IDE to determine which schema the RISC-V specification should be following. Note that this means that all `.udb` files must start with the `$schema` and `kind` fields (everything else can be unordered).

#### Whitespace Awareness

By default, Xtext grammars are not whitespace aware. So, to make the grammar YAML-like, we added synthetic `INDENT` and `DEDENT` tokens to keep track of indentations. However, to get Xtext to use these tokens to enforce indentations, we modified [UdbTokenSource.java](dev/org.xtext.udb.parent/org.xtext.udb/src/org/xtext/example/udb/parser/antlr/UdbTokenSource.java) in the `org.xtext.example.udb.parser.antlr` package. By overriding some functions, we were able to attain whitespace awareness. 

#### Note on Conditions

We do currently have a grammar implemented for conditions (see "conditions" in [schema_def.json](../../spec/schemas/schema_defs.json) for the official definition). However, we have found that this causes issues with syntax errors (see our [GitHub issue](https://github.com/niluo-shiqi/riscv-unified-db-hmc-clinic-team-/issues/4)). Since highlighting syntax errors is a very major and useful IDE feature, we decided to comment out this portion of the grammar. Until a solution has been implemented, we have replaced this grammar to just accept a simple string.

---

## The Validator

*Note to self: add idl info to this section*

### Location
`tools/eclipse/dev/org.xtext.udb.parent/org.xtext.udb/src/org/xtext/example/udb/validation/UdbValidator.java`

### Organization
The validator file is also organized by schema.

### Schema-Specific Validation
What validations a schema needs depends on what information the JSON file contains.

### Structure
- **Imports by schema** are placed at the top of the file
- **IDL integration** is placed at the bottom of the file

---

## Customizing Xtext Components

Though PRIDE is inherently an IDE project, by using Xtext we are more accurately creating a domain-specific language (DSL). As mentioned previously, given a grammar Xtext will generate IDE features. However, as a framework for DSL's, Xtext does this by generating all of the components that go into a language. These include a lexer, parser, the actual IDE features, and much, much more. 

Sometimes, what Xtext generates by default does quite do what we want it to. Luckily, we can customize these generated components (for the most part). In general, customizing Xtext components involves subclassing the class that Xtext generates, overriding necessary functions, and then registering the subclass in `UdbRuntimeModel.java`. The rest of this section details every instance where we've had to do this so far.

#### Cross-Referencing

Mention [UdbQualifiedNameProvider.java](dev/org.xtext.udb.parent/org.xtext.udb/src/org/xtext/example/udb/naming/UdbQualifiedNameProvider.java) and why we need it and  then registering it in [UdbRuntimeModule.java](dev/org.xtext.udb.parent/org.xtext.udb/src/org/xtext/example/udb/UdbRuntimeModule.java)

#### Hexadecimal and Binary

At the end of the `.xtext` file, there is a small chunk of code that defines a grammar for representing integers in either hexadecimal or binary. However, defining the grammar alone will not get Xtext to recognize hex and binary as legitimate integers. So, we created [UdbValueConverter.java](dev/org.xtext.udb.parent/org.xtext.udb/src/org/xtext/example/udb/UdbValueConverter.java). This file contains a class that subclasses Xtext's generated class that handles value conversion. The subclass then overrides some functions to allow conversion from hexadecimal and binary to integers in decimal form. We then register this class in [UdbRuntimeModule.java](dev/org.xtext.udb.parent/org.xtext.udb/src/org/xtext/example/udb/UdbRuntimeModule.java), so that at runtime hex and binary can be interpreted as actual integers. This proves useful for validation and testing.

---

## Maven

---

## JUnit Testing

---

## The Language Server

### Regenerating the Language Server

*Note: should probably add info on how to modify stuff like syntax highlighting and other ls/vs-code extension related things*

### Prerequisites
Pull the repository into your local editor.

### macOS

Run the following commands in your terminal to generate the jar file and Ruby dependencies in the correct location:

```bash
chmod +x tools/scripts/language-server-script/regen-udb-ls.sh
./tools/scripts/language-server-script/regen-udb-ls.sh
```

> **Note:** The `chmod` command only needs to be run once per session.

### Windows

Run the following command in your terminal to generate the jar file and Ruby dependencies in the correct location:

```bash
tools\scripts\language-server-script\regen-udb-ls.bat
```

### Expected Output
After running the script, you should see the following in `/tools/eclipse/udb-vscode/server`:
- `udb-ls-all.jar`
- `idlc` folder
- `vendor` folder

---

## Converting Between UDB and YAML

### Overview
UDB is not currently one-to-one with YAML. These differences are mainly due to fields in UDB being defined as strings (to avoid ambiguity problems that would occur if they were defined as unquoted IDs). To resolve these differences and remove friction, a Python conversion script has been created.

### Conversion Script
**File:** `tools/python/convertudb.py`

### How to Use

1. Download `convertudb.py` and place it in the same directory as the `.yaml` or `.udb` file you wish to convert.

2. Open your terminal and navigate to that directory:
   ```bash
   cd /path/to/file/directory
   ```

3. Run the conversion script:
   ```bash
   python convertudb.py filename.yaml
   ```

### Output
A new file will be created in the same directory with the same filename but opposite file extension.

**Example:**
- Input: `vsstatus.yaml`
- Output: `vsstatus.udb`

---

## Other Notes & Quirks
weird stuff like UdbGenerator2, 
