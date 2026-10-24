# FunctionCraft Compiler â€” Name Analysis

A Java/ANTLR compiler coursework project for the FunctionCraft language. It builds an abstract syntax tree, reports name-analysis errors, checks function dependencies for cycles, and prints the AST when those checks succeed.

## Structure

- `PLC-main/CA2/code/src/main/grammar/FunctionCraft.g4`: grammar.
- `PLC-main/CA2/code/src/main/ast/`: AST nodes.
- `PLC-main/CA2/code/src/main/symbolTable/`: declarations and symbol lookup.
- `PLC-main/CA2/code/src/main/visitor/`: AST visitors, name analysis, dependency checks.
- `PLC-main/CA2/code/samples/sample.fl`: sample input.
- `PLC-main/CA2/code/utiltiy/antlr-4.13.1-complete.jar`: bundled ANTLR tool/runtime.

## Setup and execution

Open the project in a Java IDE, mark the source directory containing the `main` package as a source root, and add the bundled ANTLR JAR to the classpath. Generate the parser from the grammar into the `parsers` package if generated sources are missing.

Run `main.FunctionCraft` with a FunctionCraft source-file path as its first argument, for example `PLC-main/CA2/code/samples/sample.fl`.

There is no Maven or Gradle build configuration in this snapshot. The repository includes the original IDE metadata; classpath setup may need adjustment on a new machine.
