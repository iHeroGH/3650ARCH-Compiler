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

        compileClass();
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

            // Invalid XML
            match = xmlPattern.matcher(currToken.toString());
            if(!match.matches()){
                throw new RuntimeException(
                    "Invalid XML " + currToken.toString()
                );
            }

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

            // Invalid XML
            match = xmlPattern.matcher(currToken.toString());
            if(!match.matches()){
                throw new RuntimeException(
                    "Invalid XML " + currToken.toString()
                );
            }

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

            // Invalid XML
            match = xmlPattern.matcher(currToken.toString());
            if(!match.matches()){
                throw new RuntimeException(
                    "Invalid XML " + currToken.toString()
                );
            }

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

        compiledTokens.add(new Token(NonTerminal.CLASS, true));
    }

    private void compileClassVarDec(){

        Matcher match;
        Token currToken;
        while (compilationPointer < numTokens){
            currToken = tokens.get(compilationPointer++);

            // Invalid XML
            match = xmlPattern.matcher(currToken.toString());
            if(!match.matches()){
                throw new RuntimeException(
                    "Invalid XML " + currToken.toString()
                );
            }

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

            // Invalid XML
            match = xmlPattern.matcher(currToken.toString());
            if(!match.matches()){
                throw new RuntimeException(
                    "Invalid XML " + currToken.toString()
                );
            }

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
            // Invalid XML
            match = xmlPattern.matcher(currToken.toString());
            if(!match.matches()){
                throw new RuntimeException(
                    "Invalid XML " + currToken.toString()
                );
            }

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
                if (
                    match.group("type").equals("keyword") &
                    match.group("value").equals("var")
                ){
                    compiledTokens.add(new Token(NonTerminal.varDec, false));
                    compiledTokens.add(currToken);
                    compileVarDec();
                    compiledTokens.add(new Token(NonTerminal.varDec, true));
                }
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
            // Invalid XML
            match = xmlPattern.matcher(currToken.toString());
            if(!match.matches()){
                throw new RuntimeException(
                    "Invalid XML " + currToken.toString()
                );
            }

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

            // Invalid XML
            match = xmlPattern.matcher(currToken.toString());
            if(!match.matches()){
                throw new RuntimeException(
                    "Invalid XML " + currToken.toString()
                );
            }

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

    }

    private void compileDo(){

    }

    private void compileLet(){

    }

    private void compileWhile(){

    }

    private void compileReturn(){

    }

    private void compileIf(){

    }

    private void compileExpression(){

    }

    private void compileTerm(){

    }

    private void compileExpressionList(){

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
