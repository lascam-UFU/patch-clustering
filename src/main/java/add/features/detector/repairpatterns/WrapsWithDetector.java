package add.features.detector.repairpatterns;

import add.entities.PatternInstance;
import add.entities.PropertyPair;
import add.entities.RepairPatterns;
import add.features.detector.spoon.MappingAnalysis;
import add.features.detector.spoon.RepairPatternUtils;
import add.features.detector.spoon.SpoonHelper;
import com.github.gumtreediff.tree.ITree;
import gumtree.spoon.builder.SpoonGumTreeBuilder;
import gumtree.spoon.diff.operations.DeleteOperation;
import gumtree.spoon.diff.operations.InsertOperation;
import gumtree.spoon.diff.operations.MoveOperation;
import gumtree.spoon.diff.operations.Operation;
import gumtree.spoon.diff.operations.UpdateOperation;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtCatch;
import spoon.reflect.code.CtConditional;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFor;
import spoon.reflect.code.CtForEach;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLoop;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtTry;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.code.CtWhile;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.visitor.filter.LineFilter;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by fermadeiral
 */
public class WrapsWithDetector extends AbstractPatternDetector {

    public static final String WRAPS_LOOP = "wrapsLoop";
    public static final String UNWRAP_METHOD = "unwrapMethod";
    public static final String UNWRAP_TRY_CATCH = "unwrapTryCatch";
    public static final String WRAPS_METHOD = "wrapsMethod";
    public static final String WRAPS_TRY_CATCH = "wrapsTryCatch";
    public static final String WRAPS_ELSE = "wrapsElse";
    public static final String WRAPS_IF_ELSE = "wrapsIfElse";
    public static final String UNWRAP_IF_ELSE = "unwrapIfElse";
    public static final String WRAPS_IF = "wrapsIf";

    public WrapsWithDetector(List<Operation> operations) {
        super(operations);
    }

    @Override
    public void detect(RepairPatterns repairPatterns) {
        for (Operation operation : this.operations) {
            this.detectWrapsIf(operation, repairPatterns);
            this.detectWrapsTryCatch(operation, repairPatterns);
            this.detectWrapsMethod(operation, repairPatterns);
            this.detectWrapsLoop(operation, repairPatterns);
        }
    }

    private void detectWrapsIf(Operation operation, RepairPatterns repairPatterns) {
        if (operation instanceof InsertOperation || operation instanceof DeleteOperation) {
            CtElement ctElement = operation.getSrcNode();
            SpoonHelper.printInsertOrDeleteOperation(ctElement.getFactory().getEnvironment(), ctElement, operation);

            List<CtIf> ifList = ctElement.getElements(new TypeFilter<>(CtIf.class));
            for (CtIf ctIf : ifList) {
                if (RepairPatternUtils.isNewIf(ctIf)) {
                    CtBlock thenBlock = ctIf.getThenStatement();
                    CtBlock elseBlock = ctIf.getElseStatement();
                    if (elseBlock == null) {
                        List stmtsMoved = RepairPatternUtils.getIsThereOldStatementInStatementList(diff, thenBlock.getStatements());
                        if (!stmtsMoved.isEmpty()) {
                            if (operation instanceof InsertOperation) {
                                List<ITree> leftTreees = new ArrayList();
                                List<CtElement> leftElements = new ArrayList();

                                calculatorLeftInformation(stmtsMoved, leftTreees, leftElements);

                                if (leftElements.size() > 0)
                                    repairPatterns.incrementFeatureCounterInstance(WRAPS_IF,
                                            new PatternInstance(WRAPS_IF, operation, ctIf, leftElements,
                                                    leftElements.get(0), leftTreees.get(0),
                                                    new PropertyPair("case", "elsenull")));
                            } else {
                                // Remove if THEN (Not else present)
                                List susp = new ArrayList<>();

                                // Let's take all elements that are inside the removed if
                                // List subelements = ctElement.getElements(new TypeFilter<>(CtStatement.class));
                                // susp.addAll(subelements);

                                if (!susp.contains(ctIf))
                                    // Suspicious is the removed if
                                    susp.add(ctIf);
//
//                                for (Object object : stmtsMoved) {
//                                    CtElement e = (CtElement) object;
//                                    susp.removeAll(e.getElements(new TypeFilter<>(CtStatement.class)));
//                                }

                                CtElement lineP = MappingAnalysis.getParentLine(new LineFilter(),
                                        (CtElement) susp.get(0));

                                ITree lineTree = MappingAnalysis.getFormattedTreeFromControlFlow(lineP);

//                                repairPatterns.incrementFeatureCounterInstance(UNWRAP_IF_ELSE, //
//                                        new PatternInstance(UNWRAP_IF_ELSE, operation, (CtElement) stmtsMoved.get(0),
//                                                susp, lineP, lineTree, new PropertyPair("case", "elsenull")));
                                repairPatterns.incrementFeatureCounterInstance(UNWRAP_IF_ELSE, //
                                        new PatternInstance(UNWRAP_IF_ELSE, operation, (CtElement) stmtsMoved.get(0),
                                                lineP, lineP, lineTree, new PropertyPair("case", "elsenull")));
                            }
                        }
                    } else {// ELSE has content
                        List sthen = RepairPatternUtils.getIsThereOldStatementInStatementList(diff, thenBlock.getStatements());
                        List selse = RepairPatternUtils.getIsThereOldStatementInStatementList(diff,
                                elseBlock.getStatements());
                        // selse.addAll(sthen);
                        if (!sthen.isEmpty() || !selse.isEmpty()) {
                            List moved = new ArrayList<>();
                            moved.addAll(sthen);
                            moved.addAll(selse);

                            if (operation instanceof InsertOperation) {

                                CtElement lineP = MappingAnalysis.getParentLine(new LineFilter(),
                                        (CtElement) moved.get(0));
                                ITree lineTree = MappingAnalysis.getFormattedTreeFromControlFlow(lineP);

                                repairPatterns.incrementFeatureCounterInstance(WRAPS_IF_ELSE,
                                        new PatternInstance(WRAPS_IF_ELSE, operation, ctIf, moved, lineP, lineTree,
                                                new PropertyPair("case", "elsenotnull")));
                            } else {
                                // ELSE with content
                                List susp = new ArrayList<>();
                                // Suspicious is the removed if
                                susp.add(ctIf);

                                CtElement lineP = MappingAnalysis.getParentLine(new LineFilter(),
                                        (CtElement) susp.get(0));
                                ITree lineTree = MappingAnalysis.getFormattedTreeFromControlFlow(lineP);

                                repairPatterns.incrementFeatureCounterInstance(UNWRAP_IF_ELSE, //
                                        new PatternInstance(UNWRAP_IF_ELSE, operation, (CtElement) moved.get(0), susp,
                                                lineP, lineTree, new PropertyPair("case", "elsenotnull")));
                            }
                        }
                    }
                }
            }

            // wrap_else repair transform
            List<CtBlock> blockList = ctElement.getElements(new TypeFilter<>(CtBlock.class));
            for (CtBlock ctBlock : blockList) {
                if (ctBlock.getMetadata("new") != null) {
                    if (ctBlock.getParent() instanceof CtIf) {
                        CtIf ctIfParent = (CtIf) ctBlock.getParent();
                        CtBlock elseBlock = ctIfParent.getElseStatement();
                        if (ctBlock == elseBlock) {
                            if (!RepairPatternUtils.isNewIf(ctIfParent)) {
                                CtBlock thenBlock = ctIfParent.getThenStatement();
                                if (thenBlock != null && RepairPatternUtils
                                        .isThereOldStatementInStatementList(thenBlock.getStatements())) {
                                    List selse = RepairPatternUtils.getIsThereOldStatementInStatementList(diff,
                                            elseBlock.getStatements());
                                    if (!selse.isEmpty()) {

                                        if (operation instanceof InsertOperation) {
                                            CtElement lineP = MappingAnalysis.getParentLine(new LineFilter(),
                                                    (CtElement) selse.get(0));
                                            ITree lineTree = MappingAnalysis.getFormattedTreeFromControlFlow(lineP);

                                            repairPatterns.incrementFeatureCounterInstance(WRAPS_ELSE,
                                                    new PatternInstance(WRAPS_ELSE, operation, ctIfParent, selse, lineP,
                                                            lineTree));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            List<CtConditional> conditionalList = ctElement.getElements(new TypeFilter<>(CtConditional.class));
            for (CtConditional ctConditional : conditionalList) {
                if (ctConditional.getMetadata("new") != null) {
                    CtExpression thenExpression = ctConditional.getThenExpression();
                    CtExpression elseExpression = ctConditional.getElseExpression();
                    if (thenExpression.getMetadata("new") == null || elseExpression.getMetadata("new") == null) {
                        CtElement statementParent = ctConditional.getParent(new TypeFilter<>(CtStatement.class));
                        if (operation instanceof InsertOperation) {

                            if (statementParent.getMetadata("new") == null) {
                                // We get the not new (the moved).
                                // As the then/else is inserted with the Condition, it's always in the right
                                CtElement suspRigh = (thenExpression.getMetadata("new") != null) ? elseExpression
                                        : thenExpression;

                                ITree leftMoved = MappingAnalysis.getLeftFromRightNodeMapped(diff,
                                        (ITree) suspRigh.getMetadata("gtnode"));

                                CtElement susp = (CtElement) leftMoved.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);

                                CtElement lineP = MappingAnalysis.getParentLine(new LineFilter(), susp);
                                ITree lineTree = MappingAnalysis.getFormattedTreeFromControlFlow(lineP);

                                repairPatterns.incrementFeatureCounterInstance(WRAPS_IF_ELSE, new PatternInstance(
                                        WRAPS_IF_ELSE, operation, ctConditional, susp, lineP, lineTree,
                                        new PropertyPair("case", "elsenotnull")));
                            }
                        } else {
                            if (statementParent.getMetadata("delete") == null) {
                                List susps = new ArrayList();
                                CtExpression patch = null;
                                if (thenExpression.getMetadata("new") == null) {
                                    patch = thenExpression;
                                    susps.add(elseExpression);
                                    susps.add(ctConditional.getCondition());

                                } else {
                                    patch = elseExpression;
                                    susps.add(thenExpression);
                                    susps.add(ctConditional.getCondition());
                                }
                                CtElement lineP = MappingAnalysis.getParentLine(new LineFilter(),
                                        (CtElement) susps.get(0));
                                ITree lineTree = MappingAnalysis.getFormattedTreeFromControlFlow(lineP);

                                repairPatterns.incrementFeatureCounterInstance(UNWRAP_IF_ELSE,
                                        new PatternInstance(UNWRAP_IF_ELSE, operation, patch, ctConditional, lineP, lineTree));
                            }
                        }
                    } else {
                        if (operation instanceof InsertOperation) {
                            for (int j = 0; j < operations.size(); j++) {
                                Operation operation2 = operations.get(j);
                                if (operation2 instanceof DeleteOperation) {
                                    CtElement node2 = operation2.getSrcNode();
                                    if (((InsertOperation) operation).getParent() != null) {
                                        if (node2.getParent() == ((InsertOperation) operation).getParent()) {

                                            CtElement lineP = MappingAnalysis.getParentLine(new LineFilter(), node2);
                                            ITree lineTree = MappingAnalysis.getFormattedTreeFromControlFlow(lineP);

                                            repairPatterns.incrementFeatureCounterInstance(WRAPS_IF_ELSE,
                                                    new PatternInstance(WRAPS_IF_ELSE, operation, ctConditional, node2,
                                                            lineP, lineTree, new PropertyPair("case", "elsenotnull")));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void calculatorLeftInformation(List stmtsMoved, List<ITree> leftTreees, List<CtElement> leftElements) {
        for (Object object : stmtsMoved) {
            CtElement moved = (CtElement) object;
            // We start in the right because the moved are taken from the added if, not from
            // the Mov operator ()
            ITree mappedLeft = MappingAnalysis.getLeftFromRightNodeMapped(diff, moved);
            if (mappedLeft != null) {
                leftTreees.add(mappedLeft);
                CtElement element = (CtElement) mappedLeft.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                if (!leftElements.contains(element))
                    leftElements.add(element);
            }
        }
    }

    private void detectWrapsTryCatch(Operation operation, RepairPatterns repairPatterns) {
        if (operation instanceof InsertOperation || operation instanceof DeleteOperation) {
            CtElement ctElement = operation.getSrcNode();
            SpoonHelper.printInsertOrDeleteOperation(ctElement.getFactory().getEnvironment(), ctElement, operation);

            List<CtTry> tryList = ctElement.getElements(new TypeFilter<>(CtTry.class));
            for (CtTry ctTry : tryList) {
                if (ctTry.getMetadata("new") != null) {
                    List<CtCatch> catchList = ctTry.getCatchers();
                    if (RepairPatternUtils.isThereOnlyNewCatch(catchList)) {
                        CtBlock tryBodyBlock = ctTry.getBody();
                        List olds = RepairPatternUtils.getIsThereOldStatementInStatementList(diff,
                                tryBodyBlock.getStatements());

                        if (tryBodyBlock != null && !olds.isEmpty()) {
                            CtElement lineP = MappingAnalysis.getParentLine(new LineFilter(), (CtElement) olds.get(0));

                            ITree lineTree = MappingAnalysis.getFormattedTreeFromControlFlow(lineP);

                            if (!(olds.get(0) instanceof CtTry))
                                if (operation instanceof InsertOperation) {
                                    repairPatterns.incrementFeatureCounterInstance(WRAPS_TRY_CATCH,
                                            new PatternInstance(WRAPS_TRY_CATCH, operation, ctTry, olds, lineP, lineTree));
                                } else {
                                    ITree tryTree = (ITree) ctTry.getMetadata("gtnode");
                                    repairPatterns.incrementFeatureCounterInstance(UNWRAP_TRY_CATCH, new PatternInstance(UNWRAP_TRY_CATCH, operation, ctTry, ctTry, ctTry, tryTree));
                                }
                        } else { // try to find a move into the body of the try
                            for (Operation operationAux : this.operations) {
                                if (operationAux instanceof MoveOperation) {
                                    CtElement ctElementDst = operationAux.getDstNode();
                                    CtTry ctTryParent = ctElementDst.getParent(new TypeFilter<>(CtTry.class));
                                    if (ctTryParent != null && ctTryParent == ctTry) {
                                        if (operation instanceof InsertOperation) {

                                            CtElement lineP = MappingAnalysis.getParentLine(new LineFilter(),
                                                    operationAux.getSrcNode());
                                            ITree lineTree = MappingAnalysis.getFormattedTreeFromControlFlow(lineP);

//                                            repairPatterns.incrementFeatureCounterInstance(WRAPS_TRY_CATCH,
//                                                    new PatternInstance(WRAPS_TRY_CATCH, operation, ctTry,
//                                                            operationAux.getSrcNode(), lineP, lineTree));
                                            if (!(operationAux.getSrcNode() instanceof CtTry))
                                                repairPatterns.incrementFeatureCounterInstance(WRAPS_TRY_CATCH,
                                                        new PatternInstance(WRAPS_TRY_CATCH, operation, ctTry,
                                                                lineP, lineP, lineTree));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void detectWrapsMethod(Operation operation, RepairPatterns repairPatterns) {
        if (operation.getSrcNode() instanceof CtInvocation) {
            if (operation instanceof InsertOperation) {
                CtInvocation ctInvocation = (CtInvocation) operation.getSrcNode();
                List<CtExpression> invocationArguments = ctInvocation.getArguments();

                for (Operation operation2 : this.operations) {
                    if (operation2 instanceof DeleteOperation) {
                        CtElement elementRemoved = operation2.getSrcNode();

                        if (elementRemoved instanceof CtVariableRead) {

                            if (invocationArguments.contains(elementRemoved)) {

                                CtElement lineP = MappingAnalysis.getParentLine(new LineFilter(), elementRemoved);
                                ITree lineTree = MappingAnalysis.getFormattedTreeFromControlFlow(lineP);
                                repairPatterns.incrementFeatureCounterInstance(WRAPS_METHOD,
                                        new PatternInstance(WRAPS_METHOD, operation, ctInvocation, elementRemoved,
                                                lineP, lineTree, new PropertyPair("Old", "VarRead"),
                                                new PropertyPair("New", "Invocation")));
                            }
                        }
                        if (elementRemoved instanceof CtAssignment) {

                            if (invocationArguments.contains(((CtAssignment) elementRemoved).getAssignment())) {

                                CtElement lineP = MappingAnalysis.getParentLine(new LineFilter(), elementRemoved);
                                ITree lineTree = MappingAnalysis.getFormattedTreeFromControlFlow(lineP);

                                repairPatterns.incrementFeatureCounterInstance(WRAPS_METHOD,
                                        new PatternInstance(WRAPS_METHOD, operation, ctInvocation,
                                                // ((CtAssignment) elementRemoved).getAssigned()
                                                elementRemoved, lineP, lineTree, new PropertyPair("Old", "Assignment"),
                                                new PropertyPair("New", "Invocation")));
                            }
                        }
                    }
                }

                for (CtExpression ctExpression : invocationArguments) {
                    if (ctExpression.getMetadata("isMoved") != null) {
                        // Operation is an Insert
                        // TODO:
                        List<CtElement> suspLeft = MappingAnalysis.getTreeLeftMovedFromRight(diff, ctInvocation);
                        if (suspLeft == null || suspLeft.isEmpty())
                            return;

                        CtElement lineP = MappingAnalysis.getParentLine(new LineFilter(), suspLeft.get(0));
                        ITree lineTree = MappingAnalysis.getFormattedTreeFromControlFlow(lineP);
                        if (RepairPatternUtils.getIsInvocationInStatemnt(diff, lineP, ctInvocation)) {
                            if (RepairPatternUtils.getElementInOld(diff, ctExpression) != null && (RepairPatternUtils.getElementInOld(diff, ctExpression).getParent() instanceof CtConstructorCall ||
                                    RepairPatternUtils.getElementInOld(diff, ctExpression).getParent() instanceof CtInvocation)
                                    && RepairPatternUtils.getElementInOld(diff, ctExpression).getParent() !=
                                    RepairPatternUtils.getElementInOld(diff, ctInvocation.getParent())) {
                            } else
                                repairPatterns.incrementFeatureCounterInstance(WRAPS_METHOD,
                                        new PatternInstance(WRAPS_METHOD, operation, ctInvocation, ctExpression, lineP,
                                                lineTree, new PropertyPair("Old", "MovedExpression"),
                                                new PropertyPair("New", "Invocation")));
                        }
                    }
                }
            } else {
                if (operation instanceof DeleteOperation) {
                    CtInvocation ctInvocation = (CtInvocation) operation.getSrcNode();
                    CtStatement statementParent = ctInvocation.getParent(new TypeFilter<>(CtStatement.class));

                    if (statementParent.getMetadata("delete") == null) {
                        List<CtExpression> invocationArguments = ctInvocation.getArguments();

                        for (CtExpression ctExpression : invocationArguments) {
                            if (ctExpression.getMetadata("isMoved") != null
                                    && ctExpression.getMetadata("movingSrc") != null) {

                                if (RepairPatternUtils.getIsMovedExpressionInStatement(diff, statementParent, ctExpression) &&
                                        whetherconsinderthemethodunrap(ctExpression, ctInvocation)) {
                                    CtElement lineP = MappingAnalysis.getParentLine(new LineFilter(),
                                            operation.getSrcNode());
                                    ITree lineTree = MappingAnalysis.getFormattedTreeFromControlFlow(lineP);

                                    repairPatterns.incrementFeatureCounterInstance(UNWRAP_METHOD, new PatternInstance(
                                            UNWRAP_METHOD, operation, statementParent, ctInvocation, lineP, lineTree));
                                }
                            }
                        }
                    }
                }
            }
        }

        if (operation.getSrcNode() instanceof CtConstructorCall) {
            if (operation instanceof InsertOperation) {
                CtConstructorCall ctConstructor = (CtConstructorCall) operation.getSrcNode();
                List<CtExpression> constructorArguments = ctConstructor.getArguments();

                for (Operation operation2 : this.operations) {
                    if (operation2 instanceof DeleteOperation) {
                        CtElement elementRemoved = operation2.getSrcNode();

                        if (elementRemoved instanceof CtVariableRead) {

                            if (constructorArguments.contains(elementRemoved)) {

                                CtElement lineP = MappingAnalysis.getParentLine(new LineFilter(), elementRemoved);
                                ITree lineTree = MappingAnalysis.getFormattedTreeFromControlFlow(lineP);
                                repairPatterns.incrementFeatureCounterInstance(WRAPS_METHOD,
                                        new PatternInstance(WRAPS_METHOD, operation, ctConstructor, elementRemoved,
                                                lineP, lineTree, new PropertyPair("Old", "VarRead"),
                                                new PropertyPair("New", "Constructor")));
                            }
                        }
                        if (elementRemoved instanceof CtAssignment) {

                            if (constructorArguments.contains(((CtAssignment) elementRemoved).getAssignment())) {

                                CtElement lineP = MappingAnalysis.getParentLine(new LineFilter(), elementRemoved);
                                ITree lineTree = MappingAnalysis.getFormattedTreeFromControlFlow(lineP);

                                repairPatterns.incrementFeatureCounterInstance(WRAPS_METHOD,
                                        new PatternInstance(WRAPS_METHOD, operation, ctConstructor,
                                                // ((CtAssignment) elementRemoved).getAssigned()
                                                elementRemoved, lineP, lineTree, new PropertyPair("Old", "Assignment"),
                                                new PropertyPair("New", "Constructor")));
                            }
                        }
                    }
                }

                for (CtExpression ctExpression : constructorArguments) {
                    if (ctExpression.getMetadata("isMoved") != null) {
                        // Operation is an Insert
                        // TODO:
                        List<CtElement> suspLeft = MappingAnalysis.getTreeLeftMovedFromRight(diff, ctConstructor);
                        if (suspLeft == null || suspLeft.isEmpty())
                            return;

                        CtElement lineP = MappingAnalysis.getParentLine(new LineFilter(), suspLeft.get(0));
                        ITree lineTree = MappingAnalysis.getFormattedTreeFromControlFlow(lineP);
                        if (RepairPatternUtils.getIsInvocationInStatemnt(diff, lineP, ctConstructor)) {

                            if (RepairPatternUtils.getElementInOld(diff, ctExpression) != null &&
                                    (RepairPatternUtils.getElementInOld(diff, ctExpression).getParent() instanceof CtConstructorCall ||
                                            RepairPatternUtils.getElementInOld(diff, ctExpression).getParent() instanceof CtInvocation)
                                    && RepairPatternUtils.getElementInOld(diff, ctExpression).getParent() !=
                                    RepairPatternUtils.getElementInOld(diff, ctConstructor.getParent())) {
                            } else
                                repairPatterns.incrementFeatureCounterInstance(WRAPS_METHOD,
                                        new PatternInstance(WRAPS_METHOD, operation, ctConstructor, ctExpression, lineP,
                                                lineTree, new PropertyPair("Old", "MovedExpression"),
                                                new PropertyPair("New", "Constructor")));
                        }
                    }
                }
            } else {
                if (operation instanceof DeleteOperation) {
                    CtConstructorCall ctConstructor = (CtConstructorCall) operation.getSrcNode();
                    CtStatement statementParent = ctConstructor.getParent(new TypeFilter<>(CtStatement.class));

                    if (statementParent.getMetadata("delete") == null) {
                        List<CtExpression> invocationArguments = ctConstructor.getArguments();

                        for (CtExpression ctExpression : invocationArguments) {
                            if (ctExpression.getMetadata("isMoved") != null
                                    && ctExpression.getMetadata("movingSrc") != null) {

                                if (whetherconsindertheconstructorunrap(ctExpression, ctConstructor) &&
                                        RepairPatternUtils.getIsMovedExpressionInStatement(diff, statementParent, ctExpression)) {
                                    CtElement lineP = MappingAnalysis.getParentLine(new LineFilter(),
                                            operation.getSrcNode());
                                    ITree lineTree = MappingAnalysis.getFormattedTreeFromControlFlow(lineP);

                                    repairPatterns.incrementFeatureCounterInstance(UNWRAP_METHOD, new PatternInstance(
                                            UNWRAP_METHOD, operation, statementParent, ctConstructor, lineP, lineTree));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean whetherconsindertheconstructorunrap(CtExpression oldexpression, CtConstructorCall ctCall) {
        ITree treenewexpression = MappingAnalysis.getRightFromLeftNodeMapped(diff, oldexpression);

        if (treenewexpression == null)
            return false;

        CtElement newexpression = (CtElement) treenewexpression.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);

        CtStatement oldparent = oldexpression.getParent(new LineFilter());
        CtStatement newparent = newexpression.getParent(new LineFilter());

        return (RepairPatternUtils.getElementInOld(diff, newparent) != null &&
                RepairPatternUtils.getElementInOld(diff, newparent) == oldparent) ||
                (oldparent instanceof CtConstructorCall && newparent instanceof CtAssignment);
    }

    private boolean whetherconsinderthemethodunrap(CtExpression oldexpression, CtInvocation ctInvocation) {
        ITree treenewexpression = MappingAnalysis.getRightFromLeftNodeMapped(diff, oldexpression);

        if (treenewexpression == null)
            return false;

        CtElement newexpression = (CtElement) treenewexpression.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);

        CtStatement oldparent = oldexpression.getParent(new LineFilter());
        CtStatement newparent = newexpression.getParent(new LineFilter());

        return (RepairPatternUtils.getElementInOld(diff, newparent) != null && RepairPatternUtils.getElementInOld(diff, newparent) == oldparent) || (oldparent instanceof CtInvocation && newparent instanceof CtAssignment);
    }

    private void detectWrapsLoop(Operation operation, RepairPatterns repairPatterns) {
        if (operation instanceof InsertOperation) {
            CtElement ctElement = operation.getSrcNode();
            SpoonHelper.printInsertOrDeleteOperation(ctElement.getFactory().getEnvironment(), ctElement, operation);

            List<CtLoop> loopList = ctElement.getElements(new TypeFilter<>(CtLoop.class));
            for (CtLoop ctLoop : loopList) {
                if ((ctLoop instanceof CtFor && RepairPatternUtils.isNewFor((CtFor) ctLoop)) ||
                        (ctLoop instanceof CtForEach && RepairPatternUtils.isNewForEach((CtForEach) ctLoop)) ||
                        (ctLoop instanceof CtWhile && RepairPatternUtils.isNewWhile((CtWhile) ctLoop))) {
                    if (ctLoop.getBody() instanceof CtBlock) {
                        CtBlock bodyBlock = (CtBlock) ctLoop.getBody();
                        List susp = RepairPatternUtils.getIsThereOldStatementInStatementList(diff, bodyBlock.getStatements());
                        if (bodyBlock != null && !susp.isEmpty()) {

                            CtElement lineP = MappingAnalysis.getParentLine(new LineFilter(), (CtElement) susp.get(0));
                            ITree lineTree = MappingAnalysis.getFormattedTreeFromControlFlow(lineP);

                            repairPatterns.incrementFeatureCounterInstance(WRAPS_LOOP,
                                    new PatternInstance(WRAPS_LOOP, operation, ctLoop, susp, lineP, lineTree));
                        } else { // try to find an update inside the body of the loop
                            for (Operation operationAux : this.operations) {
                                if (operationAux instanceof UpdateOperation) {
                                    CtElement ctElementDst = operationAux.getDstNode();
                                    CtLoop ctLoopParent = ctElementDst.getParent(new TypeFilter<>(CtLoop.class));
                                    if (ctLoopParent != null && ctLoopParent == ctLoop) {

                                        CtElement lineP = MappingAnalysis.getParentLine(new LineFilter(), operationAux.getSrcNode());
                                        ITree lineTree = MappingAnalysis.getFormattedTreeFromControlFlow(lineP);

                                        // loop add happens for the statement, not any specific nodes of it

                                        if (!RepairPatternUtils.isOldStatementInLoop(lineP))
                                            repairPatterns.incrementFeatureCounterInstance(WRAPS_LOOP,
                                                    new PatternInstance(WRAPS_LOOP, operation, ctLoop,
                                                            lineP, lineP, lineTree));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}
