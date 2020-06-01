package add.features.detector.repairpatterns;

import add.entities.PatternInstance;
import add.entities.PropertyPair;
import add.entities.RepairPatterns;
import add.features.detector.spoon.MappingAnalysis;
import add.features.detector.spoon.RepairPatternUtils;
import com.github.gumtreediff.tree.ITree;
import gumtree.spoon.diff.operations.DeleteOperation;
import gumtree.spoon.diff.operations.InsertOperation;
import gumtree.spoon.diff.operations.Operation;
import gumtree.spoon.diff.operations.UpdateOperation;
import spoon.reflect.code.CtArrayAccess;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtTypeAccess;
import spoon.reflect.code.CtVariableAccess;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.visitor.filter.LineFilter;

import java.util.List;

/**
 * Created by tdurieux
 */
public class ConstantChangeDetector extends AbstractPatternDetector {

    public static final String CONST_CHANGE = "constChange";

    public ConstantChangeDetector(List<Operation> operations) {
        super(operations);
    }

    @Override
    public void detect(RepairPatterns repairPatterns) {
        for (int i = 0; i < operations.size(); i++) {
            Operation operation = operations.get(i);
            if ((operation instanceof UpdateOperation)) {
                CtElement srcNode = operation.getSrcNode();
                if (operation.getSrcNode().getParent().getMetadata("new") != null ||
                        operation.getSrcNode().getParent().getMetadata("isMoved") != null) {
                    continue;
                }
                CtElement parent = MappingAnalysis.getParentLine(new LineFilter(), srcNode);
                ITree lineTree = MappingAnalysis.getFormattedTreeFromControlFlow(parent);

                if (srcNode instanceof CtLiteral) {
                    repairPatterns.incrementFeatureCounterInstance(CONST_CHANGE,
                            new PatternInstance(CONST_CHANGE, operation, operation.getDstNode(), srcNode, parent,
                                    lineTree, new PropertyPair("type", "literal")));
                }
                // we consider constChange_varaccess as variable related transform
//                if (srcNode instanceof CtVariableAccess
//                        && RepairPatternUtils.isConstantVariableAccess((CtVariableAccess) srcNode)) {
//                    repairPatterns.incrementFeatureCounter(CONST_CHANGE, operation);
//                    repairPatterns.incrementFeatureCounterInstance(CONST_CHANGE,
//                            new PatternInstance(CONST_CHANGE, operation, operation.getDstNode(), srcNode, parent,
//                                    lineTree, new PropertyPair("type", "varaccess")));
//                }
                // required for closure14
                if (srcNode instanceof CtTypeAccess && RepairPatternUtils.isConstantTypeAccess((CtTypeAccess) srcNode)) {
                    if (RepairPatternUtils.isConstantTypeAccess((CtTypeAccess) srcNode) &&
                            !RepairPatternUtils.isThisAccess((CtTypeAccess) srcNode)) {
                        CtVariableRead parentVarRead = srcNode.getParent(CtVariableRead.class);
                        // The change is not inside a VariableRead (which ast has as node a TypeAccess)
                        if (parentVarRead == null) {
                            repairPatterns.incrementFeatureCounterInstance(CONST_CHANGE,
                                    new PatternInstance(CONST_CHANGE, operation, operation.getDstNode(), srcNode, parent,
                                            lineTree, new PropertyPair("type", "typeaccess")));
                        }
                    }
                }
            } else {
//                if (operation instanceof DeleteOperation && operation.getSrcNode() instanceof CtLiteral) {
//                    CtLiteral ctLiteral = (CtLiteral) operation.getSrcNode();
//                    // try to search a replacement for the literal
//                    for (int j = 0; j < operations.size(); j++) {
//                        Operation operation2Insert = operations.get(j);
//                        if (operation2Insert instanceof InsertOperation) {
//                            CtElement ctElement = operation2Insert.getSrcNode();
//                            boolean isConstantVariable = false;
//                            if ((ctElement instanceof CtVariableAccess
//                                    && RepairPatternUtils.isConstantVariableAccess((CtVariableAccess) ctElement))
//                                    || (ctElement instanceof CtTypeAccess
//                                            && RepairPatternUtils.isConstantTypeAccess((CtTypeAccess) ctElement))) {
//                                isConstantVariable = true;
//                            }
//                            if (((InsertOperation) operation2Insert).getParent() == ctLiteral.getParent()
//                                    && isConstantVariable) {
//                                CtElement parent = MappingAnalysis.getParentLine(new LineFilter(), ctLiteral);
//                                ITree lineTree = MappingAnalysis.getFormatedTreeFromControlFlow(parent);
//
//                                repairPatterns.incrementFeatureCounterInstance(CONST_CHANGE,
//                                        new PatternInstance(CONST_CHANGE, operation2Insert,
//                                                operation2Insert.getSrcNode(), ctLiteral, parent, lineTree,
//                                                new PropertyPair("type", "literal_by_varaccess")));
//                            }
//                        }
//                    }
//                }

                // literal replaced by variable or type access (note enum type will be deemed as type access for partial program
                if (operation instanceof DeleteOperation && operation.getSrcNode() instanceof CtLiteral) {
                    CtLiteral ctLiteral = (CtLiteral) operation.getSrcNode();
                    // try to search a replacement for the literal
                    for (int j = 0; j < operations.size(); j++) {
                        Operation operation2Insert = operations.get(j);
                        if (operation2Insert instanceof InsertOperation) {
                            CtElement ctElement = operation2Insert.getSrcNode();
                            boolean isConstantVariable = false;
                            if (ctElement instanceof CtVariableAccess || ctElement instanceof CtArrayAccess ||
                                    (ctElement instanceof CtTypeAccess && !RepairPatternUtils.isThisAccess((CtTypeAccess) ctElement)
                                    && RepairPatternUtils.isConstantTypeAccess((CtTypeAccess) ctElement))) {
                                isConstantVariable = true;
                            }
                            if (((InsertOperation) operation2Insert).getParent() == ctLiteral.getParent() && isConstantVariable) {
                                CtElement parent = MappingAnalysis.getParentLine(new LineFilter(), ctLiteral);
                                ITree lineTree = MappingAnalysis.getFormattedTreeFromControlFlow(parent);

                                repairPatterns.incrementFeatureCounterInstance(CONST_CHANGE,
                                        new PatternInstance(CONST_CHANGE, operation2Insert,
                                                operation2Insert.getSrcNode(), ctLiteral, parent, lineTree,
                                                new PropertyPair("type", "literal_by_varaccess")));
                            }
                        }
                    }
                }

//                if (operation instanceof DeleteOperation && operation.getSrcNode() instanceof CtTypeAccess) {
//                    CtTypeAccess cttypeaccess = (CtTypeAccess) operation.getSrcNode();
//                    
//                    // try to search a replacement for the literal
//                    if(!RepairPatternUtils.isThisAccess(cttypeaccess) &&
//                            RepairPatternUtils.isConstantTypeAccess(cttypeaccess))
//                      for (int j = 0; j < operations.size(); j++) {
//                        Operation operation2Insert = operations.get(j);
//                        if (operation2Insert instanceof InsertOperation) {
//                            CtElement ctElement = operation2Insert.getSrcNode();
//                            boolean isliteralorvariable = false;
//                            if (ctElement instanceof CtLiteral
//                                    || (ctElement instanceof CtVariableAccess)) {
//                                isliteralorvariable = true;
//                            }
//                            if (((InsertOperation) operation2Insert).getParent() == cttypeaccess.getParent()
//                                    && isliteralorvariable) {
//                                CtElement parent = MappingAnalysis.getParentLine(new LineFilter(), cttypeaccess);
//                                ITree lineTree = MappingAnalysis.getFormatedTreeFromControlFlow(parent);
//
//                                repairPatterns.incrementFeatureCounterInstance(CONST_CHANGE,
//                                        new PatternInstance(CONST_CHANGE, operation2Insert,
//                                                operation2Insert.getSrcNode(), cttypeaccess, parent, lineTree,
//                                                new PropertyPair("type", "typeaccess_by_literalvariable")));
//                            }
//                        }
//                    }
//                }    
            }
        }
    }
}
