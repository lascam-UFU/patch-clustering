package add.features.detector.repairpatterns;

import add.entities.RepairPatterns;
import gumtree.spoon.diff.operations.MoveOperation;
import gumtree.spoon.diff.operations.Operation;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtVariableAccess;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.visitor.DefaultJavaPrettyPrinter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by tdurieux
 */
public class CopyPasteDetector extends AbstractPatternDetector {

    public CopyPasteDetector(List<Operation> operations) {
        super(operations);
    }

    @Override
    public void detect(RepairPatterns repairPatterns) {
        Set<String> operationString = new HashSet<>();
        for (int i = 0; i < operations.size(); i++) {
            Operation operation = operations.get(i);
            if (operation instanceof MoveOperation) {
                continue;
            }
            CtElement srcNode = operation.getSrcNode();
            CtElement parent = srcNode.getParent();
            if (parent.getMetadata("isMoved") != null || parent.getMetadata("isNew") != null) {
                continue;
            }
            DefaultJavaPrettyPrinter print = new DefaultJavaPrettyPrinter(srcNode.getFactory().getEnvironment()) {
                @Override
                public DefaultJavaPrettyPrinter scan(CtElement e) {
                    if (e != null && e.getMetadata("isMoved") == null) {
                        if (e instanceof CtBinaryOperator) {
                            CtExpression leftHandOperand = ((CtBinaryOperator) e).getLeftHandOperand();
                            CtExpression rightHandOperand = ((CtBinaryOperator) e).getRightHandOperand();
                            if (rightHandOperand.getMetadata("isMoved") == null
                                    && leftHandOperand.getMetadata("isMoved") != null) {
                                ((CtBinaryOperator) e).setLeftHandOperand(rightHandOperand);
                                ((CtBinaryOperator) e).setRightHandOperand(leftHandOperand);
                                super.scan(e);
                                ((CtBinaryOperator) e).setLeftHandOperand(leftHandOperand);
                                ((CtBinaryOperator) e).setRightHandOperand(rightHandOperand);
                            } else {
                                super.scan(e);
                            }
                        } else if (e instanceof CtVariableAccess) {
                            String simpleName = ((CtVariableAccess) e).getVariable().getSimpleName();
                            ((CtVariableAccess) e).getVariable().setSimpleName("VAR");
                            super.scan(e);
                            ((CtVariableAccess) e).getVariable().setSimpleName(simpleName);
                        } else if (e instanceof CtLiteral) {
                            Object value = ((CtLiteral) e).getValue();
                            ((CtLiteral) e).setValue("VAR");
                            super.scan(e);
                            ((CtLiteral) e).setValue(value);
                        } else {
                            super.scan(e);
                        }
                        return this;
                    }
                    return this;
                }
            };
            srcNode.setParent(null);
            print.scan(srcNode);
            srcNode.setParent(parent);
            String result = print.getResult().trim().replaceAll("\"VAR\"", "VAR");
            if (operationString.contains(result) && !result.isEmpty() && !"VAR".equals(result)) {
                repairPatterns.incrementFeatureCounter("copyPaste", operation);
            }
            operationString.add(result);
        }
    }

}
