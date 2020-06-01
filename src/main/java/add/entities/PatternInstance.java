package add.entities;

import com.github.gumtreediff.tree.ITree;
import gumtree.spoon.diff.operations.Operation;
import spoon.reflect.declaration.CtElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Matias Martinez
 */
public class PatternInstance {

    String patternName;
    Operation op = null;
    CtElement solution;
    List<CtElement> faultyStatements = new ArrayList<>();
    CtElement faultyLine;
    ITree faultyTree;
    List<PropertyPair> metadata;

    public PatternInstance(String patternName, Operation op, CtElement nodeAffectedOp, CtElement faultyElement,
                           CtElement faultyLine, ITree tree, PropertyPair... metadata) {
        super();
        this.patternName = patternName;
        this.op = op;
        this.solution = nodeAffectedOp;
        this.faultyStatements.add(faultyElement);
        this.faultyLine = faultyLine;
        this.faultyTree = tree;
        this.metadata = Arrays.asList(metadata);
    }

    public PatternInstance(String patternName, Operation op, CtElement nodeAffectedOp, List<CtElement> faultyStatements,
                           CtElement faultyLine, ITree faultyAST_Tree, PropertyPair... metadata) {
        super();
        this.patternName = patternName;
        this.op = op;
        this.solution = nodeAffectedOp;
        this.faultyStatements = faultyStatements;
        this.faultyLine = faultyLine;
        this.faultyTree = faultyAST_Tree;
        this.metadata = Arrays.asList(metadata);
    }

    public String getPatternName() {
        return patternName;
    }

    public void setPatternName(String patternName) {
        this.patternName = patternName;
    }

    public Operation getOp() {
        return op;
    }

    public void setOp(Operation op) {
        this.op = op;
    }

    public CtElement getNodeAffectedOp() {
        return solution;
    }

    public void setNodeAffectedOp(CtElement nodeAffectedOp) {
        this.solution = nodeAffectedOp;
    }

    public List<CtElement> getFaulty() {
        return faultyStatements;
    }

    public void setFautlyStatements(List<CtElement> otherNodes) {
        this.faultyStatements = otherNodes;
    }

    @Override
    public String toString() {
        return "PatternInstance [\npatternName=" + patternName + ",\n op=" + op + ",\n solution=" + solution
                + ",\n suspNodes=" + faultyStatements + "\n faulty line= " + faultyLine + "\n]" + "\n tree line= "
                + this.faultyTree + "\n]" + this.metadata + "\n]";
    }

    public CtElement getFaultyLine() {
        return faultyLine;
    }

    public void setSuspiciousElement(CtElement suspiciousElement) {
        this.faultyLine = suspiciousElement;
    }

    public ITree getFaultyTree() {
        return faultyTree;
    }

    public void setFaultyTree(ITree faultyTree) {
        this.faultyTree = faultyTree;
    }

    public List<PropertyPair> getMetadata() {
        return metadata;
    }

    public void setMetadata(List<PropertyPair> metadata) {
        this.metadata = metadata;
    }
}
