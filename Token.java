import java.util.Set;
import java.util.Arrays;
import java.util.HashSet;

public class Token {

    public static Set<Character> possibleSymbols = new HashSet<Character>();
    static {
        possibleSymbols.addAll(
            Arrays.asList(
                new Character[]{
                    '{', '}',
                    '(', ')',
                    '[', ']',
                    ',', '.', ';',
                    '+','-', '*', '/',
                    '&', '|',
                    '<', '>', '=', '~'
                }
            )
        );
    }

    private TokenType tokenType;

    private KeywordType keyword;
    private char symbol;
    private String identifier;
    private int intVal;
    private String stringVal;

    public Token(String keyword){
        // The keyword given must be lowercase, but the names of the enum
        // is UPPERCASE, and I didn't want to manually change all the cases to
        // lowercase lol
        String comparableKeyword;
        if (!keyword.equals(keyword.toLowerCase())){
            comparableKeyword = "unknown";
        } else {
            comparableKeyword = keyword.toUpperCase();
        }

        switch (comparableKeyword) {
            case "CLASS":
                this.keyword = KeywordType.CLASS;
                break;
            case "METHOD":
                this.keyword = KeywordType.METHOD;
                break;
            case "FUNCTION":
                this.keyword = KeywordType.FUNCTION;
                break;
            case "CONSTRUCTOR":
                this.keyword = KeywordType.CONSTRUCTOR;
                break;
            case "INT":
                this.keyword = KeywordType.INT;
                break;
            case "BOOLEAN":
                this.keyword = KeywordType.BOOLEAN;
                break;
            case "CHAR":
                this.keyword = KeywordType.CHAR;
                break;
            case "VOID":
                this.keyword = KeywordType.VOID;
                break;
            case "VAR":
                this.keyword = KeywordType.VAR;
                break;
            case "STATIC":
                this.keyword = KeywordType.STATIC;
                break;
            case "FIELD":
                this.keyword = KeywordType.FIELD;
                break;
            case "LET":
                this.keyword = KeywordType.LET;
                break;
            case "DO":
                this.keyword = KeywordType.DO;
                break;
            case "IF":
                this.keyword = KeywordType.IF;
                break;
            case "ELSE":
                this.keyword = KeywordType.ELSE;
                break;
            case "WHILE":
                this.keyword = KeywordType.WHILE;
                break;
            case "RETURN":
                this.keyword = KeywordType.RETURN;
                break;
            case "TRUE":
                this.keyword = KeywordType.TRUE;
                break;
            case "FALSE":
                this.keyword = KeywordType.FALSE;
                break;
            case "NULL":
                this.keyword = KeywordType.NULL;
                break;
            case "THIS":
                this.keyword = KeywordType.THIS;
                break;
            default:
                throw new RuntimeException("Unknown Keyword " + keyword);
        }
        this.tokenType = TokenType.KEYWORD;
    }

    public Token(char symbol){
        this.tokenType = TokenType.SYMBOL;
        this.symbol = symbol;
    }

    public Token(String inpString, boolean identOrStr){

        if(identOrStr){
            this.tokenType = TokenType.IDENTIFIER;
            this.identifier = inpString;
        } else {
            this.tokenType = TokenType.STRING_CONST;
            this.stringVal = inpString;
        }

    }

    public Token(int intVal){
        this.tokenType = TokenType.INT_CONST;
        this.intVal = intVal;
    }

    public TokenType getTokenType(){
        return tokenType;
    }
    public KeywordType getKeyword(){
        return keyword;
    }
    public char getSymbol(){
        return symbol;
    }
    public String getIdentifier(){
        return identifier;
    }
    public int getIntVal(){
        return intVal;
    }
    public String getStringVal(){
        return stringVal;
    }

    public static String translateXML(char symbol){
        switch (symbol){
            case '<':
                return "&lt;";
            case '>':
                return "&gt;";
            case '&':
                return "&amp;";
            default:
                return String.valueOf(symbol);
        }
    }

    public String toString(){
        switch (this.tokenType){

            case KEYWORD:
                return "<keyword> " + keyword + " </keyword>";

            case SYMBOL:
                return "<symbol> " + translateXML(symbol) + " </symbol>";

            case IDENTIFIER:
                return "<identifier> " + identifier + " </identifier>";

            case INT_CONST:
                return "<integerConstant> " + intVal + " </integerConstant>";

            case STRING_CONST:
                return "<stringConstant> " + stringVal + " </stringConstant>";

            default:
                throw new RuntimeException(
                    "Unknown Token Type " + this.tokenType
                );
        }
    }

}

enum TokenType {
    KEYWORD,
    SYMBOL,
    IDENTIFIER,
    INT_CONST,
    STRING_CONST
}

enum KeywordType {
    CLASS, METHOD, FUNCTION,
    CONSTRUCTOR,
    INT, BOOLEAN, CHAR, VOID,
    VAR, STATIC, FIELD,
    LET, DO, IF, ELSE,
    WHILE, RETURN, TRUE, FALSE,
    NULL, THIS;

    @Override
    public String toString(){
        return this.name().toLowerCase();
    }
}