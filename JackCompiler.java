public class JackCompiler {
    public static void main(String[] args){

        String filePath = "11\\Square\\Main.jack";

        JackTokenizer tokenizer = new JackTokenizer(filePath);
        new CompilationEngine(tokenizer);
    }
}
