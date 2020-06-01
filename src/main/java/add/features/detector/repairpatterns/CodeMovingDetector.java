package add.features.detector.repairpatterns;

import add.entities.RepairPatterns;
import add.features.detector.spoon.RepairPatternUtils;
import gumtree.spoon.diff.operations.MoveOperation;
import gumtree.spoon.diff.operations.Operation;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;

import java.util.List;

/**
 * Created by tdurieux
 */
public class CodeMovingDetector extends AbstractPatternDetector {

    private static final String CODE_MOVE = "codeMove";

    public CodeMovingDetector(List<Operation> operations) {
        super(operations);
    }

    @Override
    public void detect(RepairPatterns repairPatterns) {
        for (int i = 0; i < operations.size(); i++) {
            Operation operation = operations.get(i);
            if (!(operation instanceof MoveOperation)) {
                continue;
            }
            CtElement dstNode = operation.getDstNode();
            CtElement dstParent = dstNode.getParent();
            if (dstParent instanceof CtBlock) {
                if (dstParent.getMetadata("new") != null) {
                    continue;
                }
                dstParent = dstParent.getParent();
            }
            CtElement srcNode = operation.getSrcNode();
            CtElement srcParent = srcNode.getParent();
            if (srcParent instanceof CtBlock) {
                if (srcParent.getMetadata("new") != null) {
                    continue;
                }
                srcParent = srcParent.getParent();
                if (srcParent instanceof CtIf) {
                    if (RepairPatternUtils.wasConditionChangedInIf(((CtIf) srcParent))) {
                        continue;
                    }
                }
            }
            if (!(dstNode instanceof CtStatement)) {
                continue;
            }
            if (!(srcNode instanceof CtStatement)) {
                continue;
            }

            CtMethod methoddestination = dstNode.getParent(CtMethod.class);
            CtMethod methodinsrc = srcNode.getParent(CtMethod.class);
            if (methoddestination != null && methodinsrc != null && !methodinsrc.getSignature().equals(methoddestination.getSignature())) {
                continue;
            }

            if (!RepairPatternUtils.isThereChangesInChildren(srcNode)) {
                if (dstParent.getMetadata("new") == null &&
                        srcParent.getMetadata("new") == null
                        && dstNode.getPosition().getSourceStart() != srcNode.getPosition().getSourceStart()) {
                    repairPatterns.incrementFeatureCounter(CODE_MOVE, operation);

                    // Move maction = (Move) operation.getAction();
                    // List<CtElement> suspicious = MappingAnalysis.getFollowStatements(diff, maction);
                    // MappingAnalysis.getAllStatementsOfParent(maction);
                    // suspicious.remove(srcNode);
//                    if (suspicious.size() > 0) {
//                        CtElement lineP = MappingAnalysis.getParentLine(new LineFilter(), suspicious.get(0));
//                        ITree lineTree = MappingAnalysis.getCorrespondingInSourceTree(diff,
//                                operation.getAction().getNode(), lineP);
//
//                        repairPatterns.incrementFeatureCounterInstance(CODE_MOVE,
//                                new PatternInstance(CODE_MOVE, operation, dstNode, suspicious, lineP, lineTree));
//                    } else {
//                        repairPatterns.incrementFeatureCounterInstance(CODE_MOVE, new PatternInstance(CODE_MOVE,
//                                operation, dstNode, suspicious, srcNode, maction.getParent()));
//                    }
                }
            }
        }
    }

}
