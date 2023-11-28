public class Symbol {

    private String name;
    private String type;
    private Identifier kind;
    private int index;

    public Symbol(String name, String type, Identifier kind, int index){
        this.name = name;
        this.type = type;
        this.kind = kind;
        this.index = index;
    }

    public String getName(){
        return this.name;
    }

    public String getType(){
        return this.type;
    }

    public Identifier getKind(){
        return this.kind;
    }

    public int getIndex(){
        return this.index;
    }
}
