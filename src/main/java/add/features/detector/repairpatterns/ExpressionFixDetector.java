package add.features.detector.repairpatterns;

import add.entities.PatternInstance;
import add.entities.PropertyPair;
import add.entities.RepairPatterns;
import add.features.detector.spoon.LogicalExpressionAnalyzer;
import add.features.detector.spoon.MappingAnalysis;
import add.features.detector.spoon.RepairPatternUtils;
import com.github.gumtreediff.tree.ITree;
import gumtree.spoon.builder.SpoonGumTreeBuilder;
import gumtree.spoon.diff.operations.InsertOperation;
import gumtree.spoon.diff.operations.MoveOperation;
import gumtree.spoon.diff.operations.Operation;
import gumtree.spoon.diff.operations.UpdateOperation;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtCase;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFor;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.code.CtWhile;
import spoon.reflect.code.UnaryOperatorKind;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.path.CtRole;
import spoon.reflect.visitor.EarlyTerminatingScanner;
import spoon.reflect.visitor.filter.LineFilter;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by tdurieux
 */
public class ExpressionFixDetector extends AbstractPatternDetector {

    public static final String BIN_OPERATOR_MODIF = "binOperatorMod";
    public static final String EXP_LOGIC_MOD = "expLogicMod";
    public static final String EXP_LOGIC_REDUCE = "expLogicReduce";
    public static final String EXP_LOGIC_EXPAND = "expLogicExpand";

    public ExpressionFixDetector(List<Operation> operations) {
        super(operations);
    }

    @Override
    public void detect(RepairPatterns repairPatterns) {
        List<BinaryOperatorKind> mathematicOperator = Arrays.asList(BinaryOperatorKind.SL, BinaryOperatorKind.SR, BinaryOperatorKind.USR, BinaryOperatorKind.PLUS, BinaryOperatorKind.MINUS, BinaryOperatorKind.MUL, BinaryOperatorKind.DIV, BinaryOperatorKind.MOD);
        List<UnaryOperatorKind> unaryOperators = Arrays.asList(UnaryOperatorKind.PREINC, UnaryOperatorKind.POSTINC, UnaryOperatorKind.POSTDEC, UnaryOperatorKind.PREDEC);
        List<BinaryOperatorKind> logicOperators = Arrays.asList(BinaryOperatorKind.OR, BinaryOperatorKind.AND, BinaryOperatorKind.BITOR, BinaryOperatorKind.BITXOR, BinaryOperatorKind.BITAND, BinaryOperatorKind.EQ, BinaryOperatorKind.NE, BinaryOperatorKind.LT, BinaryOperatorKind.GT, BinaryOperatorKind.LE, BinaryOperatorKind.GE);
        List<BinaryOperatorKind> conditionalOperators = Arrays.asList(BinaryOperatorKind.AND, BinaryOperatorKind.OR);
        for (int i = 0; i < operations.size(); i++) {
            Operation operation = operations.get(i);
            if (operation instanceof MoveOperation) {
                continue;
            }
            LineFilter filter = new LineFilter();
            if ((operation instanceof UpdateOperation)) {
                CtElement dstNode = operation.getDstNode();
                CtBinaryOperator binaryOperator = dstNode instanceof CtBinaryOperator ? (CtBinaryOperator) dstNode : dstNode.getParent(CtBinaryOperator.class);
                if (binaryOperator != null) {

                    CtBinaryOperator buggybinaryOperator = operation.getSrcNode() instanceof CtBinaryOperator
                            ? (CtBinaryOperator) operation.getSrcNode()
                            : operation.getSrcNode().getParent(CtBinaryOperator.class);

                    if (buggybinaryOperator != null && binaryOperator != null &&
                            idem(buggybinaryOperator.getLeftHandOperand(), binaryOperator.getLeftHandOperand()) &&
                            idem(buggybinaryOperator.getRightHandOperand(), binaryOperator.getRightHandOperand())) {

                        CtElement parentLine = MappingAnalysis.getParentLine(filter, buggybinaryOperator);

                        ITree lineTree = MappingAnalysis.getFormattedTreeFromControlFlow(parentLine);

                        repairPatterns.incrementFeatureCounterInstance(BIN_OPERATOR_MODIF,
                                new PatternInstance(BIN_OPERATOR_MODIF, operation, dstNode, buggybinaryOperator,
                                        parentLine, lineTree, new PropertyPair("type", "sameLeftRighTerms")));

                    }

                    if (mathematicOperator.contains(binaryOperator.getKind())) {
                        if (!RepairPatternUtils.isStringInvolvedInBinaryOperator(binaryOperator)) {
                            repairPatterns.incrementFeatureCounter("expArithMod", operation);
                        }
                    } else {
                        CtElement parent = binaryOperator.getParent(filter);
                        if (parent instanceof CtIf) {
                            CtIf parentIf = (CtIf) parent;
                            if (parentIf.getMetadata("isMoved") == null) {
                                repairPatterns.incrementFeatureCounter(EXP_LOGIC_MOD, operation);
                            }
                        } else {
                            CtBinaryOperator parentBinaryOperator = binaryOperator;
                            while (parentBinaryOperator.getParent() instanceof CtBinaryOperator) {
                                parentBinaryOperator = (CtBinaryOperator) parentBinaryOperator.getParent();
                            }
                            if (hasMetaInIt(parentBinaryOperator, "update")) {
                                repairPatterns.incrementFeatureCounter(EXP_LOGIC_MOD, operation);
                            }
                        }
                    }
                }
                CtUnaryOperator unaryOperator = dstNode instanceof CtUnaryOperator ? (CtUnaryOperator) dstNode : dstNode.getParent(CtUnaryOperator.class);
                if (unaryOperator != null) {
                    if (unaryOperators.contains(unaryOperator.getKind())) {
                        repairPatterns.incrementFeatureCounter("expArithMod", operation);
                    }
                }
                if (dstNode.getParent() instanceof CtIf && dstNode.getRoleInParent() == CtRole.CONDITION) {
                    repairPatterns.incrementFeatureCounter(EXP_LOGIC_MOD, operation);//Chart-1
                }
            } else {
                CtElement srcNode = operation.getSrcNode();
                if (srcNode.isImplicit() && srcNode instanceof CtBlock
                        && ((CtBlock) srcNode).getStatements().size() == 1) {
                    srcNode = ((CtBlock) srcNode).getLastStatement();
                }

                boolean isExpLogicExOrRed = false;
                if (srcNode instanceof CtBinaryOperator) {
                    if (RepairPatternUtils.isMovedCondition(srcNode)) {
                        continue;
                    }

                    if (hasMetaInIt(srcNode, "isMoved")) {
                        if (mathematicOperator.contains(((CtBinaryOperator) srcNode).getKind())) {
                            if (!RepairPatternUtils.isStringInvolvedInBinaryOperator((CtBinaryOperator) srcNode)) {
                                repairPatterns.incrementFeatureCounter("expArithMod", operation);
                            }
                        }
                    }

                    CtBinaryOperator ctBinaryOperator = (CtBinaryOperator) srcNode;
                    CtBinaryOperator parentBinaryOperator = ctBinaryOperator.getParent(CtBinaryOperator.class);
                    if (parentBinaryOperator == null) {
                        parentBinaryOperator = ctBinaryOperator;
                    }
                    boolean isThereOldCondition = false;
                    boolean isThereNewCondition = false;
                    boolean isThereDeletedCondition = false;
                    boolean isThereChangedCondition = false;

                    List<CtBinaryOperator> binaryOperatorList = parentBinaryOperator
                            .getElements(new TypeFilter<>(CtBinaryOperator.class));
                    for (CtBinaryOperator binaryOperator : binaryOperatorList) {
                        if (conditionalOperators.contains(ctBinaryOperator.getKind())) {
                            if (RepairPatternUtils.isNewBinaryOperator(binaryOperator) &&
                                    !RepairPatternUtils.isDeletedBinaryOperator(binaryOperator) &&
                                    RepairPatternUtils.isNewConditionInBinaryOperator(binaryOperator)) {
                                isThereNewCondition = true;
                            }
                            if (RepairPatternUtils.isExistingConditionInBinaryOperator(binaryOperator)) {
                                isThereOldCondition = true;
                            }
                            if (RepairPatternUtils.isDeletedBinaryOperator(binaryOperator) &&
                                    RepairPatternUtils.isDeletedConditionInBinaryOperator(binaryOperator)) {
                                isThereDeletedCondition = true;
                            }
                            if (RepairPatternUtils.isUpdatedConditionInBinaryOperator(binaryOperator)) {
                                isThereChangedCondition = true;
                            }
                        }
                    }
                    List<CtUnaryOperator> unaryOperatorList = parentBinaryOperator.getElements(new TypeFilter<>(CtUnaryOperator.class));
                    for (CtUnaryOperator unaryOperator : unaryOperatorList) {
                        //if (logicOperators.contains(ctBinaryOperator.getKind()))
                        if (RepairPatternUtils.isNewUnaryOperator(unaryOperator) &&
                                !RepairPatternUtils.isDeletedUnaryOperator(unaryOperator) &&
                                RepairPatternUtils.isNewConditionInUnaryOperator(unaryOperator)) {
                            isThereNewCondition = true;
                        }
                        if (RepairPatternUtils.isExistingConditionInUnaryOperator(unaryOperator)) {
                            isThereOldCondition = true;
                        }
                        if (RepairPatternUtils.isDeletedUnaryOperator(unaryOperator)) {
                            isThereDeletedCondition = true;
                        }
                        if (RepairPatternUtils.isUpdatedConditionInUnaryOperator(unaryOperator)) {
                            isThereChangedCondition = true;
                        }
                    }
                    if (isThereChangedCondition) {
                        repairPatterns.incrementFeatureCounter(EXP_LOGIC_MOD, operation);
                        isExpLogicExOrRed = true;
                    }
                    if (isThereOldCondition && isThereNewCondition) {

                        List<CtElement> suspLeft = MappingAnalysis.getTreeLeftMovedFromRight(diff, parentBinaryOperator);
                        if (suspLeft == null || suspLeft.isEmpty()) {
                            return;
                        }

                        CtElement parentLine = MappingAnalysis.getParentLine(filter, suspLeft.get(0));
                        ITree lineTree = MappingAnalysis.getFormattedTreeFromControlFlow(parentLine);

                        CtElement susplogical = suspLeft.get(0);

                        List<CtExpression> allrootlogicalexpers = LogicalExpressionAnalyzer.getAllRootLogicalExpressions(parentLine);

                        for (int index = 0; index < allrootlogicalexpers.size(); index++) {

                            CtExpression experunderstudy = allrootlogicalexpers.get(index);
                            List<CtExpression> expressionssFromFaultyLine = experunderstudy.getElements(e -> (e instanceof CtExpression)).stream()
                                    .map(CtExpression.class::cast).collect(Collectors.toList());

                            if (expressionssFromFaultyLine.contains(susplogical)) {
                                susplogical = experunderstudy;
                                break;
                            }
                        }

                        repairPatterns.incrementFeatureCounterInstance(EXP_LOGIC_EXPAND, new PatternInstance(
                                EXP_LOGIC_EXPAND, operation, parentBinaryOperator, susplogical, parentLine, lineTree));

                        isExpLogicExOrRed = true;
                    }
                    if (isThereOldCondition && isThereDeletedCondition) {

                        ITree parentAffectInRight = MappingAnalysis.getParentInRight(diff, operation.getAction());
                        CtElement affected = (CtElement) parentAffectInRight.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);

                        CtBinaryOperator binary = (CtBinaryOperator) srcNode;

                        CtElement removedNode = (binary.getRightHandOperand().getMetadata("delete") != null)
                                ? binary.getRightHandOperand()
                                : binary.getLeftHandOperand();

                        CtElement parentLine = MappingAnalysis.getParentLine(filter, binary);

                        ITree lineTree = MappingAnalysis.getFormattedTreeFromControlFlow(parentLine);

                        CtElement susplogical = removedNode;

                        List<CtExpression> allrootlogicalexpers = LogicalExpressionAnalyzer.getAllRootLogicalExpressions(parentLine);

                        for (int index = 0; index < allrootlogicalexpers.size(); index++) {

                            CtExpression experunderstudy = allrootlogicalexpers.get(index);
                            List<CtExpression> expressionssFromFaultyLine = experunderstudy.getElements(e -> (e instanceof CtExpression)).stream()
                                    .map(CtExpression.class::cast).collect(Collectors.toList());

                            if (expressionssFromFaultyLine.contains(susplogical)) {
                                susplogical = experunderstudy;
                                break;
                            }
                        }

                        repairPatterns.incrementFeatureCounterInstance(EXP_LOGIC_REDUCE, new PatternInstance(
                                EXP_LOGIC_REDUCE, operation, affected, susplogical, parentLine, lineTree));

                        repairPatterns.incrementFeatureCounter(EXP_LOGIC_REDUCE, operation);

                        isExpLogicExOrRed = true;
                    }
                }

                if (srcNode instanceof CtUnaryOperator) {
                    if (isInCondition(srcNode)) {
                        if (srcNode.getParent().getMetadata("isMoved") == null) {
                            if (operation instanceof InsertOperation) {
                                if (hasMetaInIt(((InsertOperation) operation).getParent(), "delete")) {
                                    repairPatterns.incrementFeatureCounter(EXP_LOGIC_MOD, operation);
                                }
                            }
                        }
                    }
                }

                if (srcNode instanceof CtCase) {
                    CtCase ctCase = (CtCase) srcNode;
                    if (ctCase.getParent().getMetadata("isMoved") == null) {
                        if (ctCase.getStatements().size() == 0) {
                            if (operation instanceof InsertOperation) {
                                repairPatterns.incrementFeatureCounter(EXP_LOGIC_EXPAND, operation);
                                isExpLogicExOrRed = true;
                            } else {
                                // cannot delete completely the condition
                                if (!(srcNode.getParent() instanceof CtIf) || hasMetaInIt(srcNode, "isMoved")) {
                                    repairPatterns.incrementFeatureCounter(EXP_LOGIC_REDUCE, operation);
                                    isExpLogicExOrRed = true;
                                }
                            }
                        } else {
                            if (RepairPatternUtils.isThereOldStatementInStatementList(ctCase.getStatements())) {
                                for (int j = 0; j < operations.size(); j++) {
                                    Operation operation2 = operations.get(j);
                                    if (operation2 instanceof MoveOperation) {
                                        CtElement movedSrcNode = operation2.getSrcNode();
                                        if (movedSrcNode.getParent() instanceof CtCase) {
                                            if (operation instanceof InsertOperation) {
                                                repairPatterns.incrementFeatureCounter(EXP_LOGIC_EXPAND, operation);
                                                isExpLogicExOrRed = true;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (srcNode instanceof CtExpression && operation instanceof InsertOperation && !isExpLogicExOrRed) {
                    CtIf parent = srcNode.getParent(CtIf.class);
                    CtBinaryOperator binary = srcNode.getParent(CtBinaryOperator.class);
                    if (parent != null && isInCondition(binary) && parent.getMetadata("isMoved") == null) {
                        // Discard
                        repairPatterns.incrementFeatureCounter(EXP_LOGIC_MOD, operation);//math 76
                    }
                }
                CtBinaryOperator binaryOperator = srcNode.getParent(CtBinaryOperator.class);
                if (binaryOperator != null) {
                    if (mathematicOperator.contains(binaryOperator.getKind())) {
                        if (!RepairPatternUtils.isStringInvolvedInBinaryOperator(binaryOperator)) {
                            repairPatterns.incrementFeatureCounter("expArithMod", operation);
                        }
                    }
                }
            }
        }
    }

    private boolean idem(CtExpression rightHandOperand, CtExpression ctExpression) {
        if (rightHandOperand == null && ctExpression == null) {
            return true;
        }
        if (rightHandOperand == null || ctExpression == null) {
            return false;
        }
        return rightHandOperand.equals(ctExpression);
    }

    private boolean isInCondition(CtElement element) {
        if (element == null) {
            return false;
        }
        CtIf ifParent = element.getParent(CtIf.class);
        if (ifParent != null && (element.hasParent(ifParent.getCondition()) || element == ifParent.getCondition())) {
            return true;
        }
        CtFor forParent = element.getParent(CtFor.class);
        if (forParent != null && (element.hasParent(forParent.getExpression()) || element == forParent.getExpression())) {
            return true;
        }
        CtWhile whileParent = element.getParent(CtWhile.class);
        return whileParent != null && (element.hasParent(whileParent.getLoopingExpression()) ||
                element == whileParent.getLoopingExpression());
    }

    private boolean hasMetaInIt(CtElement element, String meta) {
        EarlyTerminatingScanner hasMeta = new EarlyTerminatingScanner() {
            @Override
            public void scan(CtElement element) {
                if (element != null && element.getMetadata(meta) != null) {
                    setResult(element);
                    terminate();
                }
                super.scan(element);
            }
        };
        hasMeta.scan(element);
        return hasMeta.getResult() != null;
    }
}
