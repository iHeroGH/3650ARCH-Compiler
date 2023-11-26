import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class JackAnalyzer {

    public static void main(String[] args){

        String filePath = "Square\\Square.jack";
        JackTokenizer tokenizer = new JackTokenizer(filePath);

        CompilationEngine compiler = new CompilationEngine(tokenizer);
        writeToFile(compiler);

    }

    public static void writeToFile(CompilationEngine compiler){
        try {
            PrintWriter writer = new PrintWriter("a.xml");
            // Write all the data from the contents list to the file
            for(Token token : compiler.getTokens()){
                writer.println(token.toString());
            }

            writer.close();
        } catch (FileNotFoundException e){
            System.out.println(
                "The provided output file (\"a.xml\") could not be created."
            );
        } catch (Exception e){ // An unexpected error
            System.out.println(
                "Something went wrong writing to the output file (\"a.xml\")."
            );
            e.printStackTrace();
        }
    }
}
