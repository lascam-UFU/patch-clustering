package add.features.detector.repairpatterns;

import add.entities.RepairPatterns;
import add.features.detector.spoon.RepairPatternUtils;
import add.features.detector.spoon.SpoonHelper;
import gumtree.spoon.diff.operations.DeleteOperation;
import gumtree.spoon.diff.operations.InsertOperation;
import gumtree.spoon.diff.operations.Operation;
import gumtree.spoon.diff.operations.UpdateOperation;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtCase;
import spoon.reflect.code.CtConditional;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.List;

/**
 * Created by fermadeiral
 */
public class ConditionalBlockDetector extends AbstractPatternDetector {

    public ConditionalBlockDetector(List<Operation> operations) {
        super(operations);
    }

    @Override
    public void detect(RepairPatterns repairPatterns) {
        for (Operation operation : this.operations) {
            if (operation instanceof InsertOperation || operation instanceof DeleteOperation) {
                CtElement ctElement = operation.getSrcNode();
                SpoonHelper.printInsertOrDeleteOperation(ctElement.getFactory().getEnvironment(), ctElement, operation);

                boolean wasPatternFound;
                List<CtIf> ifList = ctElement.getElements(new TypeFilter<>(CtIf.class));
                for (CtIf ctIf : ifList) {
                    wasPatternFound = false;
                    if (RepairPatternUtils.isNewIf(ctIf)) {
                        CtBlock thenBlock = ctIf.getThenStatement();
                        if (thenBlock != null) {
                            if (operation instanceof InsertOperation &&
                                    RepairPatternUtils.isThereOnlyNewAndMovedStatementsInStatementList(thenBlock.getStatements())) {
                                wasPatternFound = true;
                            }
                            if (operation instanceof DeleteOperation &&
                                    RepairPatternUtils.isThereOnlyRemovedAndMovedAwayStatementsInRemovedIf(ctIf)) {
                                wasPatternFound = true;
                            }
                            if (wasPatternFound) {
                                String pattern = this.getVariant(thenBlock, operation);
                                if (!pattern.isEmpty()) {
                                    repairPatterns.incrementFeatureCounter(pattern, operation);
                                }
                            }
                        }
                    }
                }

                List<CtBlock> blockList = ctElement.getElements(new TypeFilter<>(CtBlock.class));
                for (CtBlock ctBlock : blockList) {
                    if (ctBlock.getMetadata("new") != null) {
                        if (ctBlock.getParent() instanceof CtIf) {
                            CtIf ctIfParent = (CtIf) ctBlock.getParent();
                            CtBlock elseBlock = ctIfParent.getElseStatement();
                            if (ctBlock == elseBlock) {
                                if (!ctBlock.isImplicit()) {
                                    if (!RepairPatternUtils.isThereOldStatementInStatementList(elseBlock.getStatements())) {
                                        String pattern = this.getVariant(ctBlock, operation);
                                        if (!pattern.isEmpty()) {
                                            repairPatterns.incrementFeatureCounter(pattern, operation);
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
                        if (thenExpression.getMetadata("new") != null &&
                                elseExpression.getMetadata("new") != null) {
                            if (operation instanceof InsertOperation) {
                                repairPatterns.incrementFeatureCounter("condBlockOthersAdd", operation);
                            } else {
                                repairPatterns.incrementFeatureCounter("condBlockRem", operation);
                            }
                        }
                    }
                }

                List<CtCase> caseList = ctElement.getElements(new TypeFilter<>(CtCase.class));
                for (CtCase ctCase : caseList) {
                    if (ctCase.getMetadata("new") != null) {
                        List<CtStatement> statements = ctCase.getStatements();
                        if (statements.size() > 0 && !RepairPatternUtils.isThereOldStatementInStatementList(statements)) {
                            String pattern = this.getVariant(ctCase, operation);
                            if (!pattern.isEmpty()) {
                                repairPatterns.incrementFeatureCounter(pattern, operation);
                            }
                        }
                    }
                }
            } else {
                if (operation instanceof UpdateOperation) {
                    CtElement srcNode = operation.getSrcNode();
                    List<CtConditional> conditionalList = srcNode.getElements(new TypeFilter<>(CtConditional.class));
                    for (CtConditional ctConditional : conditionalList) {
                        if (ctConditional.getMetadata("new") != null) {
                            CtExpression thenExpression = ctConditional.getThenExpression();
                            CtExpression elseExpression = ctConditional.getElseExpression();
                            if (thenExpression.getMetadata("new") != null &&
                                    elseExpression.getMetadata("new") != null) {
                                repairPatterns.incrementFeatureCounter("condBlockRem", operation);
                            }
                        }
                    }
                    CtElement dstNode = operation.getDstNode();
                    conditionalList = dstNode.getElements(new TypeFilter<>(CtConditional.class));
                    for (CtConditional ctConditional : conditionalList) {
                        if (ctConditional.getMetadata("new") != null) {
                            CtExpression thenExpression = ctConditional.getThenExpression();
                            CtExpression elseExpression = ctConditional.getElseExpression();
                            if (thenExpression.getMetadata("new") != null &&
                                    elseExpression.getMetadata("new") != null) {
                                repairPatterns.incrementFeatureCounter("condBlockOthersAdd", operation);
                            }
                        }
                    }
                }
            }
        }
    }

    private String getVariant(CtElement ctElement, Operation operation) {
        if (operation instanceof InsertOperation) {
            boolean isThereReturn = RepairPatternUtils.isThereReturnInIfOrCase(ctElement);
            if (isThereReturn) {
                return "condBlockRetAdd";
            }
            boolean isThereThrow = RepairPatternUtils.isThereThrowInIfOrCase(ctElement);
            if (isThereThrow) {
                return "condBlockExcAdd";
            }
            if (!isThereReturn && !isThereThrow) {
                return "condBlockOthersAdd";
            }
        } else {
            return "condBlockRem";
        }
        return "";
    }

}
