package add.features.detector.repairpatterns;

import add.entities.PatternInstance;
import add.entities.RepairPatterns;
import add.features.detector.spoon.MappingAnalysis;
import add.features.detector.spoon.RepairPatternUtils;
import com.github.gumtreediff.tree.ITree;
import gumtree.spoon.builder.SpoonGumTreeBuilder;
import gumtree.spoon.diff.operations.InsertOperation;
import gumtree.spoon.diff.operations.Operation;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.CtElement;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class AssignmentDetector extends AbstractPatternDetector {

    public static final String ADD_ASSIGNMENT = "addAssignment";

    public AssignmentDetector(List<Operation> operations) {
        super(operations);
    }

    @Override
    public void detect(RepairPatterns repairPatterns) {
        for (int i = 0; i < operations.size(); i++) {
            Operation operation = operations.get(i);
            if (!(operation instanceof InsertOperation) || !(operation.getSrcNode() instanceof CtAssignment)) {
                continue;
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

            if (!(srcNode instanceof CtStatement)) {
                continue;
            }

            InsertOperation operationIns = (InsertOperation) operation;

            List<ITree> treeInLeft = MappingAnalysis.getFollowStatementsInLeft(diff, operationIns.getAction());
            if (treeInLeft.isEmpty()) {
                continue;
            }

            List<CtElement> followCtElementsInLeft = new ArrayList<>();
            // Fixed: we are interested only in the first statement after the insertion
            ITree suspiciousTree = treeInLeft.get(0);

            CtElement associatedLeftCtElement = (CtElement) suspiciousTree
                    .getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);

            // Let's format the node in case that it's a control flow
            suspiciousTree = MappingAnalysis.getFormattedTreeFromControlFlow(associatedLeftCtElement);

            followCtElementsInLeft.add(associatedLeftCtElement);

            repairPatterns.incrementFeatureCounterInstance(ADD_ASSIGNMENT,
                    new PatternInstance(ADD_ASSIGNMENT,
                            operation,
                            operation.getSrcNode(),
                            followCtElementsInLeft,
                            associatedLeftCtElement,
                            suspiciousTree));

        }
    }

}
