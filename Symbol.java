public class Symbol implements Comparable<Symbol> {

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

    @Override
    public String toString(){
        return String.format(
            "%-17s%-17s%-17s%-17s%n",
            this.name, this.type, this.kind, this.index
        );
    }

    @Override
    public int compareTo(Symbol o) {
        int kindComp = this.kind.name().compareTo(o.kind.name());
        if (kindComp == 0){
            return this.index - o.index;
        }
        return kindComp;
    }
}
