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

    public CompilationEngine(JackTokenizer tokenizer){
        this.tokenizer = tokenizer;
        tokens = this.tokenizer.getTokens();
        numTokens = tokens.size();

        this.compiledTokens = new ArrayList<Token>();
        this.compilationPointer = 0;

        try{
            compileClass();
        } catch (Exception e) {
            throw e;
        }
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
                compileClassVarDec();
            } else {
                break;
            }
        }

        // subroutineDec
        // = constructor|function|method void|type name ( parameterList ) subroutineBody
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
                compileSubroutine();
                break;
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

    private void compileClassVarDec(){

        while (compilationPointer < numTokens){
            compilationPointer++;
            compileVarDec();
            break;
        }

        compiledTokens.add(new Token(NonTerminal.classVarDec, true));
    }

    private void compileSubroutine(){
        Matcher match;
        Token currToken;
        boolean foundType = false;
        boolean openPar = false;
        boolean openCurl = false;
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

                    compiledTokens.add(new Token(NonTerminal.varDec, false));
                    compiledTokens.add(currToken);
                    compileVarDec();

                    currToken = tokens.get(compilationPointer++);
                    match = getMatch(currToken);

                    compiledTokens.add(new Token(NonTerminal.varDec, true));
                }
                compilationPointer--;
                compileStatements();
                compilationPointer++;
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
                    match.group("type").equals("identifier")) &
                    possibleTypes.contains(match.group("value"))
                    ){
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
                    compiledTokens.add(currToken);
                    validVariables = false;
                    foundType = false;
                    continue;
            }
        }

        compiledTokens.add(new Token(NonTerminal.parameterList, true));
        compiledTokens.add(currToken);
    }

    private void compileVarDec(){
        Matcher match;
        Token currToken;
        boolean validVariables = false;
        boolean foundType = false;
        while (compilationPointer < numTokens){
            currToken = tokens.get(compilationPointer++);
            match = getMatch(currToken);

            // Type
            if (!foundType){
                if (
                    (match.group("type").equals("keyword") |
                    match.group("type").equals("identifier")) &
                    possibleTypes.contains(match.group("value"))
                    ){
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
                throw new RuntimeException("Invalid statement keyword.");
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
                    throw new RuntimeException("Invalid statement keyword.");
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
    }

    private void compileLet(){
        Matcher match;
        Token currToken;
        boolean openIndex = false;
        boolean eqFound = false;
        while (compilationPointer < numTokens){
            // varName or varName[expression]
            if (!eqFound) compileVariableOrIndexing();
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
                if (openIndex){
                    throw new RuntimeException("Invalid indexing");
                }
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

        }
    }

    private void compileWhile(){
        compileWrappedExpression();
        compileWrappedStatements();
    }

    private void compileReturn(){
        Token currToken = tokens.get(compilationPointer);
        Matcher match = getMatch(currToken);

        boolean foundExpression = false;
        if(!match.group("value").equals(";")){
            if(!foundExpression){
                compileExpression();
            } else
                throw new RuntimeException("Unterminated");
        } else {
            compiledTokens.add(currToken);
        }
    }

    private void compileIf(){

    }

    private void compileExpression(){

        compiledTokens.add(new Token(NonTerminal.expression, false));

        Matcher match;
        Token currToken;
        currToken = tokens.get(compilationPointer);
        match = getMatch(currToken);


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

            // String or Int constant
            switch (match.group("type")){
                case "stringConstant":
                case "integerConstant":
                    compiledTokens.add(currToken);
                    return;
            }

            // Keyword Constant
            switch (match.group("value")){
                case "true":
                case "false":
                case "null":
                case "this":
                    compiledTokens.add(currToken);
                    return;
            }

            // Unary term
            if (
                match.group("type").equals("symbol") &
                (match.group("value").equals("-") |
                 match.group("value").equals("~"))
                ){

                    compiledTokens.add(currToken);
                    compileTerm();
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

            if(match.group("value").equals(".")){
                isCall = true;
                canKnow = true;
            }

            if(isCall){ // Subroutine
                compilationPointer = brIndex;
                compileSubroutineCall();
                return;
            }

            if(canKnow & !isCall){ // Variable
                compilationPointer = brIndex;
                compileVariableOrIndexing();
                compilationPointer--;
                return;
            }

            canKnow = true;
        }
    }

    private void compileExpressionList(){
        Matcher match;
        Token currToken;
        boolean validExpressions = true;
        compiledTokens.add(new Token(NonTerminal.expressionList, false));

        // Expression
        compilationPointer--;
        compileExpression();
        compilationPointer--;
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
                compileExpression();
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
    }

    private void compileSubroutineCall(){
        Matcher match;
        Token currToken;
        boolean nameFound = false;
        boolean openPar = false;

        int brIndex = compilationPointer;
        Token possibleName = null;

        // className|varName .
        while (compilationPointer < numTokens){
            currToken = tokens.get(compilationPointer++);
            match = getMatch(currToken);

            if (match.group("type").equals("identifier")){
                if(possibleName != null){
                    throw new RuntimeException("Duplicate name");
                }
                possibleName = currToken;
                continue;
            }

            if (possibleName != null){
                if (
                    match.group("type").equals("symbol") &
                    match.group("value").equals(".")
                    ){
                    compiledTokens.add(possibleName);
                    compiledTokens.add(currToken);
                    break;
                } else {
                    compilationPointer = brIndex;
                }
            }

        }

        // subroutineName(expressionList)
        while (compilationPointer < numTokens){
            currToken = tokens.get(compilationPointer++);
            match = getMatch(currToken);

            // Subroutine name
            if (match.group("type").equals("identifier")){
                if(nameFound){
                    throw new RuntimeException("Duplicate name");
                }
                compiledTokens.add(currToken);
                nameFound = true;
                continue;
            }

            // (
            if (
                match.group("type").equals("symbol") &
                match.group("value").equals("(")
                ){
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
                        compiledTokens.add(currToken);
                        break;
                }

                compileExpressionList();
                currToken = tokens.get(compilationPointer++);
                match = getMatch(currToken);
                // )
                if (
                    match.group("type").equals("symbol") &
                    match.group("value").equals(")")
                    ){
                        compiledTokens.add(currToken);
                        break;
                }
            }
        }
    }

    private void compileVariableOrIndexing(){
        Matcher match;
        Token currToken;
        boolean varNameFound = false;
        boolean openIndex = false;
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

                break;
            }

            if (varNameFound){
                break;
            }
        }
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
