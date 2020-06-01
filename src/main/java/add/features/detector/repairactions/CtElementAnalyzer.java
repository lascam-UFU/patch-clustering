package add.features.detector.repairactions;

import add.entities.RepairActions;
import spoon.reflect.code.CtAbstractInvocation;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtCase;
import spoon.reflect.code.CtConditional;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtFor;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtLoop;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtThrow;
import spoon.reflect.code.CtTry;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtModifiable;
import spoon.reflect.declaration.CtNamedElement;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtVariable;
import spoon.reflect.path.CtRole;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.CtInheritanceScanner;
import spoon.reflect.visitor.CtScanner;

/**
 * Created by tdurieux
 */
public class CtElementAnalyzer {

    public enum ACTION_TYPE {
        ADD("Add"),
        UPDATE("Change"),
        DELETE("Rem");

        private String name;

        ACTION_TYPE(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private CtElement element;
    private CtElement dstElement;

    public CtElementAnalyzer(CtElement e, CtElement dst) {
        this.element = e;
        this.dstElement = dst;
    }

    public CtElementAnalyzer(CtElement element) {
        this(element, null);
    }

    public RepairActions analyze(final RepairActions output, final ACTION_TYPE actionType) {
        if (actionType == ACTION_TYPE.UPDATE) {
            element.accept(new CtInheritanceScanner() {
                @Override
                public <T> void visitCtBinaryOperator(CtBinaryOperator<T> e) {
                    CtIf ctIf = e.getParent(CtIf.class);
                    if (ctIf != null && (e.equals(ctIf.getCondition()) || e.hasParent(ctIf.getCondition()))) {
                        output.incrementFeatureCounter("condExpMod", e);
                    }
                    super.visitCtBinaryOperator(e);
                }

                @Override
                public <T> void visitCtMethod(CtMethod<T> e) {
                    if (!e.getSimpleName().equals(((CtNamedElement) dstElement).getSimpleName())) {
                        output.incrementFeatureCounter("mdRen", e);
                    }
                    if (e.getModifiers().size() != ((CtModifiable) dstElement).getModifiers().size() ||
                            !e.getModifiers().containsAll(((CtModifiable) dstElement).getModifiers())) {
                        output.incrementFeatureCounter("mdModChange", e);
                    }
                    super.visitCtMethod(e);
                }

                @Override
                public <T> void scanCtAbstractInvocation(CtAbstractInvocation<T> e) {
                    if (dstElement instanceof CtAbstractInvocation) {
                        if (e.getArguments().size() > ((CtAbstractInvocation) dstElement).getArguments().size()) {
                            output.incrementFeatureCounter("mcParRem", e);
                        } else if (e.getArguments().size() < ((CtAbstractInvocation) dstElement).getArguments().size()) {
                            output.incrementFeatureCounter("mcParAdd", e);
                        }
                    }
                    super.scanCtAbstractInvocation(e);
                }

                @Override
                public <T> void visitCtConditional(CtConditional<T> e) {
                    if (!(dstElement instanceof CtConditional)) {
                        output.incrementFeatureCounter("condBranRem", e);
                    }
                    super.visitCtConditional(e);
                }

                @Override
                public <T> void scanCtVariable(CtVariable<T> e) {
                    if (e.getModifiers().size() != ((CtModifiable) dstElement).getModifiers().size() ||
                            !e.getModifiers().containsAll(((CtModifiable) dstElement).getModifiers())) {
                        output.incrementFeatureCounter("varModChange", e);
                    }
                    super.scanCtVariable(e);
                }

                @Override
                public <T> void scanCtType(CtType<T> type) {
                    if (!type.getSimpleName().equals(((CtNamedElement) dstElement).getSimpleName())) {
                        //output.incrementFeatureCounter("tyRen");
                    }
                    super.scanCtType(type);
                }

                @Override
                public <T> void visitCtConstructorCall(CtConstructorCall<T> e) {
                    output.incrementFeatureCounter("objInstMod", e);
                    super.visitCtConstructorCall(e);
                }

                @Override
                public <T> void scanCtExpression(CtExpression<T> expression) {
                    if (!(expression instanceof CtConditional) && dstElement instanceof CtConditional) {
                        output.incrementFeatureCounter("condBranIfElseAdd", expression);
                    }
                    if (expression.getRoleInParent() == CtRole.ARGUMENT && expression.getParent().getMetadata("new") == null) {
                        output.incrementFeatureCounter("mcParVal" + actionType.name, expression);
                    }
                    CtAssignment assignment = expression.getParent(CtAssignment.class);
                    if (assignment != null && assignment.getMetadata("isMoved") != null && expression.hasParent(assignment.getAssignment())) {
                        output.incrementFeatureCounter("assignExp" + actionType.name, expression);
                    }
                    CtLocalVariable localVariable = expression.getParent(CtLocalVariable.class);
                    if (localVariable != null && localVariable.getMetadata("new") == null && localVariable.getMetadata("delete") == null) {
                        output.incrementFeatureCounter("assignExp" + actionType.name, expression);
                    }
                    CtReturn ctReturn = expression.getParent(CtReturn.class);
                    if (ctReturn != null && ctReturn.getMetadata("new") == null && ctReturn.getMetadata("delete") == null) {
                        output.incrementFeatureCounter("retExpChange", ctReturn);
                    }

                    CtFor ctFor = expression.getParent(CtFor.class);
                    if (ctFor != null && ctFor.getMetadata("new") == null) {
                        if (ctFor.getForInit() != null && !ctFor.getForInit().isEmpty() && expression.hasParent(ctFor.getForInit().get(0))) {
                            output.incrementFeatureCounter("loopInitChange", expression);
                        } else if (expression.hasParent(ctFor.getExpression())) {
                            output.incrementFeatureCounter("loopCondChange", expression);
                        }
                    }
                    super.scanCtExpression(expression);
                }

                @Override
                public <T> void visitCtParameter(CtParameter<T> e) {
                    if (e.getParent().getMetadata("new") == null) {
                        output.incrementFeatureCounter("mdParRem", e);
                    }
                    super.visitCtParameter(e);
                }

                @Override
                public <T> void visitCtTypeReference(CtTypeReference<T> e) {
                    if ((e.getRoleInParent() == CtRole.TYPE || e.getRoleInParent() == CtRole.MULTI_TYPE)
                            && e.getMetadata("update") != null) {
                        if (e.getParent() instanceof CtMethod) {
                            output.incrementFeatureCounter("mdRetTyChange", e);
                        }
                        if (e.getParent() instanceof CtVariable) {
                            output.incrementFeatureCounter("varTyChange", e);
                            if (e.getParent() instanceof CtParameter) {
                                output.incrementFeatureCounter("mdParTyChange", e);
                            }
                        }
                    } else if (e.getRoleInParent() == CtRole.INTERFACE) {
                        output.incrementFeatureCounter("tyImpInterf", e);
                    }
                    super.visitCtTypeReference(e);
                }

                @Override
                public <T> void visitCtInvocation(CtInvocation<T> e) {
                    output.incrementFeatureCounter("mcRepl", e);
                    super.visitCtInvocation(e);
                }

                @Override
                public <T> void visitCtFieldRead(CtFieldRead<T> e) {
                    output.incrementFeatureCounter("varReplVar", e);
                    super.visitCtFieldRead(e);
                }

                @Override
                public <T> void visitCtVariableRead(CtVariableRead<T> e) {
                    output.incrementFeatureCounter("varReplVar", e);
                    super.visitCtVariableRead(e);
                }
            });
            return output;
        }
        element.accept(new CtScanner() {
            @Override
            public void scan(CtElement element) {
                if (element != null && element.getMetadata("isMoved") == null) {
                    super.scan(element);
                }
            }

            @Override
            protected void enter(CtElement e) {
                e.accept(new CtInheritanceScanner() {
                    @Override
                    public <T, A extends T> void visitCtAssignment(CtAssignment<T, A> e) {
                        output.incrementFeatureCounter("assign" + actionType.name, e);
                        super.visitCtAssignment(e);
                    }

                    @Override
                    public void visitCtIf(CtIf e) {
                        if (actionType == ACTION_TYPE.DELETE) {
                            output.incrementFeatureCounter("condBran" + actionType.name, e);
                        } else if (e.getElseStatement() != null
                                && e.getElseStatement().getMetadata("isMoved") == null) {
                            output.incrementFeatureCounter("condBranIfElse" + actionType.name, e);
                        } else {
                            output.incrementFeatureCounter("condBranIf" + actionType.name, e);
                        }
                        super.visitCtIf(e);
                    }

                    @Override
                    public <R> void visitCtBlock(CtBlock<R> e) {
                        if (e.getRoleInParent() == CtRole.ELSE) {
                            if (actionType == ACTION_TYPE.DELETE) {
                                output.incrementFeatureCounter("condBran" + actionType.name, e);
                            } else {
                                if (e.getParent().getMetadata("new") == null) {
                                    output.incrementFeatureCounter("condBranElseAdd", e);
                                } else {
                                    output.incrementFeatureCounter("condBranIfElse" + actionType.name, e);
                                }
                            }
                        }
                        super.visitCtBlock(e);
                    }

                    @Override
                    public <T> void visitCtConditional(CtConditional<T> e) {
                        if (actionType == ACTION_TYPE.DELETE) {
                            output.incrementFeatureCounter("condBran" + actionType.name, e);
                        } else {
                            output.incrementFeatureCounter("condBranIfElse" + actionType.name, e);
                        }
                        super.visitCtConditional(e);
                    }

                    @Override
                    public <E> void visitCtCase(CtCase<E> e) {
                        if (actionType == ACTION_TYPE.DELETE) {
                            output.incrementFeatureCounter("condBranRem", e);
                        } else if (actionType == ACTION_TYPE.ADD) {
                            output.incrementFeatureCounter("condBranCaseAdd", e);
                        }
                        super.visitCtCase(e);
                    }

                    @Override
                    public <T> void visitCtBinaryOperator(CtBinaryOperator<T> expression) {
                        if (actionType == ACTION_TYPE.DELETE) {
                            CtIf ctIf = expression.getParent(CtIf.class);
                            if (ctIf != null && ctIf.getMetadata("new") == null && expression.hasParent(ctIf.getCondition())) {
                                output.incrementFeatureCounter("condExpRed", e);
                            }
                        } else if (actionType == ACTION_TYPE.ADD) {
                            CtIf ctIf = expression.getParent(CtIf.class);
                            if (ctIf != null && ctIf.getMetadata("new") == null) {
                                if (expression.hasParent(ctIf.getCondition())) {
                                    output.incrementFeatureCounter("condExpExpand", e);
                                }
                            }
                        }
                        super.visitCtBinaryOperator(expression);
                    }

                    @Override
                    public void scanCtLoop(CtLoop loop) {
                        output.incrementFeatureCounter("loop" + actionType.name, e);
                        super.scanCtLoop(loop);
                    }

                    @Override
                    public <T> void visitCtLiteral(CtLiteral<T> e) {
                        CtVariable parent = e.getParent(CtVariable.class);
                        if (parent != null && parent.getMetadata("new") == null) {
                            output.incrementFeatureCounter("objInstMod", e);
                        }
                        super.visitCtLiteral(e);
                    }

                    @Override
                    public <T> void visitCtInvocation(CtInvocation<T> e) {
                        output.incrementFeatureCounter("mc" + actionType.name, e);
                        super.visitCtInvocation(e);
                    }

                    @Override
                    public <R> void visitCtReturn(CtReturn<R> e) {
                        if (actionType == ACTION_TYPE.ADD) {
                            output.incrementFeatureCounter("retBranch" + actionType.name, e);
                        } else if (actionType == ACTION_TYPE.DELETE) {
                            output.incrementFeatureCounter("ret" + actionType.name, e);
                        }
                        super.visitCtReturn(e);
                    }

                    @Override
                    public void visitCtThrow(CtThrow e) {
                        output.incrementFeatureCounter("exThrows" + actionType.name, e);
                        super.visitCtThrow(e);
                    }

                    @Override
                    public <T> void visitCtMethod(CtMethod<T> e) {
                        if (e.hasAnnotation(Override.class)) {
                            output.incrementFeatureCounter("mdOverride", e);
                        } else {
                            output.incrementFeatureCounter("md" + actionType.name, e);
                        }

                        super.visitCtMethod(e);
                    }

                    @Override
                    public <T> void visitCtParameter(CtParameter<T> e) {
                        if (e.getParent().getMetadata("new") == null) {
                            output.incrementFeatureCounter("mdPar" + actionType.name, e);
                        }
                        super.visitCtParameter(e);
                    }

                    @Override
                    public <T> void visitCtTypeReference(CtTypeReference<T> e) {
                        if ((e.getRoleInParent() == CtRole.TYPE || e.getRoleInParent() == CtRole.MULTI_TYPE) && e.getMetadata("new") == null) {
                            if (e.getParent() instanceof CtVariable) {
                                output.incrementFeatureCounter("varTyChange", e);
                            }
                        } else if (e.getRoleInParent() == CtRole.INTERFACE) {
                            output.incrementFeatureCounter("tyImpInterf", e);
                        }
                        super.visitCtTypeReference(e);
                    }

                    @Override
                    public void visitCtTry(CtTry e) {
                        output.incrementFeatureCounter("exTryCatch" + actionType.name, e);
                        super.visitCtTry(e);
                    }

                    @Override
                    public <T> void visitCtLocalVariable(CtLocalVariable<T> e) {
                        output.incrementFeatureCounter("var" + actionType.name, e);
                        if (e.getDefaultExpression() != null) {
                            output.incrementFeatureCounter("assign" + actionType.name, e);
                        }
                        super.visitCtLocalVariable(e);
                    }

                    @Override
                    public <T> void scanCtType(CtType<T> type) {
                        if (actionType == ACTION_TYPE.ADD) {
                            output.incrementFeatureCounter("tyAdd", e);
                        }
                        super.scanCtType(type);
                    }

                    @Override
                    public <T> void visitCtConstructorCall(CtConstructorCall<T> e) {
                        output.incrementFeatureCounter("objInst" + actionType.name, e);
                        super.visitCtConstructorCall(e);
                    }

                    @Override
                    public <T> void scanCtExpression(CtExpression<T> expression) {
                        if (expression.getRoleInParent() == CtRole.ARGUMENT && expression.getParent().getMetadata("new") == null) {
                            output.incrementFeatureCounter("mcPar" + actionType.name, e);
                        }
                        CtInvocation ctInvocation = expression.getParent(CtInvocation.class);
                        if (ctInvocation != null && ctInvocation.getMetadata("new") == null) {
                            output.incrementFeatureCounter("mcParValChange", e);
                        }
                        CtConstructorCall ctConstructorCall = expression.getParent(CtConstructorCall.class);
                        if (ctConstructorCall != null && ctConstructorCall.getMetadata("new") == null) {
                            output.incrementFeatureCounter("mcParValChange", e);
                        }
                        super.scanCtExpression(expression);
                    }
                });
            }

        });
        return output;
    }

}
