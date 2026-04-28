# UDB VSCode Extension

A Visual Studio Code extension for working with **UDB (Unified Database)** schema files used in the [RISC-V Unified Database](https://github.com/riscv-software-src/riscv-unified-db) project.

## Features

### Syntax Highlighting
Full syntax highlighting for UDB schema files, making it easier to read and navigate complex database definitions at a glance.

### Autocomplete
Context-aware autocomplete suggestions as you type, helping you write valid UDB schemas faster and with fewer errors.

### Schema Support
Dedicated support for the following UDB schema types:
- **CSR schemas** — author Control and Status Register definitions
- **Instruction schemas** — define and edit RISC-V instruction definitions
- **Extension schemas** — work with RISC-V extension definitions
- **Config schemas** — define architecture configurations
- **Exception code schemas** — define exception codes
- **Instruction opcode schemas** — define instruction opcodes
- **Instruction variable type schemas** — define instruction variable types
- **Interrupt code schemas** — define interrupt codes
- **Manual schemas** — define manual metadata
- **Manual version schemas** — define manual version metadata
- **Non-ISA schemas** — define non-ISA specifications
- **Profile family schemas** — define profile families
- **Profile schemas** — define RISC-V profiles
- **Register file schemas** — define register files

### Cross-Referencing
Navigate across related schema definitions with cross-referencing support — jump to referenced definitions directly from within your editor.

## Requirements

- **Visual Studio Code** `v1.109.0` or higher
- **Java 21 or later** — required to run the language server

## Installing Java

If you don't have Java 21+ installed, the extension will show an error with installation instructions. Here's a quick setup guide:

### macOS
```bash
brew install openjdk@21
```

### Windows
Download and install from [Adoptium](https://adoptium.net/) or [Oracle JDK](https://www.oracle.com/java/technologies/downloads/#java21)

### Linux (Ubuntu/Debian)
```bash
sudo apt update
sudo apt install openjdk-21-jdk
```

### Linux (Fedora/RHEL)
```bash
sudo dnf install java-21-openjdk
```

Verify installation:
```bash
java -version
```

You should see output like:
```
openjdk version "21.0.x" ...
```

## Getting Started

1. Install Java 21+ (see above)
2. Install the extension from the VS Code Marketplace
3. Open a folder containing your UDB schema files
4. Start editing — syntax highlighting, autocomplete, and cross-referencing will activate automatically on `.udb` files

## Troubleshooting

### Language server doesn't start
1. Check the Output panel: `View → Output` and select **UDB Language Server** from the dropdown
2. Verify Java is installed: run `java -version` in your terminal
3. If Java is installed but the extension doesn't find it, configure the path in VS Code settings:

```json
"udb.javaPath": "/path/to/java"
```

For example on macOS with Homebrew:
```json
"udb.javaPath": "/opt/homebrew/opt/openjdk@21/bin/java"
```

### Version mismatch error
If you see an error about Java version, ensure you have Java 21 or later installed:
```bash
java -version
```

If you have multiple Java versions installed, use the `udb.javaPath` setting to point to Java 21+.

## Feedback & Contributions

This extension is developed and maintained by the **Harvey Mudd Clinic Team**. For bugs or feature requests, please open an issue on the project repository.