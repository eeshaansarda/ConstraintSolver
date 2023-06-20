import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.stream.IntStream;

public class Solver {

    public static void main(String args[]) {
        String inputErrMsg = "Usage: java -jar P2.jar <file.csp> <fc/mac> <sdf/asc> asc";
        if (args.length != 4) {
            System.out.println(inputErrMsg) ;
            return;
        }

        boolean algorithm = args[1].equals("fc") || args[1].equals("mac");
        boolean varorder = args[2].equals("sdf") || args[2].equals("asc");
        boolean valorder = args[3].equals("asc");

        if(!algorithm || !varorder || !valorder) {
            System.out.println(inputErrMsg);
            return;
        }

        BinaryCSPReader reader = new BinaryCSPReader() ;
        BinaryCSP csp = reader.readBinaryCSP(args[0]);

        Solver solver = new Solver(csp, args[1], args[2]);
        solver.solve();

    }

    String ALGORITHM = "fc", VARORDER = "asc";
    boolean DEBUG = false;
    BinaryCSP csp;
    int nodeCount, arcCount;

    // index is variable and value is domain/variable assignment
    // varlist is the list of unassigned variables
    ArrayList<Integer> varlist;
    ArrayList<Integer>[] domainArr;
    int[] assignmentVal;

    public Solver(BinaryCSP csp, String algorithm, String varorder) {
        this.csp = csp;
        ALGORITHM = algorithm;
        VARORDER = varorder;

        domainArr = csp.getDomainArray();
        assignmentVal = new int[csp.getNoVariables()];
        Arrays.fill(assignmentVal, -1);
        varlist = new ArrayList<>(IntStream.range(0, csp.getNoVariables()).boxed().toList());

        nodeCount = 0;
        arcCount = 0;
    }

    public void solve() {
        if(ALGORITHM.equals("fc")) {
            forwardchecking();
        } else {
            mac();
        }
    }

    void printDebug(String msg) {
        if(DEBUG) System.out.println(msg);
    }

    void printSolution() {
        System.out.println("Node count: " + nodeCount);
        System.out.println("Arc count : " + arcCount);
        System.out.println("Solution: " + Arrays.toString(assignmentVal));
        System.exit(0);
    }

    public void forwardchecking() {
        // completeassignment
        if(varlist.isEmpty()) {
            printSolution();
        }

        int var = selectVar();
        int val = Collections.min(domainArr[var]);

        printDebug("Selected var, val: " + var + ", " + val);
        printDebug("assignmentVal: " + Arrays.toString(assignmentVal));

        branchLeft(var, val);
        branchRight(var, val);
    }

    public int selectVar() {
        if(VARORDER.equals("sdf")) {
            int lowestDomain = domainArr[varlist.get(0)].size();
            int variable = varlist.get(0);
            for(int var: varlist) {
                printDebug("size: " + domainArr[var].size());
                if(lowestDomain > domainArr[var].size()) {
                    lowestDomain = domainArr[var].size();
                    variable = var;
                }
            }
            printDebug("lowestDomain: " + lowestDomain);
            return variable;
        }
        // varorder == "asc"
        return Collections.min(varlist);
    }

    // branch left is when you assign a value to the variable
    void branchLeft(int var, int val) {
        nodeCount++;
        printDebug("Branch left");

        // assign(var, val)
        assignmentVal[var] = val;
        varlist.remove((Integer) var);

        // to reverse changes made by reviseArcs
        ArrayList<Integer>[] copy = new ArrayList[domainArr.length];
        System.arraycopy(domainArr, 0, copy, 0, domainArr.length);

        if(reviseArcs(var))
            forwardchecking();

        // reverse changes made by reviseArcs
        domainArr = copy;

        //unassign(var, val);
        varlist.add(var);
        assignmentVal[var] = -1;
    }

    // branch right is when you don't assign the value to the variable
    void branchRight(int var, int val) {
        nodeCount++;
        printDebug("Branch right");

        // deleteValue(var, val);
        domainArr[var].remove((Integer) val);

        if(!domainArr[var].isEmpty()) {
            ArrayList<Integer>[] copy = new ArrayList[domainArr.length];
            System.arraycopy(domainArr, 0, copy, 0, domainArr.length);

            if(reviseArcs(var)) forwardchecking();

            // reverse changes made by reviseArcs
            domainArr = copy;
        }

        //restoreValue(var, val);
        domainArr[var].add(val);
    }

    public boolean reviseArcs(int var) {
        boolean consistent = true;
        for (int fVar: varlist) {
            if(fVar != var) {
                consistent = revise(var, fVar);
                if(!consistent) {
                    nodeCount++;
                    return false;
                }
                arcCount++;
            }
        }
        printDebug("domainArr (after var " + var + "): " + Arrays.toString(domainArr));
        return true;
    }

    public boolean revise(int present, int future) {
        // present can be greater than future therefore allow getConstraint to get that
        BinaryConstraint constraint = csp.getConstraint(present, future);

        // neccesary for sudoku because constraint doesn't exist for all variables in varlist
        if(constraint == null) return true; // return false;

        int pVal = assignmentVal[present];
        ArrayList<Integer> domain = domainArr[future];
        if(pVal == -1) return true; // when no variable is assigned (branchright)

        ArrayList<Integer> filtereddomain = new ArrayList<>();
        for (int val: domain) {
            int first = pVal;
            int second = val;

            if(present > future) {
                first = val;
                second = pVal;
            }
            if(constraint.isValid(first, second)) filtereddomain.add(val);
        }

        if(filtereddomain.isEmpty()) return false;

        domainArr[future] = filtereddomain;
        return true;
    }

    public void mac(){
        if(varlist.isEmpty()) {
            printSolution();
        }

        int var = selectVar();
        int val = Collections.min(domainArr[var]);

        printDebug("var: " + var);

        assignmentVal[var] = val;
        varlist.remove((Integer) var);

        ArrayList<Integer>[] copy = new ArrayList[domainArr.length];
        System.arraycopy(domainArr, 0, copy, 0, domainArr.length);

        if (ac3(var)) {
            //printDebug(varlist.toString());
            //printDebug("Remove: " + var);
            nodeCount++;
            mac();
        }

        domainArr = copy;

        varlist.add(var);
        assignmentVal[var] = -1;

        domainArr[var].remove((Integer) val);
        if(!domainArr[var].isEmpty()) {
            System.arraycopy(domainArr, 0, copy, 0, domainArr.length);
            if(ac3(var)) {
                nodeCount++;
                mac();
            }
            domainArr = copy;
        }
        domainArr[var].add(val);
    }

    boolean ac3(int var) {
        printDebug("ac3: " + var);
        ArrayList<BinaryConstraint> queue = csp.getVariableConstraints(var);

        //printDebug(queue.toString());
        while(!queue.isEmpty()) {
            boolean consistent = false;
            BinaryConstraint c = queue.remove(0);
            //printDebug("removed: " + c);
            //printDebug("arc(" + c.getFV() + "," + c.getSV() + ")");
            consistent = reviseDomain(c);
            if(!consistent) {
                printDebug("return f");
                return false;
            }
            ArrayList<BinaryConstraint> cs = csp.getVariableConstraints(c.getSV());
            if(!hasIntersection(cs, queue)) queue.addAll(cs);
            //printDebug(queue.toString());
        }
        printDebug("return t");
        return true;
    }

    public boolean hasIntersection(ArrayList<BinaryConstraint> list1, ArrayList<BinaryConstraint> list2) {
        ArrayList<BinaryConstraint> result = new ArrayList<>(list1);
        result.retainAll(list2);
        return !result.isEmpty();
    }

    // returns domain is empty
    boolean reviseDomain(BinaryConstraint constraint) {
        if(constraint == null) return true;

        if(assignmentVal[constraint.getFV()] != -1) {
            if(!revise(constraint.getFV(), constraint.getSV())) {
                nodeCount++;
                return false;
            }
            arcCount++;
            return true;
        }

        ArrayList<Integer> domain1 = domainArr[constraint.getFV()];
        ArrayList<Integer> domain2 = domainArr[constraint.getSV()];

        printDebug("--- fv, sv: " + constraint.getFV() + ", " + constraint.getSV());
        printDebug("--- before: " + domain2.size());

        ArrayList<Integer> filtereddomain = new ArrayList<>();
        ArrayList<BinaryTuple> filteredTuple = new ArrayList<>();
        for (BinaryTuple tuple: constraint.getTuples()) {
            // add binary tuples if fv of tuple exists in domain1
            if(domain1.contains(tuple.getVal1())) filteredTuple.add(tuple);
        }

        for (BinaryTuple tuple: filteredTuple) {
            printDebug(tuple.toString());
            if(domain2.contains(tuple.getVal2())) filtereddomain.add(tuple.getVal2());
        }

        filtereddomain = removeDuplicated(filtereddomain);

        if(filtereddomain.isEmpty()) {
            nodeCount++;
            return false;
        }

        printDebug("--- after: " + filtereddomain.size());
        domainArr[constraint.getSV()] = filtereddomain;
        arcCount++;
        return true;
    }

    ArrayList<Integer> removeDuplicated(ArrayList<Integer> list) {
        Set<Integer> set = new LinkedHashSet<>();
        set.addAll(list);
        list.clear();
        list.addAll(set);
        return list;
    }

}
