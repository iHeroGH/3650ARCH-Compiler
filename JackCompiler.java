public class JackCompiler {
    public static void main(String[] args){

        String filePath = "11\\Average\\Main.jack";

        JackTokenizer tokenizer = new JackTokenizer(filePath);
        CompilationEngine compiler = new CompilationEngine(tokenizer);

        // VMWriter vmWriter = new VMWriter(filePath, compiler);
    }

}
