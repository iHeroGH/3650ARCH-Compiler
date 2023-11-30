import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SymbolTable {

    private Map<String, Symbol> classScope;
    private Map<String, Symbol> subroutineScope;

    public SymbolTable(){
        classScope = new HashMap<String, Symbol>();
        subroutineScope = new HashMap<String, Symbol>();
    }

    public void startSubRoutine(){
        // if (subroutineScope.size() != 0)
            // System.out.println(subroutineScopeString());
        subroutineScope.clear();
    }

    public void define(String name, String type, Identifier kind){
        Symbol symbol = new Symbol(name, type, kind, varCount(kind));

        switch (kind){
            case STATIC:
            case FIELD:
                classScope.put(name, symbol);
                break;

            case ARG:
            case VAR:
                subroutineScope.put(name, symbol);
                break;
        }
    }

    public int varCount(Identifier kind){
        switch (kind){
            case STATIC:
            case FIELD:
                return varCount(classScope, kind);

            case ARG:
            case VAR:
                return varCount(subroutineScope, kind);

            default:
                throw new RuntimeException("Unknown kind " + kind);
        }
    }

    private int varCount(Map<String, Symbol> scope, Identifier kind){
        int count = 0;
        for(Symbol symbol : scope.values()){
            if(symbol.getKind().equals(kind)){
                count++;
            }
        }

        return count;
    }

    public Identifier kindOf(String name){
        if (subroutineScope.containsKey(name)){
            return subroutineScope.get(name).getKind();
        }
        if (classScope.containsKey(name)){
            return classScope.get(name).getKind();
        }

        return null;
    }

    public String typeOf(String name){

        if (subroutineScope.containsKey(name)){
            return subroutineScope.get(name).getType();
        }
        if (classScope.containsKey(name)){
            return classScope.get(name).getType();
        }

        return "";
    }

    public int indexOf(String name){
        if (subroutineScope.containsKey(name)){
            return subroutineScope.get(name).getIndex();
        }
        if (classScope.containsKey(name)){
            return classScope.get(name).getIndex();
        }

        throw new RuntimeException("Out-of-scope variable " + name);
    }

    @Override
    public String toString(){
        return subroutineScopeString() + "\n\n" + classScopeString();
    }

    public String classScopeString(){
        String output = "";

        output += "Class-Scope ";
        output += "---------------------------------------------\n";
        output += String.format(
            "%-17s%-17s%-17s%-17s%n",
            "Name", "Type", "Kind", "#"
        );
        output += "---------------------------------------------------------\n";
        for(Symbol symbol : sortScope(classScope)){
            output += symbol.toString() + "\n";
        }

        return output;
    }

    public String subroutineScopeString(){
        String output = "";

        output += "Method-Scope ";
        output += "---------------------------------------------\n";
        output += String.format(
            "%-17s%-17s%-17s%-17s%n",
            "Name", "Type", "Kind", "#"
        );
        output += "---------------------------------------------------------\n";
        for(Symbol symbol : sortScope(subroutineScope)){
            output += symbol.toString() + "\n";
        }

        return output;
    }

    public List<Symbol> sortScope(Map<String, Symbol> scope){
        List<Symbol> symbolList = new ArrayList<Symbol>(scope.values());
        Collections.sort(symbolList);
        return symbolList;
    }

}

enum Identifier{
    STATIC, FIELD, ARG, VAR;

    public static Identifier fromString(String strId){
        strId = strId.toLowerCase();
        switch(strId){
            case "static":
                return Identifier.STATIC;
            case "field":
                return Identifier.FIELD;
            case "var":
                return Identifier.VAR;
            default:
                return Identifier.ARG;
        }
    }
}