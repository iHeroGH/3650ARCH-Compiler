import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class VMWriter {

    private String filePath;
    private CompilationEngine compiler;
    private PrintWriter writer;

    public VMWriter(String filePath, CompilationEngine compiler){
        this.filePath = filePath;
        this.compiler = compiler;

        String outputPath = this.filePath.replaceAll(
            "\\.jack", ".vm"
        );
        outputPath = "a.vm"; // TODO

        try{
            writer = new PrintWriter(outputPath);
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

    public void writePush(Segment segment, int index){

    }

    public void writePop(Segment segment, int index){

    }

    public void writeArithmetic(Command command){

    }

    public void writeLabel(String label){

    }

    public void writeGoto(String label){

    }

    public void writeIf(String label){

    }

    public void writeCall(String name, int numArgs){

    }

    public void writeFunction(String name, int numLocals){

    }

    public void writeReturn(){

    }

    public void close(){
        writer.close();
    }
}

enum Segment{
    CONST, ARG, LOCAL,
    STATIC, THIS, THAT, POINTER, TEMP;
}

enum Command{
    ADD, SUB, NEG,
    EQ, GT, LT,
    AND, OR, NOT;
}