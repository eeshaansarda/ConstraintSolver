import java.util.* ;

public final class BinaryConstraint {
    private int firstVar, secondVar ;
    private ArrayList<BinaryTuple> tuples ;

    public BinaryConstraint(int fv, int sv, ArrayList<BinaryTuple> t) {
        firstVar = fv ;
        secondVar = sv ;
        tuples = t ;
    }

    public int getFV() {
        return firstVar;
    }

    public int getSV() {
        return secondVar;
    }

    public ArrayList<BinaryTuple> getTuples() {
        return tuples;
    }

    public String toString() {
        StringBuffer result = new StringBuffer() ;
        result.append("c("+firstVar+", "+secondVar+")\n") ;
        //for (BinaryTuple bt : tuples)
            //result.append(bt+"\n") ;
        return result.toString() ;
    }

    public boolean matchesVariables(int fv, int sv) {
        return (fv == firstVar && sv == secondVar) || (sv == firstVar && fv == secondVar);
    }

    public boolean isFirstVariable(int var) {
        return (firstVar == var);
    }

    public boolean isValid(int v1, int v2) {
        for(BinaryTuple tuple : tuples) {
            if(tuple.matches(v1, v2)) return true;
        }
        return false;
    }

}
