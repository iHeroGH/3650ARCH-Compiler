import java.util.HashMap;
import java.util.Map;

public class SymbolTable {

    private Map<String, Symbol> classScope;
    private Map<String, Symbol> subroutineScope;

    public SymbolTable(){
        classScope = new HashMap<String, Symbol>();
        subroutineScope = new HashMap<String, Symbol>();
    }

    public void startSubRoutine(){
        subroutineScope.clear();
    }

    public void define(String name, String type, Identifier kind){
        Symbol symbol = new Symbol(name, type, kind, varCount(kind) + 1);

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

        throw new RuntimeException("Out-of-scope variable " + name);
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

}

enum Identifier{
    STATIC, FIELD, ARG, VAR;
}