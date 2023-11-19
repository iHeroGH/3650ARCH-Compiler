import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class JackTokenizer {

    private static Pattern masterPattern = Pattern.compile(
        "\\w+|//|/\\*\\*|\\*/|[{}()\\[\\]\\.,;=<>+\\-\\*/\\\"\\n]|."
    );

    private String filePath;
    private Scanner scanner;

    private String currentTokenString;
    private List<Token> tokens;

    public JackTokenizer(String filePath){
        this.filePath = filePath;
        initializeScanner();

        this.tokens = new ArrayList<Token>();

        readFile();
    }

    public void readFile(){
        boolean inSingleComment = false;
        boolean inMultiComment = false;
        boolean inString = false;
        String currString = "";

        Matcher match;
        String curr;
        while (hasMoreTokens()){
            advance();
            match = masterPattern.matcher(currentTokenString);
            while (match.find()){
                curr = match.group();
                // Deal with comments
                switch (curr){
                    // Multi-Line Comments
                    case "/*":
                    case "/**":
                        if (!inMultiComment)
                            inMultiComment = true;
                        else
                            throw new RuntimeException(
                                "Illegal Token " + curr
                            );
                        break;

                    case "*/":
                        if (inMultiComment){
                            inMultiComment = false;
                            continue;
                        }
                        else{
                            throw new RuntimeException(
                                "Illegal Token " + curr
                            );
                        }

                    // Single-line comments
                    case "//":
                        inSingleComment = true;
                        break;

                    case "\n":
                        if (inSingleComment){
                            inSingleComment = false;
                            continue;
                        }
                        break;
                }

                if (inSingleComment | inMultiComment){
                    continue;
                }

                // Deal with new lines
                if (curr.equals("\n")){
                    if (inString){
                        throw new RuntimeException("Unterminated String");
                    }
                    continue;
                }

                // Deal with strings
                if (curr.equals("\"")){
                    if (inString){
                        inString = false;
                        tokens.add(new Token(currString, false));
                        currString = "";
                    } else {
                        inString = true;
                    }
                    continue;
                }

                if (inString){
                    currString += curr;
                    continue;
                }

                // Deal with integers
                try{
                    int intVal = Integer.parseInt(curr);
                    tokens.add(new Token(intVal));
                    continue;
                } catch (NumberFormatException e){}

                // Deal with symbols
                if (curr.length() == 1
                        && Token.possibleSymbols.contains(curr.charAt(0))
                    ){
                    tokens.add(new Token(curr.charAt(0)));
                    continue;
                }

                // Deal with keywords
                try{
                    tokens.add(new Token(curr));
                    continue;
                } catch (RuntimeException e){}

                // Must be an identifier
                if (curr.replaceAll("\\s", "").length() == 0){
                    continue;
                }
                tokens.add(new Token(curr, true));
            }
        }
    }

    public String getFilePath(){
        return this.filePath;
    }

    public List<Token> getTokens(){
        return this.tokens;
    }

    public boolean hasMoreTokens(){
        return this.scanner.hasNext();
    }

    public void advance(){
        this.currentTokenString = this.scanner.next();
    }

    public void initializeScanner(){
        try {
            File file = new File(this.filePath);
            this.scanner = new Scanner(file);
            this.scanner.useDelimiter("\\b");
        } catch (FileNotFoundException e){
            System.out.println(
                "The provided input file (" + filePath + ") was not found."
            );
        }
    }
}


