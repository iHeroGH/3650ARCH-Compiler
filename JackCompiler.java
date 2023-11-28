public class JackCompiler {
    public static void main(String[] args){

        String filePath = "Square\\Square.jack";

        JackTokenizer tokenizer = new JackTokenizer(filePath);
        new CompilationEngine(tokenizer);
    }
}
