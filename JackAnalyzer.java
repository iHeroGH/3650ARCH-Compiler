import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class JackAnalyzer {

    public static void main(String[] args){

        String filePath = "ArrayTest\\Main.jack";
        JackTokenizer tokenizer = new JackTokenizer(filePath);

        writeToFile(tokenizer);

        // CompilationEngine compiler = new CompilationEngine(tokenizer);
    }

    public static void writeToFile(JackTokenizer tokenizer){
        try {
            PrintWriter writer = new PrintWriter("a.xml");
            writer.println("<tokens>");
            // Write all the data from the contents list to the file
            for(Token token : tokenizer.getTokens()){
                writer.println(token.toString());
            }
            writer.println("</tokens>");

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
