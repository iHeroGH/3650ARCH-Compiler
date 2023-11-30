import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CompilationEngine {

    private static Pattern xmlPattern = Pattern.compile(
        // Token Type
        "<(?<type>keyword|identifier|symbol|integerConstant|stringConstant)> " +
        // Value of token
        "(?<value>.+) " +
        // Must be the same as first token type
        "<\\/\\1>"
    );

    private static List<String> possibleTypes = new ArrayList<String>(
        Arrays.asList(
            "int", "char", "boolean", "void",
            "Math", "String", "Array", "Output",
            "Screen", "Keyboard", "Memory", "Sys"
        )
    );

    private JackTokenizer tokenizer;
    private List<Token> tokens;
    private int numTokens;

    private List<Token> compiledTokens;
    private int compilationPointer;

    private String className;
    private SymbolTable symbolTable;

    private VMWriter vmWriter;
    private int labelIndex = 0;

    public CompilationEngine(JackTokenizer tokenizer){
        this.tokenizer = tokenizer;
        tokens = this.tokenizer.getTokens();
        numTokens = tokens.size();

        this.compiledTokens = new ArrayList<Token>();
        this.compilationPointer = 0;

        this.className = "";
        this.symbolTable = new SymbolTable();

        this.vmWriter = new VMWriter(tokenizer.getFilePath());

        try{
            compileClass();
            vmWriter.close();
        } catch (Exception e) {
            throw e;
        }

        // System.out.println(symbolTable);
    }

    private void compileClass(){

        Matcher match;
        boolean foundClass = false;

        if (numTokens < 3){
            throw new RuntimeException(
                "No Class Found."
            );
        }

        // class
        // = class className {
        Token currToken;
        for(; compilationPointer < 3; compilationPointer++){
            currToken = tokens.get(compilationPointer);
            match = getMatch(currToken);

            // class
            if(compilationPointer == 0){
                if (
                    match.group("type").equals("keyword") &
                    match.group("value").equals("class")
                    ){
                        compiledTokens.add(
                            new Token(NonTerminal.CLASS, false)
                        );
                        compiledTokens.add(currToken);
                } else {
                    throw new RuntimeException(
                        "No Class Found."
                    );
                }
            }

            // className
            if(compilationPointer == 1){
                if (!match.group("type").equals("identifier"))
                    throw new RuntimeException(
                        "No Class Found."
                    );
                possibleTypes.add(match.group("value"));
                className = match.group("value");
                compiledTokens.add(currToken);
            }

            // {
            if (compilationPointer == 2){
                if (
                    !match.group("type").equals("symbol") |
                    !match.group("value").equals("{")
                ){
                    throw new RuntimeException(
                        "No Class Found."
                    );
                }
                compiledTokens.add(currToken);
                foundClass = true;
            }
        }
        if (!foundClass){
            throw new RuntimeException(
                "No Class Found."
            );
        }

        compilationPointer = 3;

        // classVarDec
        // = static|field type varName (, varName)*;
        while (compilationPointer < numTokens){
            currToken = tokens.get(compilationPointer);
            match = getMatch(currToken);

            if (
                match.group("type").equals("keyword") &
                (
                match.group("value").equals("static") |
                match.group("value").equals("field")
                )
            ){
                compiledTokens.add(new Token(NonTerminal.classVarDec, false));
                compiledTokens.add(currToken);
                compileClassVarDec(Identifier.fromString(match.group("value")));
            } else {
                break;
            }
        }

        // subroutineDec
        // = constructor|function|method void|type name ( parameterList )
        // subroutineBody
        while (compilationPointer < numTokens){
            currToken = tokens.get(compilationPointer);
            match = getMatch(currToken);

            if (
                match.group("type").equals("keyword") &
                (
                match.group("value").equals("constructor") |
                match.group("value").equals("function") |
                match.group("value").equals("method")
                )
            ){
                compiledTokens.add(new Token(NonTerminal.subroutineDec, false));
                compiledTokens.add(currToken);
                compilationPointer++;
                symbolTable.startSubRoutine();

                compileSubroutine(
                    match.group("value").equals("method"),
                    match.group("value").equals("constructor")
                );
                continue;
            } else {
                break;
            }
        }

        // }
        currToken = tokens.get(compilationPointer);
        match = getMatch(currToken);
        if (match.group("value").equals("}")){
            compiledTokens.add(currToken);
            compiledTokens.add(new Token(NonTerminal.CLASS, true));
        } else{
            throw new RuntimeException("Unterminated Class");
        }
    }

    private void compileClassVarDec(Identifier kind){

        while (compilationPointer < numTokens){
            compilationPointer++;
            compileVarDec(kind);
            break;
        }

        compiledTokens.add(new Token(NonTerminal.classVarDec, true));
    }

    private void compileSubroutine(boolean isMethod, boolean isConstructor){
        Matcher match;
        Token currToken;
        boolean foundType = false;
        boolean openPar = false;
        boolean openCurl = false;
        String subroutineName = "";
        while (compilationPointer < numTokens){
            currToken = tokens.get(compilationPointer++);
            match = getMatch(currToken);

            // Void or type
            if(!foundType){
                if (
                    (match.group("type").equals("keyword") |
                    match.group("type").equals("identifier")) &
                    possibleTypes.contains(match.group("value"))
                ){
                    compiledTokens.add(currToken);
                    foundType = true;
                    continue;
                }
            }

            // Name
            if (match.group("type").equals("identifier")){
                if(!foundType){
                    throw new RuntimeException("No type declared.");
                }

                if(isMethod){
                    this.symbolTable.define(
                        "this", className, Identifier.ARG
                    );
                }

                subroutineName = match.group("value");
                compiledTokens.add(currToken);
                continue;
            }

            // (
            if (
                match.group("type").equals("symbol") &
                match.group("value").equals("(")
                ){
                    if (openPar){
                        throw new RuntimeException("Hanging (");
                    }
                    openPar = true;
                    compiledTokens.add(currToken);
            }

            // Parameter list
            // = type name, type name*
            compileParameterList();
            break;
        }

        // Body
        while (compilationPointer < numTokens){
            currToken = tokens.get(compilationPointer++);
            match = getMatch(currToken);

            // {
            if (
                match.group("type").equals("symbol") &
                match.group("value").equals("{")
                ){
                    if (openCurl){
                        throw new RuntimeException("Hanging {");
                    }
                    openCurl = true;
                    compiledTokens.add(new Token(NonTerminal.subroutineBody, false));
                    compiledTokens.add(currToken);
                    continue;
            }


            // subroutineBody
            // = varDec* statements
            // = var type name (, name)*;
            // = statements...
            if(openCurl){
                while (
                    match.group("type").equals("keyword") &
                    match.group("value").equals("var")
                    ){
                        compiledTokens.add(
                            new Token(NonTerminal.varDec, false)
                        );
                        compiledTokens.add(currToken);
                        compileVarDec(Identifier.fromString("var"));

                        currToken = tokens.get(compilationPointer++);
                        match = getMatch(currToken);

                        compiledTokens.add(
                            new Token(NonTerminal.varDec, true)
                        );
                }

                vmWriter.writeFunction(
                    className + "." + subroutineName,
                    symbolTable.varCount(Identifier.VAR)
                );

                if (isMethod){
                    vmWriter.writePush(Segment.ARG, 0);
                    vmWriter.writePush(Segment.POINTER, 0);
                }

                if (isConstructor){
                    vmWriter.writePush(
                        Segment.CONST, symbolTable.varCount(Identifier.FIELD)
                    );
                    vmWriter.writeCall("Memory.alloc", 1);
                    vmWriter.writePop(Segment.POINTER, 0);
                }

                compilationPointer--;
                compileStatements();
                currToken = tokens.get(compilationPointer++);
                match = getMatch(currToken);
            }

            // }
            if (
                match.group("type").equals("symbol") &
                match.group("value").equals("}")
                ){
                    if (!openCurl){
                        throw new RuntimeException("Hanging }");
                    }
                    compiledTokens.add(currToken);
                    break;
            }

        }

        compiledTokens.add(new Token(NonTerminal.subroutineBody, true));
        compiledTokens.add(new Token(NonTerminal.subroutineDec, true));
    }

    private void compileParameterList(){
        Matcher match;
        Token currToken = null;
        boolean validVariables = true;
        boolean foundType = false;

        String type = "";
        String varName = "";

        compiledTokens.add(new Token(NonTerminal.parameterList, false));
        while (compilationPointer < numTokens){
            currToken = tokens.get(compilationPointer++);
            match = getMatch(currToken);

            // )
            if (
                match.group("type").equals("symbol") &
                match.group("value").equals(")")
                ){
                    if (!validVariables){
                        throw new RuntimeException("Invalid variables.");
                    }
                    break;
            }

            // Type
            if (!foundType){
                if (
                    (match.group("type").equals("keyword") |
                    match.group("type").equals("identifier"))
                    ){
                        type = match.group("value");
                        compiledTokens.add(currToken);
                        validVariables = false;
                        foundType = true;
                        continue;
                } else {
                    throw new RuntimeException(
                        "Unknown type " + match.group("value")
                    );
                }
            }

            // Variable Name
            if (match.group("type").equals("identifier")){
                if(!foundType){
                    throw new RuntimeException("No type declared.");
                }
                    varName = match.group("value");
                    compiledTokens.add(currToken);
                    validVariables = true;
                    continue;
            }

            // ,
            if (
                match.group("type").equals("symbol") &
                match.group("value").equals(",")
                ){
                    if (!validVariables){
                        throw new RuntimeException("Hanging comma.");
                    }
                    this.symbolTable.define(varName, type, Identifier.ARG);
                    type = "";
                    varName = "";
                    compiledTokens.add(currToken);
                    validVariables = false;
                    foundType = false;
                    continue;
            }
        }

        if (!type.equals("") & !varName.equals("")){
            this.symbolTable.define(varName, type, Identifier.ARG);
        }

        compiledTokens.add(new Token(NonTerminal.parameterList, true));
        compiledTokens.add(currToken);
    }

    private void compileVarDec(Identifier kind){
        Matcher match;
        Token currToken;
        boolean validVariables = false;
        boolean foundType = false;

        String type = "";
        List<String> varNames = new ArrayList<String>();

        while (compilationPointer < numTokens){
            currToken = tokens.get(compilationPointer++);
            match = getMatch(currToken);

            // Type
            if (!foundType){
                if (
                    (match.group("type").equals("keyword") |
                    match.group("type").equals("identifier"))
                    ){
                        type = match.group("value");
                        compiledTokens.add(currToken);
                        foundType = true;
                        continue;
                } else {
                    throw new RuntimeException(
                        "Unknown type " + match.group("value")
                    );
                }
            }

            // Variable Name
            if (match.group("type").equals("identifier")){
                if(!foundType){
                    throw new RuntimeException("No type declared.");
                }
                varNames.add(match.group("value"));
                compiledTokens.add(currToken);
                validVariables = true;
                continue;
            }

            // ,
            if (
                match.group("type").equals("symbol") &
                match.group("value").equals(",")
                ){
                    if (!foundType){
                        throw new RuntimeException("No type declared.");
                    }
                    if (!validVariables){
                        throw new RuntimeException("Hanging comma.");
                    }
                    compiledTokens.add(currToken);
                    validVariables = false;
                    continue;
            }

            // ;
            if (
                match.group("type").equals("symbol") &
                match.group("value").equals(";")
                ){
                    if (!foundType){
                        throw new RuntimeException("No type declared.");
                    }
                    if (!validVariables){
                        throw new RuntimeException("Invalid variables.");
                    }
                    compiledTokens.add(currToken);

                    for(String varName : varNames){
                        this.symbolTable.define(varName, type, kind);
                    }

                    break;
            }
        }
    }

    private void compileStatements(){
        Matcher match;
        Token currToken;
        compiledTokens.add(new Token(NonTerminal.statements, false));
        while (compilationPointer < numTokens){
            currToken = tokens.get(compilationPointer++);
            match = getMatch(currToken);

            if (match.group("type").equals("symbol")){
                compilationPointer--;
                break;
            }

            // Invalid statement
            if (!match.group("type").equals("keyword")){
                throw new RuntimeException(
                        "Invalid statement keyword " + match.group("value")
                    );
            }

            switch (match.group("value")){
                case "let":
                    compiledTokens.add(new Token(NonTerminal.letStatement, false));
                    compiledTokens.add(currToken);
                    compileLet();
                    compiledTokens.add(new Token(NonTerminal.letStatement, true));
                    break;
                case "if":
                    compiledTokens.add(new Token(NonTerminal.ifStatement, false));
                    compiledTokens.add(currToken);
                    compileIf();
                    compiledTokens.add(new Token(NonTerminal.ifStatement, true));
                    break;
                case "while":
                    compiledTokens.add(new Token(NonTerminal.whileStatement, false));
                    compiledTokens.add(currToken);
                    compileWhile();
                    compiledTokens.add(new Token(NonTerminal.whileStatement, true));
                    break;
                case "do":
                    compiledTokens.add(new Token(NonTerminal.doStatement, false));
                    compiledTokens.add(currToken);
                    compileDo();
                    compiledTokens.add(new Token(NonTerminal.doStatement, true));
                    break;
                case "return":
                    compiledTokens.add(new Token(NonTerminal.returnStatement, false));
                    compiledTokens.add(currToken);
                    compileReturn();
                    compiledTokens.add(new Token(NonTerminal.returnStatement, true));
                    break;
                default:
                    throw new RuntimeException(
                        "Invalid statement keyword " + match.group("value")
                    );
            }
        }

        compiledTokens.add(new Token(NonTerminal.statements, true));
    }

    private void compileDo(){
        compileSubroutineCall();
        Token currToken = tokens.get(compilationPointer++);
        Matcher match = getMatch(currToken);

        if(!match.group("value").equals(";")){
            throw new RuntimeException("Unterminated");
        } else {
            compiledTokens.add(currToken);
        }

        vmWriter.writePop(Segment.TEMP, 0);
    }

    private void compileLet(){
        Matcher match;
        Token currToken;
        boolean eqFound = false;
        String varName = "";
        boolean isIndex = false;
        String[] varIdentifier = new String[2];
        while (compilationPointer < numTokens){
            // varName or varName[expression]
            if (!eqFound) varIdentifier = compileVariableOrIndexing(true);
            varName = varIdentifier[0];
            isIndex = Boolean.valueOf(varIdentifier[1]);
            compilationPointer--;
            currToken = tokens.get(compilationPointer++);
            match = getMatch(currToken);

            // =
            if (
                match.group("type").equals("symbol") &
                match.group("value").equals("=")
                ){
                    if (eqFound){
                        throw new RuntimeException("Duplicate =");
                    }
                    compiledTokens.add(currToken);
                    eqFound = true;
            }

            // expression;
            if (eqFound){
                compileExpression();
                compilationPointer--;
                currToken = tokens.get(compilationPointer++);
                match = getMatch(currToken);
                if (
                    !match.group("type").equals("symbol") |
                    !match.group("value").equals(";")
                    ){
                        throw new RuntimeException("Unterminated Stmt");
                } else {
                    compiledTokens.add(currToken);
                    break;
                }
            }

            if (isIndex){
                vmWriter.writePop(Segment.TEMP, 0);
                vmWriter.writePop(Segment.POINTER, 1);
                vmWriter.writePush(Segment.TEMP, 0);
                vmWriter.writePop(Segment.THAT, 0);
            } else {
                vmWriter.writePop(
                    Segment.fromIdentifer(symbolTable.kindOf(varName)),
                    symbolTable.indexOf(varName)
                );
            }
        }

        if (isIndex){
            vmWriter.writePop(Segment.TEMP, 0);
            vmWriter.writePop(Segment.POINTER, 1);
            vmWriter.writePush(Segment.TEMP, 0);
            vmWriter.writePop(Segment.THAT, 0);
        } else {
            vmWriter.writePop(
                Segment.fromIdentifer(symbolTable.kindOf(varName)),
                symbolTable.indexOf(varName)
            );
        }
    }

    private void compileWhile(){
        String continueLabel = generateLabel();
        String topLabel = generateLabel();

        vmWriter.writeLabel(topLabel);

        compileWrappedExpression();

        vmWriter.writeArithmetic(Command.NOT);
        vmWriter.writeIf(continueLabel);

        compileWrappedStatements();

        vmWriter.writeGoto(topLabel);
        vmWriter.writeLabel(continueLabel);
    }

    private void compileReturn(){
        Token currToken = tokens.get(compilationPointer);
        Matcher match = getMatch(currToken);

        if(!match.group("value").equals(";")){
            compileExpression();
            compilationPointer--;
            currToken = tokens.get(compilationPointer);
            match = getMatch(currToken);
        }

        if(match.group("value").equals(";")){
            vmWriter.writePush(Segment.CONST, 0);
            compiledTokens.add(currToken);
            compilationPointer++;
        }

        vmWriter.writeReturn();
    }

    private void compileIf(){

        String elseLabel = generateLabel();
        String endLabel = generateLabel();

        compileWrappedExpression();

        vmWriter.writeArithmetic(Command.NOT);
        vmWriter.writeIf(elseLabel);

        compileWrappedStatements();

        vmWriter.writeGoto(endLabel);
        vmWriter.writeLabel(elseLabel);

        Token currToken = tokens.get(compilationPointer);
        Matcher match = getMatch(currToken);

        if (
            match.group("type").equals("keyword") &
            match.group("value").equals("else")
            ){
                compiledTokens.add(currToken);
                compilationPointer++;
                compileWrappedStatements();
        }

        vmWriter.writeLabel(endLabel);
    }

    private void compileExpression(){

        compiledTokens.add(new Token(NonTerminal.expression, false));

        Token currToken = tokens.get(compilationPointer);
        Matcher match = getMatch(currToken);

        compileTerm();
        compiledTokens.add(new Token(NonTerminal.term, true));

        boolean operator = false;
        while (compilationPointer < numTokens){
            currToken = tokens.get(compilationPointer++);
            match = getMatch(currToken);
            if (match.group("type").equals("symbol")){
                switch(match.group("value")){
                    case "+":
                    case "-":
                    case "*":
                    case "/":
                    case "&amp;":
                    case "|":
                    case "&lt;":
                    case "&gt;":
                    case "=":
                        compiledTokens.add(currToken);
                        operator = true;
                        break;

                    default:
                        break;
                }
            }

            if (operator){
                compileTerm();

                vmWriter.writeCommand(match.group("value"));

                compiledTokens.add(new Token(NonTerminal.term, true));
                operator = false;
            } else {
                break;
            }

        }

        compiledTokens.add(new Token(NonTerminal.expression, true));
    }

    private void compileTerm(){
        compiledTokens.add(new Token(NonTerminal.term, false));

        Matcher match;
        Token currToken;
        int brIndex = compilationPointer;
        boolean isCall = false;
        boolean canKnow = false;
        while (compilationPointer < numTokens){
            currToken = tokens.get(compilationPointer++);
            match = getMatch(currToken);

            if(
                match.group("value").equals(".") |
                match.group("value").equals("(")
                ){
                    if (canKnow){
                        isCall = true;
                    }
            }

            if(canKnow & isCall){ // Subroutine
                compilationPointer = brIndex;
                compileSubroutineCall();
                return;
            }

            if(canKnow & !isCall){ // Variable
                compilationPointer = brIndex;
                String[] varIdentifier = compileVariableOrIndexing(false);
                String varName = varIdentifier[0];
                boolean isIndex = Boolean.valueOf(varIdentifier[1]);

                if(isIndex){
                    vmWriter.writePop(Segment.POINTER, 1);
                    vmWriter.writePush(Segment.THAT, 0);
                }else {
                    vmWriter.writePush(
                        Segment.fromIdentifer(symbolTable.kindOf(varName)),
                        symbolTable.indexOf(varName));
                }

                compilationPointer--;
                return;
            }

            if(canKnow){
                compilationPointer--;
                currToken = tokens.get(compilationPointer++);
                match = getMatch(currToken);
            }

            // String or Int constant
            switch (match.group("type")){
                case "stringConstant":
                    String strVal = currToken.getStringVal();
                    vmWriter.writePush(
                        Segment.CONST,
                        strVal.length()
                    );
                    vmWriter.writeCall("String.new", 1);
                    compiledTokens.add(currToken);

                    for(int i = 0; i < strVal.length(); i++){
                        vmWriter.writePush(
                            Segment.CONST, (int) strVal.charAt(i)
                        );
                        vmWriter.writeCall("String.appendChar", 2);
                    }
                    return;

                case "integerConstant":
                    vmWriter.writePush(Segment.CONST, currToken.getIntVal());
                    compiledTokens.add(currToken);
                    return;
            }

            // Keyword Constant
            switch (match.group("value")){
                case "true":
                    vmWriter.writePush(Segment.CONST, 0);
                    vmWriter.writeArithmetic(Command.NOT);
                    compiledTokens.add(currToken);
                    return;

                case "false":
                case "null":
                    vmWriter.writePush(Segment.CONST, 0);
                    compiledTokens.add(currToken);
                    return;

                case "this":
                    vmWriter.writePush(Segment.CONST, 0);
                    compiledTokens.add(currToken);
                    return;
            }

            // Unary term
            if (
                match.group("type").equals("symbol") &
                (match.group("value").equals("-") |
                 match.group("value").equals("~"))
                ){
                    compileTerm();

                    switch (match.group("value")){
                        case "-":
                            vmWriter.writeArithmetic(Command.NEG);
                            break;
                        case "~":
                            vmWriter.writeArithmetic(Command.NOT);
                            break;
                    }

                    compiledTokens.add(currToken);
                    compiledTokens.add(new Token(NonTerminal.term, true));
                    return;
            }

            // (
            else if (
                match.group("type").equals("symbol") &
                match.group("value").equals("(")
                ){
                    compilationPointer--;
                    compileWrappedExpression();
                    return;
            }

            canKnow = true;

        }
    }

    private int compileExpressionList(){
        Matcher match;
        Token currToken;
        boolean validExpressions = true;
        compiledTokens.add(new Token(NonTerminal.expressionList, false));

        // Expression
        compilationPointer--;
        compileExpression();
        compilationPointer--;
        int numArgs = 0;
        while (compilationPointer < numTokens){

            currToken = tokens.get(compilationPointer++);
            match = getMatch(currToken);

            // ,
            if (
                match.group("type").equals("symbol") &
                match.group("value").equals(",")
                ){
                    if (!validExpressions){
                        throw new RuntimeException("Hanging comma.");
                    }
                    compiledTokens.add(currToken);
                    validExpressions = false;
                    continue;
            }

            if (!validExpressions){
                compilationPointer--;
                compileExpression();
                numArgs++;
                compilationPointer--;
                validExpressions = true;
                continue;
            }

            // )
            if (
                match.group("type").equals("symbol") &
                match.group("value").equals(")")
                ){
                    if (!validExpressions){
                        throw new RuntimeException("Invalid expressions.");
                    }
                    compilationPointer--;
                    break;
            }
        }
        compiledTokens.add(new Token(NonTerminal.expressionList, true));

        return numArgs;
    }

    private void compileSubroutineCall(){
        Matcher match;
        Token currToken;
        boolean nameFound = false;
        boolean openPar = false;

        int brIndex = compilationPointer;
        Token possibleName = null;
        boolean usedPossible = false;

        int numArgs = 0;

        // className|varName .
        while (compilationPointer < numTokens){
            currToken = tokens.get(compilationPointer++);
            match = getMatch(currToken);

            // name
            if (match.group("type").equals("identifier")){
                if(possibleName != null){
                    throw new RuntimeException("Duplicate name");
                }
                possibleName = currToken;
                continue;
            }

            // .
            if (possibleName != null){
                if (
                    match.group("type").equals("symbol") &
                    match.group("value").equals(".")
                    ){
                        usedPossible = true;
                        compiledTokens.add(possibleName);
                        compiledTokens.add(currToken);
                        break;
                } else {
                    compilationPointer = brIndex;
                    break;
                }
            }
        }

        String subroutineName = "";
        // subroutineName(expressionList)
        while (compilationPointer < numTokens){
            currToken = tokens.get(compilationPointer++);
            match = getMatch(currToken);

            // Subroutine name
            if (match.group("type").equals("identifier")){
                if(nameFound){
                    throw new RuntimeException("Duplicate name");
                }
                subroutineName = match.group("value");
                compiledTokens.add(currToken);
                nameFound = true;
                continue;
            }

            // (
            if (
                match.group("type").equals("symbol") &
                match.group("value").equals("(")
                ){
                    if (!usedPossible){
                        vmWriter.writePush(Segment.POINTER, 0);
                    } else {
                        String nameIdent = possibleName.getIdentifier();
                        if (!symbolTable.typeOf(nameIdent).equals("")){
                            vmWriter.writePush(
                                Segment.fromIdentifer(symbolTable.kindOf(
                                    nameIdent
                                )),
                                symbolTable.indexOf(nameIdent)
                            );
                            possibleName = new Token(
                                symbolTable.typeOf(nameIdent), true
                            );
                            numArgs++;
                        }
                    }
                    compiledTokens.add(currToken);
                    openPar = true;
            }

            if (openPar){
                if (!nameFound){
                        throw new RuntimeException("No name");
                    }
                currToken = tokens.get(compilationPointer++);
                match = getMatch(currToken);
                // )
                if (
                    match.group("type").equals("symbol") &
                    match.group("value").equals(")")
                    ){
                        compiledTokens.add(
                            new Token(NonTerminal.expressionList, false)
                        );
                        compiledTokens.add(
                            new Token(NonTerminal.expressionList, true)
                        );

                        if(possibleName != null & usedPossible){
                            vmWriter.writeCall(
                                possibleName.getIdentifier() + "." +
                                subroutineName, numArgs
                            );
                        } else {
                            vmWriter.writeCall(
                                className + "." + subroutineName, numArgs
                            );
                        }

                        compiledTokens.add(currToken);
                        break;
                }

                numArgs += compileExpressionList();
                currToken = tokens.get(compilationPointer++);
                match = getMatch(currToken);
                // )
                if (
                    match.group("type").equals("symbol") &
                    match.group("value").equals(")")
                    ){
                        if(possibleName != null){
                            vmWriter.writeCall(
                                possibleName.getIdentifier() + "." +
                                subroutineName, numArgs
                            );
                        } else {
                            vmWriter.writeCall(
                                className + "." + subroutineName, numArgs
                            );
                        }
                        compiledTokens.add(currToken);
                        break;
                }

            }
        }
    }

    private String[] compileVariableOrIndexing(boolean isLet){
        Matcher match;
        Token currToken;
        boolean varNameFound = false;
        boolean openIndex = false;
        boolean isIndex = false;
        String varName = "";
        while (compilationPointer < numTokens){
            currToken = tokens.get(compilationPointer++);
            match = getMatch(currToken);

            // Variable Name
            if (match.group("type").equals("identifier")){

                if (varNameFound){
                    throw new RuntimeException("Duplicate name");
                }

                compiledTokens.add(currToken);
                varNameFound = true;
                varName = match.group("value");

                continue;
            }

            // [expression]
            // [
            if (
                match.group("type").equals("symbol") &
                match.group("value").equals("[")
                ){

                if (!varNameFound){
                    throw new RuntimeException("No variable to index");
                }
                if (openIndex){
                    throw new RuntimeException("Hanging [");
                }

                compiledTokens.add(currToken);
                openIndex = true;
            }

            // expression]
            if (openIndex){

                if (!varNameFound){
                    throw new RuntimeException("No variable to index");
                }

                vmWriter.writePush(
                    Segment.fromIdentifer(symbolTable.kindOf(varName)),
                    symbolTable.indexOf(varName)
                );

                compileExpression();
                compilationPointer--;

                currToken = tokens.get(compilationPointer++);
                match = getMatch(currToken);

                if (
                    !match.group("type").equals("symbol") |
                    !match.group("value").equals("]")
                    ){
                        throw new RuntimeException("Hanging [");
                } else {
                    compiledTokens.add(currToken);
                    compilationPointer++;
                    openIndex = false;
                }

                vmWriter.writeArithmetic(Command.ADD);

                isIndex = true;

                break;
            }

            if (varNameFound){
                break;
            }
        }

        return new String[]{varName, String.valueOf(isIndex)};
    }

    private void compileWrappedExpression(){
        Matcher match;
        Token currToken;
        boolean openPar = false;
        while (compilationPointer < numTokens){
            currToken = tokens.get(compilationPointer++);
            match = getMatch(currToken);

            // (
            if (
                match.group("type").equals("symbol") &
                match.group("value").equals("(")
                ){
                    if (openPar){
                        throw new RuntimeException("Hanging (");
                    }
                    openPar = true;
                    compiledTokens.add(currToken);
            }

            // expression
            if(openPar){
                compileExpression();
                compilationPointer--;
                currToken = tokens.get(compilationPointer++);
                match = getMatch(currToken);

                // )
                if (
                    match.group("type").equals("symbol") &
                    match.group("value").equals(")")
                    ){
                        if (!openPar){
                            throw new RuntimeException("Hanging )");
                        }
                        compiledTokens.add(currToken);
                        break;
                }
            }
        }
    }

    private void compileWrappedStatements(){
        Matcher match;
        Token currToken;
        boolean openCurl = false;
        while (compilationPointer < numTokens){
            currToken = tokens.get(compilationPointer++);
            match = getMatch(currToken);

            // {
            if (
                match.group("type").equals("symbol") &
                match.group("value").equals("{")
                ){
                    if (openCurl){
                        throw new RuntimeException("Hanging {");
                    }
                    openCurl = true;
                    compiledTokens.add(currToken);
            }


            // statements
            if(openCurl){
                compileStatements();
                currToken = tokens.get(compilationPointer++);
                match = getMatch(currToken);
            }

            // }
            if (
                match.group("type").equals("symbol") &
                match.group("value").equals("}")
                ){
                    if (!openCurl){
                        throw new RuntimeException("Hanging }");
                    }
                    compiledTokens.add(currToken);
                    break;
            }

        }
    }

    private Matcher getMatch(Token currToken){
        Matcher match = xmlPattern.matcher(currToken.toString());
        if(!match.matches()){
            throw new RuntimeException(
                "Invalid XML " + currToken.toString()
            );
        }
        return match;
    }

    private String generateLabel(){
        return "LABEL_" + (labelIndex++);
    }

    public List<Token> getTokens(){
        return this.compiledTokens;
    }
}

enum NonTerminal{
    CLASS, classVarDec,
    subroutineDec, parameterList, subroutineBody, varDec,
    statements, whileStatement, ifStatement, returnStatement,
    letStatement, doStatement,
    expression, term, expressionList;

    @Override
    public String toString(){
        if (this.name().equals("CLASS")){
            return "class";
        }
        return this.name();
    }
}
