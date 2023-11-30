import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class __JackAnalyzer {

    public static void main(String[] args){

        String filePath = "Square\\Square.jack";
        JackTokenizer tokenizer = new JackTokenizer(filePath);

        CompilationEngine compiler = new CompilationEngine(tokenizer);
        writeToFile(compiler, filePath);

    }

    public static void writeToFile(CompilationEngine compiler, String filePath){
        String outputPath = filePath.replaceAll(
            "\\.jack", ".gen.xml"
        );
        try {
            PrintWriter writer = new PrintWriter(outputPath);
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
