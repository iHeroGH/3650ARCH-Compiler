import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class VMWriter {

    private String filePath;
    private PrintWriter writer;

    public VMWriter(String filePath){
        this.filePath = filePath;

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
        writeCommand("push", segment.toString(), String.valueOf(index));
    }

    public void writePop(Segment segment, int index){
        writeCommand("pop", segment.toString(), String.valueOf(index));
    }

    public void writeArithmetic(Command command){
        writeCommand(command.toString());
    }

    public void writeLabel(String label){
        writeCommand("label", label);
    }

    public void writeGoto(String label){
        writeCommand("goto", label);
    }

    public void writeIf(String label){
        writeCommand("if-goto", label);
    }

    public void writeCall(String name, int numArgs){
        writeCommand("call", name, String.valueOf(numArgs));
    }

    public void writeFunction(String name, int numLocals){
        writeCommand("function", name, String.valueOf(numLocals));
    }

    public void writeReturn(){
        writeCommand("return");
    }

    public void writeComment(String comment){
        writer.write("//" + comment + "\n");
    }

    public void writeCommand(String command){
        switch(command){
            case "+":
                command = "add";
                break;
            case "-":
                command = "sub";
                break;
            case "*":
                command = "call Math.multiply 2";
                break;
            case "/":
                command = "call Math.divide 2";
                break;
            case "&amp;":
                command = "and";
                break;
            case "|":
                command = "or";
                break;
            case "&lt;":
                command = "lt";
                break;
            case "&gt;":
                command = "gt";
                break;
            case "=":
                command = "eq";
                break;
        }

        writeCommand(command, "", "");
    }

    public void writeCommand(String command, String argument){
        writeCommand(command, argument, "");
    }

    public void writeCommand(String command, String arg1, String arg2){
        // System.out.println(command + " " + arg1 + " " + arg2);
        writer.write(command + " " + arg1 + " " + arg2 + "\n");
    }

    public void close(){
        writer.close();
    }
}

enum Segment{
    CONST, ARG, LOCAL,
    STATIC, THIS, THAT, POINTER, TEMP;

    @Override
    public String toString(){
        switch(this.name()){
            case "CONST":
                return "constant";
            case "ARG":
                return "argument";
            case "LOCAL":
                return "local";
            case "STATIC":
                return "static";
            case "THIS":
                return "this";
            case "THAT":
                return "that";
            case "POINTER":
                return "pointer";
            case "TEMP":
                return "temp";
            default:
                throw new RuntimeException("Unknown Segment " + this.name());
        }
    }

    public static Segment fromIdentifer(Identifier identifier){
        switch(identifier){
            case ARG:
                return Segment.ARG;
            case FIELD:
                return Segment.THIS;
            case STATIC:
                return Segment.STATIC;
            case VAR:
                return Segment.LOCAL;
            default:
                throw new RuntimeException("Unknown Identifer " + identifier);
        }
    }
}

enum Command{
    ADD, SUB, NEG,
    EQ, GT, LT,
    AND, OR, NOT;

    @Override
    public String toString(){
        return this.name().toLowerCase();
    }
}