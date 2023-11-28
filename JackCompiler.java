public class JackCompiler {
    public static void main(String[] args){

        String filePath = "ArrayTest\\Main.jack";

        JackTokenizer tokenizer = new JackTokenizer(filePath);
        CompilationEngine compiler = new CompilationEngine(tokenizer);

        // VMWriter vmWriter = new VMWriter(filePath, compiler);
    }

}
