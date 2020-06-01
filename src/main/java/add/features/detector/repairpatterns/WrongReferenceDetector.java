package add.features.detector.repairpatterns;

import add.entities.PatternInstance;
import add.entities.PropertyPair;
import add.entities.RepairPatterns;
import add.features.detector.spoon.MappingAnalysis;
import add.features.detector.spoon.RepairPatternUtils;
import add.features.detector.spoon.SpoonHelper;
import add.features.detector.spoon.filter.NullCheckFilter;
import add.main.Config;
import com.github.gumtreediff.tree.ITree;
import gumtree.spoon.builder.SpoonGumTreeBuilder;
import gumtree.spoon.diff.operations.DeleteOperation;
import gumtree.spoon.diff.operations.InsertOperation;
import gumtree.spoon.diff.operations.Operation;
import gumtree.spoon.diff.operations.UpdateOperation;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtAbstractInvocation;
import spoon.reflect.code.CtArrayAccess;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtConditional;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtFieldWrite;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtTargetedExpression;
import spoon.reflect.code.CtTypeAccess;
import spoon.reflect.code.CtVariableAccess;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.code.CtVariableWrite;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtVariable;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.LineFilter;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tdurieux
 */
public class WrongReferenceDetector extends AbstractPatternDetector {

    public static final String WRONG_VAR_REF = "wrongVarRef";
    public static final String WRONG_METHOD_REF = "wrongMethodRef";
    public static final String UNWRAP_METHOD = "unwrapMethod";
    public static final String WRAPS_METHOD = "wrapsMethod";
    public static final String CONST_CHANGE = "constChange";
    public static final String WRAPS_IF_ELSE = "wrapsIfElse";
    public static final String UNWRAP_IF_ELSE = "unwrapIfElse";
    public static final String MISS_NULL_CHECK_N = "missNullCheckN";
    public static final String MISS_NULL_CHECK_P = "missNullCheckP";

    private Config config;

    public WrongReferenceDetector(Config config, List<Operation> operations) {
        super(operations);
        this.config = config;
    }

    @Override
    public void detect(RepairPatterns repairPatterns) {
        List<CtElement> alreadyConsidered = new ArrayList<>();

        for (int i = 0; i < operations.size(); i++) {
            Operation operation = operations.get(i);
            if (operation instanceof DeleteOperation) {
                CtElement srcNode = operation.getSrcNode();
                if (srcNode instanceof CtVariableAccess ||
                        srcNode instanceof CtInvocation ||
                        srcNode instanceof CtConstructorCall ||
                        srcNode instanceof CtTypeAccess) {
                    if (srcNode.getMetadata("delete") != null) {
                        CtElement statementParent = srcNode.getParent(CtStatement.class);
                        if (statementParent != null && statementParent.getMetadata("delete") == null) {
                            CtElement newElementReplacementOfTheVar = null;
                            boolean wasVariableWrapped = false;
                            boolean wasVarUnWrapted = false;
                            for (int j = 0; j < operations.size(); j++) {
                                Operation operation2 = operations.get(j);
                                if (operation2 instanceof InsertOperation) {
                                    CtElement node2 = operation2.getSrcNode();
                                    if (node2 instanceof CtInvocation || node2 instanceof CtConstructorCall) {
                                        if (((InsertOperation) operation2).getParent() != null) {
                                            if (srcNode.getParent() == ((InsertOperation) operation2).getParent()) {
                                                wasVariableWrapped = whetherVarWrapped(srcNode, node2);
                                                if (wasVariableWrapped) {
                                                    alreadyConsidered.add(node2);
                                                }
                                            }
                                        }
                                    }

                                    if (node2 instanceof CtVariableAccess || node2 instanceof CtTypeAccess ||
                                            node2 instanceof CtInvocation || node2 instanceof CtConstructorCall) {
                                        if (srcNode instanceof CtInvocation || srcNode instanceof CtConstructorCall) {
                                            if (((InsertOperation) operation2).getParent() != null) {
                                                if (srcNode.getParent() == ((InsertOperation) operation2).getParent()) {
                                                    wasVarUnWrapted = whethervarunwrapped(srcNode, node2);
                                                    if (wasVarUnWrapted) {
                                                        alreadyConsidered.add(node2);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            for (int j = 0; j < operations.size(); j++) {
                                Operation operation2 = operations.get(j);
                                if (operation2 instanceof InsertOperation) {
                                    CtElement node2 = operation2.getSrcNode();

                                    if (srcNode.getParent() == ((InsertOperation) operation2).getParent() && !alreadyConsidered.contains(node2)) {
                                        newElementReplacementOfTheVar = node2;
                                        alreadyConsidered.add(node2);
                                        break;
                                    }
                                }
                            }

                            if (srcNode instanceof CtVariableAccess) {
                                if (whetherVariableDeclarationDeleted((CtVariableAccess) srcNode)) {
                                    continue;
                                }
                            }

                            if (!wasVariableWrapped && !wasVarUnWrapted) {
                                CtElement susp = operation.getSrcNode();
                                CtElement patch = null;

                                ITree parentRight = MappingAnalysis.getParentInRight(diff, operation.getAction());
                                if (parentRight != null) {
                                    patch = (CtElement) parentRight.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                                }

                                CtElement parentLine = MappingAnalysis.getParentLine(new LineFilter(), susp);
                                ITree lineTree = MappingAnalysis.getFormattedTreeFromControlFlow(parentLine);

                                PropertyPair[] metadata = null;

                                PropertyPair propertyOldElemet = new PropertyPair("Old", "Removed_"
                                        + srcNode.getClass().getSimpleName().replace("Ct", "").replace("Impl", ""));

                                String replaceElementType = "";
                                // if we have the element that has be inserted
                                if (newElementReplacementOfTheVar != null) {
                                    PropertyPair propertyNewElement = new PropertyPair("New",
                                            "Added_" + newElementReplacementOfTheVar.getClass().getSimpleName()
                                                    .replace("Ct", "").replace("Impl", ""));
                                    replaceElementType = newElementReplacementOfTheVar.getClass().getSimpleName()
                                            .replace("Ct", "").replace("Impl", "");

                                    metadata = new PropertyPair[]{propertyOldElemet, propertyNewElement};
                                } else {
                                    metadata = new PropertyPair[]{propertyOldElemet};
                                }

                                Boolean whetherConsiderInitial = false;
                                if (metadata.length == 2) {
                                    if (newElementReplacementOfTheVar instanceof CtFieldRead) {
                                        if (whetherVariableNewlyDeclared((CtFieldRead) newElementReplacementOfTheVar)) {
                                            continue;
                                        }
                                    }

                                    if (newElementReplacementOfTheVar instanceof CtFieldWrite) {
                                        if (whetherVariableNewlyDeclared((CtFieldWrite) newElementReplacementOfTheVar)) {
                                            continue;
                                        }
                                    }

                                    if (newElementReplacementOfTheVar instanceof CtVariableRead) {
                                        // Martin comments this if/continue because it breaks WrongReferenceDetectorTest.math64
                                        // to maximize backward compatibility
//                                        if (whethervariablenewlydeclared((CtVariableRead) newElementReplacementOfTheVar))
//                                            continue;
                                    }

                                    if (newElementReplacementOfTheVar instanceof CtVariableWrite) {
                                        if (whetherVariableNewlyDeclared((CtVariableWrite) newElementReplacementOfTheVar)) {
                                            continue;
                                        }
                                    }

                                    if (newElementReplacementOfTheVar instanceof CtInvocation) {
                                        if (whetherMethodNewlyDeclared((CtInvocation) newElementReplacementOfTheVar)) {
                                            continue;
                                        }
                                    }

                                    if (replaceElementType.equals("Invocation") || replaceElementType.equals("VariableRead")
                                            || replaceElementType.equals("FieldRead") || replaceElementType.equals("ConstructorCall")
                                            || replaceElementType.equals("Literal") || replaceElementType.equals("FieldWrite")
                                            || replaceElementType.equals("VariableWrite") || replaceElementType.equals("TypeAccess")) {
                                        whetherConsiderInitial = true;
                                    }
                                }

                                if (whetherConsiderInitial) {
                                    if (srcNode instanceof CtInvocation) {
                                        if (newElementReplacementOfTheVar instanceof CtInvocation &&
                                                ((CtInvocation) srcNode).getExecutable().getSimpleName().
                                                        equals(((CtInvocation) newElementReplacementOfTheVar).getExecutable().getSimpleName())
                                                && ((CtInvocation) srcNode).getArguments().size() == ((CtInvocation) newElementReplacementOfTheVar).getArguments().size()) {
                                        } else {
                                            if (newElementReplacementOfTheVar instanceof CtAbstractInvocation) {
                                                String srcCallMethodName = ((CtInvocation) srcNode).getExecutable().getSimpleName();
                                                String dstCallMethodName = ((CtAbstractInvocation) newElementReplacementOfTheVar).getExecutable().getSimpleName();

                                                if (!srcCallMethodName.equals(dstCallMethodName)) {
                                                    repairPatterns.incrementFeatureCounterInstance(WRONG_METHOD_REF,
                                                            new PatternInstance(WRONG_METHOD_REF, operation, patch, susp, parentLine,
                                                                    lineTree, new PropertyPair("Change", "differentMethodName")));
                                                } else {
                                                    repairPatterns.incrementFeatureCounterInstance(WRONG_METHOD_REF,
                                                            new PatternInstance(WRONG_METHOD_REF, operation, patch, susp, parentLine,
                                                                    lineTree, new PropertyPair("Change", "SameNameDifferentArgument")));
                                                }
                                            } else {
                                                repairPatterns.incrementFeatureCounterInstance(WRONG_METHOD_REF, new PatternInstance(WRONG_METHOD_REF, operation, patch, susp,
                                                        parentLine, lineTree, metadata));
                                            }
                                        }

                                        if (newElementReplacementOfTheVar instanceof CtInvocation) {
                                            List<CtExpression> invocationArgumentsold = new ArrayList<>();
                                            invocationArgumentsold = ((CtInvocation) srcNode).getArguments();

                                            List<CtExpression> invocationArgumentnew = new ArrayList<>();
                                            invocationArgumentnew = ((CtInvocation) newElementReplacementOfTheVar).getArguments();

                                            detectVarArgumentChange(invocationArgumentsold, invocationArgumentnew, repairPatterns, srcNode, newElementReplacementOfTheVar);
                                        }

                                        if (newElementReplacementOfTheVar instanceof CtConstructorCall) {
                                            List<CtExpression> invocationArgumentsold = new ArrayList<>();
                                            invocationArgumentsold = ((CtInvocation) srcNode).getArguments();

                                            List<CtExpression> invocationArgumentnew = new ArrayList<>();
                                            invocationArgumentnew = ((CtConstructorCall) newElementReplacementOfTheVar).getArguments();

                                            detectVarArgumentChange(invocationArgumentsold, invocationArgumentnew, repairPatterns, srcNode, newElementReplacementOfTheVar);
                                        }
                                    } else if (srcNode instanceof CtConstructorCall) {
                                        String[] namespace = ((CtConstructorCall) srcNode).getExecutable().getSignature().split("\\(")[0].split("\\.");
                                        String originalConstructorName = namespace[namespace.length - 1];

                                        String newconstructorname = "";
                                        if (newElementReplacementOfTheVar instanceof CtConstructorCall) {
                                            String[] namespacenew = ((CtConstructorCall) newElementReplacementOfTheVar).getExecutable().
                                                    getSignature().split("\\(")[0].split("\\.");
                                            newconstructorname = namespacenew[namespacenew.length - 1];
                                        }

                                        if (!(newElementReplacementOfTheVar instanceof CtConstructorCall) || !originalConstructorName.equals(newconstructorname)
                                            || ((CtConstructorCall) srcNode).getArguments().size() != ((CtConstructorCall) newElementReplacementOfTheVar).getArguments().size()) {
                                            if (newElementReplacementOfTheVar instanceof CtConstructorCall) {
                                                if (!originalConstructorName.equals(newconstructorname)) {
                                                    repairPatterns.incrementFeatureCounterInstance(WRONG_METHOD_REF,
                                                            new PatternInstance(WRONG_METHOD_REF, operation, patch, susp, parentLine,
                                                                    lineTree, new PropertyPair("Change", "differentMethodName")));
                                                } else {
                                                    repairPatterns.incrementFeatureCounterInstance(WRONG_METHOD_REF,
                                                            new PatternInstance(WRONG_METHOD_REF, operation, patch, susp, parentLine,
                                                                    lineTree, new PropertyPair("Change", "SameNamedifferentArgument")));
                                                }
                                            } else {
                                                repairPatterns.incrementFeatureCounterInstance(WRONG_METHOD_REF,
                                                        new PatternInstance(WRONG_METHOD_REF, operation, patch, susp,
                                                                parentLine, lineTree, metadata));
                                            }
                                        }

                                        if (newElementReplacementOfTheVar instanceof CtConstructorCall) {

                                            List<CtExpression> invocationArgumentsold = new ArrayList<>();
                                            invocationArgumentsold = ((CtConstructorCall) srcNode).getArguments();

                                            List<CtExpression> invocationArgumentnew = new ArrayList<>();
                                            invocationArgumentnew = ((CtConstructorCall) newElementReplacementOfTheVar).getArguments();

                                            detectVarArgumentChange(invocationArgumentsold, invocationArgumentnew, repairPatterns, srcNode, newElementReplacementOfTheVar);
                                        }

                                        if (newElementReplacementOfTheVar instanceof CtInvocation) {
                                            List<CtExpression> invocationArgumentsold = new ArrayList<>();
                                            invocationArgumentsold = ((CtConstructorCall) srcNode).getArguments();

                                            List<CtExpression> invocationArgumentnew = new ArrayList<>();
                                            invocationArgumentnew = ((CtInvocation) newElementReplacementOfTheVar).getArguments();

                                            detectVarArgumentChange(invocationArgumentsold, invocationArgumentnew, repairPatterns, srcNode, newElementReplacementOfTheVar);
                                        }
                                    } else {
                                        repairPatterns.incrementFeatureCounterInstance(WRONG_VAR_REF,
                                                new PatternInstance(WRONG_VAR_REF, operation, patch, susp, parentLine,
                                                        lineTree, metadata));
                                    }
                                }

                                if (!whetherConsiderInitial && metadata.length == 1) {
                                    if (susp.getParent() instanceof CtInvocation || susp.getParent() instanceof CtConstructorCall) {
                                        CtElement susp_parent = susp.getParent();
                                        if (!whetherParentChanged(susp_parent) && whetherargumentnumberdifferent(susp_parent)) {
                                            repairPatterns.incrementFeatureCounterInstance(WRONG_METHOD_REF,
                                                    new PatternInstance(WRONG_METHOD_REF, operation, patch, susp_parent,
                                                            parentLine, lineTree, new PropertyPair("Change", "SameNamedifferentArgument")));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (operation instanceof UpdateOperation) {
                CtElement srcNode = operation.getSrcNode();
                CtElement dstNode = operation.getDstNode();

                if (dstNode.getParent().getMetadata("new") != null ||
                        dstNode.getParent().getMetadata("isMoved") != null) {
                    continue;
                }
                if (srcNode.getParent().getMetadata("new") != null ||
                        srcNode.getParent().getMetadata("isMoved") != null) {
                    continue;
                }

                if (srcNode instanceof CtVariableAccess || srcNode instanceof CtTypeAccess) {
                    if (operation.getDstNode() instanceof CtVariableAccess || operation.getDstNode() instanceof CtTypeAccess) {
                        if (srcNode instanceof CtVariableAccess && operation.getDstNode() instanceof CtVariableAccess) {
                            if (whetherVariableDeclarationDeleted((CtVariableAccess) srcNode)) {
                                continue;
                            }
                        }

                        CtElement susp = operation.getSrcNode();
                        CtElement patch = null;

                        CtElement parentLine = MappingAnalysis.getParentLine(new LineFilter(), susp);
                        ITree lineTree = MappingAnalysis.getFormattedTreeFromControlFlow(parentLine);

                        ITree parentRight = MappingAnalysis.getParentInRight(diff, operation.getAction());
                        if (parentRight != null) {
                            patch = (CtElement) parentRight.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                        }

                        repairPatterns.incrementFeatureCounterInstance(WRONG_VAR_REF, new PatternInstance(WRONG_VAR_REF,
                                operation, patch, susp, parentLine, lineTree, new PropertyPair("Change", "Update_"
                                + srcNode.getClass().getSimpleName().replace("Ct", "").replace("Impl", ""))));
                    }
                }

                if (!(srcNode instanceof CtInvocation) && !(srcNode instanceof CtConstructorCall)) {
                    continue;
                }

                if (srcNode instanceof CtInvocation && srcNode.toString().startsWith("this(")) {
                    if (!(srcNode instanceof CtInvocation) || !dstNode.toString().startsWith("this("))
                        continue;
                }

                if (dstNode instanceof CtInvocation || dstNode instanceof CtConstructorCall) {

                    String srcCallMethodName;
                    CtTargetedExpression srcInvocation = (CtTargetedExpression) srcNode;
                    List<CtTypeReference> srcCallRealArguments;

                    if (srcNode instanceof CtInvocation) {
                        srcCallMethodName = ((CtInvocation) srcNode).getExecutable().getSimpleName();
                        srcCallRealArguments = ((CtInvocation) srcNode).getArguments();
                    } else {
                        String[] namespace = ((CtConstructorCall) srcNode).getType().getQualifiedName().split("\\(")[0].split("\\.");
                        srcCallMethodName = namespace[namespace.length - 1];
                        srcCallRealArguments = ((CtConstructorCall) srcNode).getArguments();
                    }

                    String dstCallMethodName;
                    CtElement dst = dstNode;
                    List<CtTypeReference> dstCallRealArguments;
                    CtTargetedExpression dstInvocation;

                    if (dstNode instanceof CtInvocation) {
                        dstCallMethodName = ((CtInvocation) dstNode).getExecutable().getSimpleName();
                        dstInvocation = (CtTargetedExpression) dstNode;
                        dstCallRealArguments = ((CtInvocation) dstNode).getArguments();
                    } else {
                        String[] namespace = ((CtConstructorCall) dstNode).getType().getQualifiedName().split("\\(")[0].split("\\.");
                        dstCallMethodName = namespace[namespace.length - 1];
                        dstInvocation = (CtTargetedExpression) dstNode;
                        dstCallRealArguments = ((CtConstructorCall) dstNode).getArguments();
                    }

                    if (!whetherMethodDefUpdated(srcNode, dstNode)) {
                        // if the target of the method call is different then it is not a wrong ref method
                        if (srcInvocation.getTarget() != null && dstInvocation.getTarget() != null && !srcInvocation.getTarget().equals(dstInvocation.getTarget())) {
                            if (srcInvocation.getTarget() instanceof CtVariableAccess) {
                                String variableName = ((CtVariableAccess) srcInvocation.getTarget()).getVariable().getSimpleName();
                                boolean foundTargetInArgument = false;
                                for (CtElement r : dstCallRealArguments) {
                                    if (r instanceof CtVariableAccess && ((CtVariableAccess) r).getVariable().getSimpleName().equals(variableName)) {
                                        foundTargetInArgument = true;
                                        break;
                                    }
                                }
                                // if target is moved to arguments consider that it is a wrong method reference
                                if (!foundTargetInArgument) {
                                    continue;
                                }
                            } else {
                                continue;
                            }
                        }

                        CtElement parentLine = MappingAnalysis.getParentLine(new LineFilter(), srcInvocation);
                        ITree lineTree = MappingAnalysis.getFormattedTreeFromControlFlow(parentLine);

                        if (!srcCallMethodName.equals(dstCallMethodName)) {
                            repairPatterns.incrementFeatureCounterInstance(WRONG_METHOD_REF,
                                    new PatternInstance(WRONG_METHOD_REF, operation, dst, srcInvocation, parentLine,
                                            lineTree, new PropertyPair("Change", "differentMethodName")));

                        } else {
                            if (srcCallRealArguments.size() != dstCallRealArguments.size())
                                repairPatterns.incrementFeatureCounterInstance(WRONG_METHOD_REF,
                                        new PatternInstance(WRONG_METHOD_REF, operation, dst, srcInvocation, parentLine,
                                                lineTree, new PropertyPair("Change", "sameNameDifferentArgument")));
                        }

                        List<CtExpression> invocationArgumentsold = new ArrayList<>();
                        if (srcNode instanceof CtInvocation) {
                            invocationArgumentsold = ((CtInvocation) srcNode).getArguments();
                        }
                        if (srcNode instanceof CtConstructorCall) {
                            invocationArgumentsold = ((CtConstructorCall) srcNode).getArguments();
                        }

                        List<CtExpression> invocationArgumentnew = new ArrayList<>();
                        if (dstNode instanceof CtInvocation) {
                            invocationArgumentnew = ((CtInvocation) dstNode).getArguments();
                        }
                        if (dstNode instanceof CtConstructorCall) {
                            invocationArgumentnew = ((CtConstructorCall) dstNode).getArguments();
                        }

                        detectVarArgumentChange(invocationArgumentsold, invocationArgumentnew, repairPatterns, srcNode, dstNode);
                    }
                }
            }
        }
    }

    public void detectVarArgumentChange(List<CtExpression> invocationArgumentsold, List<CtExpression> invocationArgumentsnew,
                                        RepairPatterns repairPatterns, CtElement source, CtElement destination) {

        for (Operation operation : diff.getAllOperations()) {
            this.detectWrapsMethod(invocationArgumentsold, invocationArgumentsnew, operation, repairPatterns, source, destination);
            this.detectWrapIfChange(invocationArgumentsold, invocationArgumentsnew, operation, repairPatterns, source, destination);
        }

        this.detectConstantChange(invocationArgumentsold, invocationArgumentsnew, repairPatterns, source, destination);
        this.detectNullCheckChange(invocationArgumentsold, invocationArgumentsnew, repairPatterns, source, destination);

        List<CtElement> alreadyconsidered = new ArrayList<CtElement>();
        for (int i = 0; i < diff.getAllOperations().size(); i++) {

            Operation operation = diff.getAllOperations().get(i);

            if (operation instanceof DeleteOperation) {
                Operation operationDelete = operation;
                CtElement srcNode = operationDelete.getSrcNode();

                if ((srcNode instanceof CtVariableAccess || srcNode instanceof CtInvocation ||
                        srcNode instanceof CtConstructorCall || srcNode instanceof CtTypeAccess) && invocationArgumentsold.contains(srcNode)
                        && srcNode.getParent() == source) {

                    CtElement newElementReplacementOfTheVar = null;
                    boolean wasVariableWrapped = false;
                    boolean wasVarUnWrapted = false;

                    for (int j = 0; j < diff.getAllOperations().size(); j++) {
                        Operation operation2 = diff.getAllOperations().get(j);
                        if (operation2 instanceof InsertOperation) {
                            CtElement node2 = operation2.getSrcNode();

                            if ((node2 instanceof CtInvocation || node2 instanceof CtConstructorCall) &&
                                    invocationArgumentsnew.contains(node2) && node2.getParent() == destination) {
                                wasVariableWrapped = whetherVarWrapped(srcNode, node2);

                                if (wasVariableWrapped)
                                    alreadyconsidered.add(node2);
                            }

                            if ((node2 instanceof CtVariableAccess || node2 instanceof CtTypeAccess ||
                                    node2 instanceof CtInvocation || node2 instanceof CtConstructorCall)
                                    && (srcNode instanceof CtInvocation || srcNode instanceof CtConstructorCall) &&
                                    invocationArgumentsnew.contains(node2) && node2.getParent() == destination) {
                                wasVarUnWrapted = whethervarunwrapped(srcNode, node2);
                                if (wasVarUnWrapted)
                                    alreadyconsidered.add(node2);
                            }
                        }
                    }

                    for (int j = 0; j < diff.getAllOperations().size(); j++) {
                        Operation operation2 = diff.getAllOperations().get(j);
                        if (operation2 instanceof InsertOperation) {
                            CtElement node2 = operation2.getSrcNode();

                            if (invocationArgumentsnew.contains(node2) && node2.getParent() == destination &&
                                    !alreadyconsidered.contains(node2)) {
                                newElementReplacementOfTheVar = node2;
                                alreadyconsidered.add(node2);
                                break;
                            }
                        }
                    }

                    if (srcNode instanceof CtVariableAccess) {
                        if (whetherVariableDeclarationDeleted((CtVariableAccess) srcNode)) {
                            continue;
                        }
                    }

                    if (!wasVariableWrapped && !wasVarUnWrapted && !whetherSameTypeWithSameName(srcNode, newElementReplacementOfTheVar)) {

                        CtElement susp = operationDelete.getSrcNode();
                        CtElement patch = null;

                        ITree parentRight = MappingAnalysis.getParentInRight(diff, operationDelete.getAction());
                        if (parentRight != null) {
                            patch = (CtElement) parentRight.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                        }

                        CtElement parentLine = MappingAnalysis.getParentLine(new LineFilter(), susp);
                        ITree lineTree = MappingAnalysis.getFormattedTreeFromControlFlow(parentLine);

                        PropertyPair[] metadata = null;
                        PropertyPair propertyOldElemet = new PropertyPair("Old", "Removed_"
                                + srcNode.getClass().getSimpleName().replace("Ct", "").replace("Impl", ""));

                        String replaceElementType = "";

                        if (newElementReplacementOfTheVar != null) {
                            PropertyPair propertyNewElement = new PropertyPair("New",
                                    "Added_" + newElementReplacementOfTheVar.getClass().getSimpleName()
                                            .replace("Ct", "").replace("Impl", ""));
                            replaceElementType = newElementReplacementOfTheVar.getClass().getSimpleName()
                                    .replace("Ct", "").replace("Impl", "");

                            metadata = new PropertyPair[]{propertyOldElemet, propertyNewElement};
                        } else
                            metadata = new PropertyPair[]{propertyOldElemet};

                        Boolean whetherConsiderInitial = false;

                        if (metadata.length == 2) {

                            if (newElementReplacementOfTheVar instanceof CtFieldRead) {

                                if (whetherVariableNewlyDeclared((CtFieldRead) newElementReplacementOfTheVar))
                                    continue;
                            }

                            if (newElementReplacementOfTheVar instanceof CtFieldWrite) {

                                if (whetherVariableNewlyDeclared((CtFieldWrite) newElementReplacementOfTheVar))
                                    continue;
                            }

                            if (newElementReplacementOfTheVar instanceof CtVariableRead) {

                                if (whetherVariableNewlyDeclared((CtVariableRead) newElementReplacementOfTheVar))
                                    continue;
                            }

                            if (newElementReplacementOfTheVar instanceof CtVariableWrite) {

                                if (whetherVariableNewlyDeclared((CtVariableWrite) newElementReplacementOfTheVar))
                                    continue;
                            }

                            if (newElementReplacementOfTheVar instanceof CtInvocation) {

                                if (whetherMethodNewlyDeclared((CtInvocation) newElementReplacementOfTheVar))
                                    continue;
                            }

                            if (replaceElementType.equals("Invocation") || replaceElementType.equals("VariableRead")
                                    || replaceElementType.equals("FieldRead") || replaceElementType.equals("ConstructorCall")
                                    || replaceElementType.equals("Literal") || replaceElementType.equals("FieldWrite")
                                    || replaceElementType.equals("VariableWrite") || replaceElementType.equals("TypeAccess"))
                                whetherConsiderInitial = true;
                        }

                        if (whetherConsiderInitial) {

                            if (srcNode instanceof CtInvocation) {

                                if (newElementReplacementOfTheVar instanceof CtInvocation &&
                                        ((CtInvocation) srcNode).getExecutable().getSimpleName().
                                                equals(((CtInvocation) newElementReplacementOfTheVar).getExecutable().getSimpleName())
                                        && ((CtInvocation) srcNode).getArguments().size() == ((CtInvocation) newElementReplacementOfTheVar).getArguments().size()) {
                                } else {
                                    if (newElementReplacementOfTheVar instanceof CtInvocation) {

                                        String srcCallMethodName = ((CtInvocation) srcNode).getExecutable().getSimpleName();
                                        String dstCallMethodName = ((CtInvocation) newElementReplacementOfTheVar).
                                                getExecutable().getSimpleName();

                                        if (!srcCallMethodName.equals(dstCallMethodName)) {
                                            repairPatterns.incrementFeatureCounterInstance(WRONG_METHOD_REF,
                                                    new PatternInstance(WRONG_METHOD_REF, operationDelete, patch, susp, parentLine,
                                                            lineTree, new PropertyPair("Change", "differentMethodName")));

                                        } else {
                                            repairPatterns.incrementFeatureCounterInstance(WRONG_METHOD_REF,
                                                    new PatternInstance(WRONG_METHOD_REF, operationDelete, patch, susp, parentLine,
                                                            lineTree, new PropertyPair("Change", "SameNamedifferentArgument")));
                                        }
                                    } else
                                        repairPatterns.incrementFeatureCounterInstance(WRONG_METHOD_REF, new PatternInstance(WRONG_METHOD_REF, operationDelete, patch, susp,
                                                parentLine, lineTree, metadata));
                                }
                            } else if (srcNode instanceof CtConstructorCall) {

                                String[] namespace = ((CtConstructorCall) srcNode).getExecutable().getSignature().split("\\(")[0].split("\\.");
                                String originalconstructorname = namespace[namespace.length - 1];

                                String newconstructorname = "";
                                if (newElementReplacementOfTheVar instanceof CtConstructorCall) {
                                    String[] namespacenew = ((CtConstructorCall) newElementReplacementOfTheVar).getExecutable().
                                            getSignature().split("\\(")[0].split("\\.");
                                    newconstructorname = namespacenew[namespacenew.length - 1];
                                }

                                if (newElementReplacementOfTheVar instanceof CtConstructorCall &&
                                        originalconstructorname.equals(newconstructorname)
                                        && ((CtConstructorCall) srcNode).getArguments().size() == ((CtConstructorCall) newElementReplacementOfTheVar).getArguments().size()) {
                                } else {
                                    if (newElementReplacementOfTheVar instanceof CtConstructorCall) {

                                        if (!originalconstructorname.equals(newconstructorname)) {
                                            repairPatterns.incrementFeatureCounterInstance(WRONG_METHOD_REF,
                                                    new PatternInstance(WRONG_METHOD_REF, operationDelete, patch, susp, parentLine,
                                                            lineTree, new PropertyPair("Change", "differentMethodName")));
                                        } else {
                                            repairPatterns.incrementFeatureCounterInstance(WRONG_METHOD_REF,
                                                    new PatternInstance(WRONG_METHOD_REF, operationDelete, patch, susp, parentLine,
                                                            lineTree, new PropertyPair("Change", "SameNamedifferentArgument")));
                                        }
                                    } else
                                        repairPatterns.incrementFeatureCounterInstance(WRONG_METHOD_REF,
                                                new PatternInstance(WRONG_METHOD_REF, operationDelete, patch, susp,
                                                        parentLine, lineTree, metadata));
                                }
                            } else {
                                repairPatterns.incrementFeatureCounterInstance(WRONG_VAR_REF,
                                        new PatternInstance(WRONG_VAR_REF, operationDelete, patch, susp, parentLine,
                                                lineTree, metadata));
                            }
                        }
                    }
                }
            }

            if (operation instanceof UpdateOperation) {

                CtElement srcNode = operation.getSrcNode();
                CtElement dstNode = operation.getDstNode();

                if (!invocationArgumentsold.contains(srcNode) || srcNode.getParent() != source
                        || !invocationArgumentsnew.contains(dstNode) || dstNode.getParent() != destination)
                    continue;

                if (srcNode instanceof CtVariableAccess || srcNode instanceof CtTypeAccess) {
                    if (operation.getDstNode() instanceof CtVariableAccess || operation.getDstNode() instanceof CtTypeAccess) {
                        if (srcNode instanceof CtVariableAccess && operation.getDstNode() instanceof CtVariableAccess) {
                            if (whetherVariableDeclarationDeleted((CtVariableAccess) srcNode)) {
                                continue;
                            }
                        }

                        CtElement susp = operation.getSrcNode();
                        CtElement patch = null;

                        CtElement parentLine = MappingAnalysis.getParentLine(new LineFilter(), susp);
                        ITree lineTree = MappingAnalysis.getFormattedTreeFromControlFlow(parentLine);

                        ITree parentRight = MappingAnalysis.getParentInRight(diff, operation.getAction());
                        if (parentRight != null) {
                            patch = (CtElement) parentRight.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                        }

                        repairPatterns.incrementFeatureCounterInstance(WRONG_VAR_REF, new PatternInstance(WRONG_VAR_REF,
                                operation, patch, susp, parentLine, lineTree, new PropertyPair("Change", "Update_"
                                + srcNode.getClass().getSimpleName().replace("Ct", "").replace("Impl", ""))));
                    }
                }

                if (!(srcNode instanceof CtInvocation) && !(srcNode instanceof CtConstructorCall)) {
                    continue;
                }

                if (dstNode instanceof CtInvocation || dstNode instanceof CtConstructorCall) {
                    String srcCallMethodName;
                    CtTargetedExpression srcInvocation = (CtTargetedExpression) srcNode;
                    List<CtTypeReference> srcCallRealArguments;

                    if (srcNode instanceof CtInvocation) {
                        srcCallMethodName = ((CtInvocation) srcNode).getExecutable().getSimpleName();
                        srcCallRealArguments = ((CtInvocation) srcNode).getArguments();
                    } else {
                        String[] namespace = ((CtConstructorCall) srcNode).getExecutable().getSignature().split("\\(")[0].split("\\.");
                        srcCallMethodName = namespace[namespace.length - 1];
                        srcCallRealArguments = ((CtConstructorCall) srcNode).getArguments();
                    }

                    String dstCallMethodName;
                    CtElement dst = dstNode;
                    List<CtTypeReference> dstCallRealArguments;
                    CtTargetedExpression dstInvocation;

                    if (dstNode instanceof CtInvocation) {
                        dstCallMethodName = ((CtInvocation) dstNode).getExecutable().getSimpleName();
                        dstInvocation = (CtTargetedExpression) dstNode;
                        dstCallRealArguments = ((CtInvocation) dstNode).getArguments();

                    } else {
                        String[] namespace = ((CtConstructorCall) dstNode).getExecutable().getSignature().split("\\(")[0].split("\\.");
                        dstCallMethodName = namespace[namespace.length - 1];
                        dstInvocation = (CtTargetedExpression) dstNode;
                        dstCallRealArguments = ((CtConstructorCall) dstNode).getArguments();

                    }
                    if (!whetherMethodDefUpdated(srcNode, dstNode)) {
                        if (srcInvocation != null && dstInvocation != null && srcInvocation.getTarget() != null
                                && dstInvocation.getTarget() != null
                                && !srcInvocation.getTarget().equals(dstInvocation.getTarget())) {
                            continue;
                        }

                        CtElement parentLine = MappingAnalysis.getParentLine(new LineFilter(), srcInvocation);
                        ITree lineTree = MappingAnalysis.getFormattedTreeFromControlFlow(parentLine);

                        if (!srcCallMethodName.equals(dstCallMethodName)) {
                            repairPatterns.incrementFeatureCounterInstance(WRONG_METHOD_REF,
                                    new PatternInstance(WRONG_METHOD_REF, operation, dst, srcInvocation, parentLine,
                                            lineTree, new PropertyPair("Change", "differentMethodName")));
                        } else {
                            if (srcCallRealArguments.size() != dstCallRealArguments.size()) {
                                repairPatterns.incrementFeatureCounterInstance(WRONG_METHOD_REF,
                                        new PatternInstance(WRONG_METHOD_REF, operation, dst, srcInvocation, parentLine,
                                                lineTree, new PropertyPair("Change", "SameNamedifferentArgument")));
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean whetherSameTypeWithSameName(CtElement oldElement, CtElement newElement) {
        if (oldElement != null && newElement != null) {
            if (oldElement instanceof CtVariableAccess && newElement instanceof CtVariableAccess) {
                if (((CtVariableAccess) oldElement).getVariable().getSimpleName().
                        equals(((CtVariableAccess) newElement).getVariable().getSimpleName())) {
                    return true;
                }
            }

            if (oldElement instanceof CtTypeAccess && newElement instanceof CtTypeAccess) {
                if (((CtTypeAccess) oldElement).getType().getSimpleName().
                        equals(((CtTypeAccess) newElement).getType().getSimpleName())) {
                    return true;
                }
            }

            if (oldElement instanceof CtInvocation && newElement instanceof CtInvocation) {
                if (((CtInvocation) oldElement).getExecutable().getSignature().
                        equals(((CtInvocation) newElement).getExecutable().getSignature())) {
                    return true;
                }
            }

            if (oldElement instanceof CtConstructorCall && newElement instanceof CtConstructorCall) {
                return ((CtConstructorCall) oldElement).getExecutable().getSignature().
                        equals(((CtConstructorCall) newElement).getExecutable().getSignature());
            }
        }

        return false;
    }

    private void detectWrapsMethod(List<CtExpression> invocationArgumentsold, List<CtExpression> invocationArgumentsnew,
                                   Operation operation, RepairPatterns repairPatterns, CtElement source, CtElement destination) {

        if (operation.getSrcNode() instanceof CtInvocation) {
            if (operation instanceof InsertOperation) {
                CtInvocation ctInvocation = (CtInvocation) operation.getSrcNode();
                List<CtExpression> invocationArguments = ctInvocation.getArguments();
                if (invocationArgumentsnew.contains(ctInvocation) && ctInvocation.getParent() == destination) {

                    for (Operation operation2 : diff.getAllOperations()) {
                        if (operation2 instanceof DeleteOperation) {
                            CtElement elementRemoved = operation2.getSrcNode();

                            if (elementRemoved instanceof CtVariableRead && invocationArgumentsold.contains(elementRemoved)
                                    && elementRemoved.getParent() == source) {

                                if (invocationArguments.contains(elementRemoved)) {
                                    CtElement lineP = MappingAnalysis.getParentLine(new LineFilter(), elementRemoved);
                                    ITree lineTree = MappingAnalysis.getFormattedTreeFromControlFlow(lineP);
                                    repairPatterns.incrementFeatureCounterInstance(WRAPS_METHOD,
                                            new PatternInstance(WRAPS_METHOD, operation, ctInvocation, elementRemoved,
                                                    lineP, lineTree, new PropertyPair("Old", "VarRead"),
                                                    new PropertyPair("New", "Invocation")));
                                }
                            }
                        }
                    }

                    for (CtExpression ctExpression : invocationArguments) {
                        if (ctExpression.getMetadata("isMoved") != null) {

                            List<CtElement> suspLeft = MappingAnalysis.getTreeLeftMovedFromRight(diff, ctInvocation);
                            if (suspLeft == null || suspLeft.isEmpty())
                                return;

                            CtElement lineP = MappingAnalysis.getParentLine(new LineFilter(), suspLeft.get(0));
                            ITree lineTree = MappingAnalysis.getFormattedTreeFromControlFlow(lineP);

                            if (RepairPatternUtils.getIsInvocationInStatemnt(diff, lineP, ctInvocation) &&
                                    invocationArgumentsold.contains(ctExpression) && ctExpression.getParent() == source) {

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
                }
            } else {
                if (operation instanceof DeleteOperation) {
                    CtInvocation ctInvocation = (CtInvocation) operation.getSrcNode();
                    CtStatement statementParent = ctInvocation.getParent(new TypeFilter<>(CtStatement.class));

                    if (invocationArgumentsold.contains(ctInvocation) && ctInvocation.getParent() == source)
                        if (statementParent.getMetadata("delete") == null) {
                            List<CtExpression> invocationArguments = ctInvocation.getArguments();

                            for (CtExpression ctExpression : invocationArguments) {
                                if (ctExpression.getMetadata("isMoved") != null
                                        && ctExpression.getMetadata("movingSrc") != null) {

                                    if (RepairPatternUtils.getIsMovedExpressionInStatement(diff, statementParent, ctExpression) &&
                                            whetherconsinderthemethodunrap(ctExpression, ctInvocation) &&
                                            invocationArgumentsnew.contains(ctExpression) && ctExpression.getParent() == destination) {
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
                CtConstructorCall ctInvocation = (CtConstructorCall) operation.getSrcNode();
                List<CtExpression> invocationArguments = ctInvocation.getArguments();
                if (invocationArgumentsnew.contains(ctInvocation) && ctInvocation.getParent() == destination) {
                    for (Operation operation2 : diff.getAllOperations()) {
                        if (operation2 instanceof DeleteOperation) {
                            CtElement elementRemoved = operation2.getSrcNode();

                            if (elementRemoved instanceof CtVariableRead && invocationArgumentsold.contains(elementRemoved)
                                    && elementRemoved.getParent() == source) {

                                if (invocationArguments.contains(elementRemoved)) {
                                    CtElement lineP = MappingAnalysis.getParentLine(new LineFilter(), elementRemoved);
                                    ITree lineTree = MappingAnalysis.getFormattedTreeFromControlFlow(lineP);
                                    repairPatterns.incrementFeatureCounterInstance(WRAPS_METHOD,
                                            new PatternInstance(WRAPS_METHOD, operation, ctInvocation, elementRemoved,
                                                    lineP, lineTree, new PropertyPair("Old", "VarRead"),
                                                    new PropertyPair("New", "Constructor")));
                                }
                            }
                        }
                    }

                    for (CtExpression ctExpression : invocationArguments) {
                        if (ctExpression.getMetadata("isMoved") != null) {

                            List<CtElement> suspLeft = MappingAnalysis.getTreeLeftMovedFromRight(diff, ctInvocation);
                            if (suspLeft == null || suspLeft.isEmpty())
                                return;

                            CtElement lineP = MappingAnalysis.getParentLine(new LineFilter(), suspLeft.get(0));
                            ITree lineTree = MappingAnalysis.getFormattedTreeFromControlFlow(lineP);

                            if (RepairPatternUtils.getIsInvocationInStatemnt(diff, lineP, ctInvocation) &&
                                    invocationArgumentsold.contains(ctExpression) && ctExpression.getParent() == source) {

                                if (RepairPatternUtils.getElementInOld(diff, ctExpression) != null &&
                                        (RepairPatternUtils.getElementInOld(diff, ctExpression).getParent() instanceof CtConstructorCall ||
                                                RepairPatternUtils.getElementInOld(diff, ctExpression).getParent() instanceof CtInvocation)
                                        && RepairPatternUtils.getElementInOld(diff, ctExpression).getParent() !=
                                        RepairPatternUtils.getElementInOld(diff, ctInvocation.getParent())) {
                                } else
                                    repairPatterns.incrementFeatureCounterInstance(WRAPS_METHOD,
                                            new PatternInstance(WRAPS_METHOD, operation, ctInvocation, ctExpression, lineP,
                                                    lineTree, new PropertyPair("Old", "MovedExpression"),
                                                    new PropertyPair("New", "Constructor")));
                            }
                        }
                    }
                }
            } else {
                if (operation instanceof DeleteOperation) {
                    CtConstructorCall ctInvocation = (CtConstructorCall) operation.getSrcNode();
                    CtStatement statementParent = ctInvocation.getParent(new TypeFilter<>(CtStatement.class));

                    if (invocationArgumentsold.contains(ctInvocation) && ctInvocation.getParent() == source)
                        if (statementParent.getMetadata("delete") == null) {
                            List<CtExpression> invocationArguments = ctInvocation.getArguments();

                            for (CtExpression ctExpression : invocationArguments) {
                                if (ctExpression.getMetadata("isMoved") != null
                                        && ctExpression.getMetadata("movingSrc") != null) {

                                    if (RepairPatternUtils.getIsMovedExpressionInStatement(diff, statementParent, ctExpression) &&
                                            whetherConsinderTheConstructorUnwrap(ctExpression, ctInvocation) &&
                                            invocationArgumentsnew.contains(ctExpression) &&
                                            ctExpression.getParent() == destination) {
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
    }

    private boolean whetherConsinderTheConstructorUnwrap(CtExpression oldExpression, CtConstructorCall ctCall) {
        ITree treenewexpression = MappingAnalysis.getRightFromLeftNodeMapped(diff, oldExpression);

        if (treenewexpression == null)
            return false;

        CtElement newexpression = (CtElement) treenewexpression.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);

        CtStatement oldparent = oldExpression.getParent(new LineFilter());
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

    public void detectConstantChange(List<CtExpression> invocationArgumentsOld, List<CtExpression> invocationArgumentsNew,
                                     RepairPatterns repairPatterns, CtElement source, CtElement destination) {
        List<CtElement> alreadyConsidered = new ArrayList<>();
        for (int i = 0; i < diff.getAllOperations().size(); i++) {
            Operation operation = diff.getAllOperations().get(i);

            if ((operation instanceof UpdateOperation) && invocationArgumentsOld.contains(operation.getSrcNode()) &&
                    operation.getSrcNode().getParent() == source && operation.getDstNode().getParent() == destination
                    && invocationArgumentsNew.contains(operation.getDstNode())) {
                CtElement srcNode = operation.getSrcNode();

                CtElement parent = MappingAnalysis.getParentLine(new LineFilter(), srcNode);
                ITree lineTree = MappingAnalysis.getFormattedTreeFromControlFlow(parent);

                if (srcNode instanceof CtLiteral) {
                    repairPatterns.incrementFeatureCounterInstance(CONST_CHANGE,
                            new PatternInstance(CONST_CHANGE, operation, operation.getDstNode(), srcNode, parent,
                                    lineTree, new PropertyPair("type", "literal")));
                }
            } else if (operation instanceof DeleteOperation && operation.getSrcNode() instanceof CtLiteral &&
                        invocationArgumentsOld.contains(operation.getSrcNode()) && operation.getSrcNode().getParent() == source) {
                CtLiteral ctLiteral = (CtLiteral) operation.getSrcNode();

                for (int j = 0; j < diff.getAllOperations().size(); j++) {
                    Operation operation2Insert = diff.getAllOperations().get(j);
                    if (operation2Insert instanceof InsertOperation &&
                            invocationArgumentsNew.contains(operation2Insert.getSrcNode()) &&
                            operation2Insert.getSrcNode().getParent() == destination) {
                        CtElement ctElement = operation2Insert.getSrcNode();

                        boolean isConstantVariable = false;
                        if (ctElement instanceof CtVariableAccess || ctElement instanceof CtArrayAccess
                                || (ctElement instanceof CtTypeAccess && !RepairPatternUtils.isThisAccess((CtTypeAccess) ctElement)
                                && RepairPatternUtils.isConstantTypeAccess((CtTypeAccess) ctElement))) {
                            isConstantVariable = true;
                        }
                        if (isConstantVariable && !alreadyConsidered.contains(ctElement)) {
                            alreadyConsidered.add(ctElement);
                            CtElement parent = MappingAnalysis.getParentLine(new LineFilter(), ctLiteral);
                            ITree lineTree = MappingAnalysis.getFormattedTreeFromControlFlow(parent);

                            repairPatterns.incrementFeatureCounterInstance(CONST_CHANGE,
                                    new PatternInstance(CONST_CHANGE, operation2Insert,
                                            operation2Insert.getSrcNode(), ctLiteral, parent, lineTree,
                                            new PropertyPair("type", "literal_by_var_access")));
                            break;
                        }
                    }
                }
            }
        }
    }

    public void detectWrapIfChange(List<CtExpression> invocationArgumentsOld, List<CtExpression> invocationArgumentsNew,
                                   Operation operation, RepairPatterns repairPatterns, CtElement source, CtElement destination) {

        if (operation instanceof InsertOperation || operation instanceof DeleteOperation) {
            CtElement ctElement = operation.getSrcNode();
            SpoonHelper.printInsertOrDeleteOperation(ctElement.getFactory().getEnvironment(), ctElement, operation);

            List<CtConditional> conditionalList = ctElement.getElements(new TypeFilter<>(CtConditional.class));
            for (CtConditional ctConditional : conditionalList) {
                if (ctConditional.getMetadata("new") != null && (invocationArgumentsOld.contains(ctConditional) ||
                        invocationArgumentsNew.contains(ctConditional))) {

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

                                if (invocationArgumentsNew.contains(ctConditional) && ctConditional.getParent() == destination
                                        && invocationArgumentsOld.contains(susp) && susp.getParent() == source)
                                    repairPatterns.incrementFeatureCounterInstance(WRAPS_IF_ELSE, new PatternInstance(
                                            WRAPS_IF_ELSE, operation, ctConditional, susp, lineP, lineTree,
                                            new PropertyPair("case", "elsenotnull")));
                            }
                        } else {
                            if (statementParent.getMetadata("delete") == null) {
                                List susps = new ArrayList();
                                CtExpression patch;
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

                                ITree rightMoved = MappingAnalysis.getRightFromLeftNodeMapped(diff,
                                        (ITree) patch.getMetadata("gtnode"));

                                CtElement remaining = (CtElement) rightMoved.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);

                                if (invocationArgumentsNew.contains(remaining) && remaining.getParent() == destination
                                        && invocationArgumentsOld.contains(ctConditional) && ctConditional.getParent() == source) {
                                    repairPatterns.incrementFeatureCounterInstance(UNWRAP_IF_ELSE,
                                            new PatternInstance(UNWRAP_IF_ELSE, operation, patch, susps, lineP, lineTree));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void detectNullCheckChange(List<CtExpression> invocationArgumentsOld, List<CtExpression> invocationArgumentsNew,
                                      RepairPatterns repairPatterns, CtElement source, CtElement destination) {

        for (Operation operation : diff.getAllOperations()) {
            if (operation instanceof InsertOperation) {

                CtElement srcNode = operation.getSrcNode();

                if (srcNode instanceof spoon.reflect.declaration.CtMethod || !invocationArgumentsNew.contains(srcNode)
                        || srcNode.getParent() != destination) {
                    continue;
                }

                SpoonHelper.printInsertOrDeleteOperation(srcNode.getFactory().getEnvironment(), srcNode, operation);

                List<CtBinaryOperator> binaryOperatorList = srcNode.getElements(new NullCheckFilter());

                for (CtBinaryOperator binaryOperator : binaryOperatorList) {
                    if (RepairPatternUtils.isNewBinaryOperator(binaryOperator)) {
                        if (RepairPatternUtils.isNewConditionInBinaryOperator(binaryOperator)) {
                            boolean wasPatternFound = false;

                            List soldt = null;
                            List soldelse = null;

                            CtElement parent = binaryOperator.getParent(new LineFilter());

                            if (binaryOperator.getParent() instanceof CtConditional) {

                                CtConditional c = (CtConditional) binaryOperator.getParent();
                                CtElement thenExpr = c.getThenExpression();
                                CtElement elseExp = c.getElseExpression();

                                if (thenExpr != null) {
                                    soldt = new ArrayList<>();
                                    // If it's not new the THEN
                                    if (thenExpr.getMetadata("new") == null && invocationArgumentsOld.
                                            contains(RepairPatternUtils.getElementInOld(diff, thenExpr))
                                            && RepairPatternUtils.getElementInOld(diff, thenExpr).getParent() == source) {
                                        //    soldelse.add(thenExpr);
                                        soldt.add(RepairPatternUtils.getElementInOld(diff, thenExpr));
                                        wasPatternFound = true;
                                    }
                                }
                                if (elseExp != null) {
                                    soldelse = new ArrayList<>();
                                    if (elseExp.getMetadata("new") == null && invocationArgumentsOld.
                                            contains(RepairPatternUtils.getElementInOld(diff, elseExp))
                                            && RepairPatternUtils.getElementInOld(diff, elseExp).getParent() == source) {
                                        //    soldelse.add(elseExp);
                                        soldt.add(RepairPatternUtils.getElementInOld(diff, elseExp));
                                        wasPatternFound = true;
                                    }
                                }
                            }

                            if (wasPatternFound) {
                                List<CtElement> susp = new ArrayList<>();
                                if (soldt != null) {
                                    susp.addAll(soldt);
                                }
                                if (soldelse != null) {
                                    susp.addAll(soldelse);
                                }

                                CtElement lineP;
                                ITree lineTree;
                                lineTree = null;
                                if (!susp.isEmpty()) {

                                    lineP = MappingAnalysis.getParentLine(new LineFilter(), susp.get(0));
                                    lineTree = MappingAnalysis.getFormattedTreeFromControlFlow(lineP);

                                } else {

                                    InsertOperation operationIns = (InsertOperation) operation;

                                    List<ITree> treeInLeft = MappingAnalysis.getFollowStatementsInLeft(diff,
                                            operationIns.getAction());

                                    if (treeInLeft.isEmpty()) {
                                        System.out.println(
                                                "Problem!!!! Empty parent in " + MISS_NULL_CHECK_N.toLowerCase());
                                        continue;
                                    }

                                    for (ITree iTree : treeInLeft) {
                                        susp.add((CtElement) iTree.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT));
                                    }

                                    lineP = susp.get(0);
                                    //    lineTree = treeInLeft.get(0);
                                    lineTree = MappingAnalysis.getFormattedTreeFromControlFlow(lineP);
                                }

                                if (binaryOperator.getKind().equals(BinaryOperatorKind.EQ)) {
                                    repairPatterns.incrementFeatureCounterInstance(MISS_NULL_CHECK_P,
                                            new PatternInstance(MISS_NULL_CHECK_P, operation, parent, susp, lineP,
                                                    lineTree));
                                } else {
                                    repairPatterns.incrementFeatureCounterInstance(MISS_NULL_CHECK_N,
                                            new PatternInstance(MISS_NULL_CHECK_N, operation, parent, susp, lineP,
                                                    lineTree));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean whetherMethodDefUpdated(CtElement srcNode, CtElement dstNode) {
        boolean wasMethodDefUpdated = false;

        String srcCallMethodName;
        List<CtTypeReference> srcCallArguments;

        if (srcNode instanceof CtInvocation) {
            srcCallMethodName = ((CtInvocation) srcNode).getExecutable().getSimpleName();
            srcCallArguments = ((CtInvocation) srcNode).getActualTypeArguments();
        } else {
            String[] namespace = ((CtConstructorCall) srcNode).getExecutable().getSignature().split("\\(")[0].split("\\.");
            srcCallMethodName = namespace[namespace.length - 1];
            srcCallArguments = ((CtConstructorCall) srcNode).getActualTypeArguments();
        }

        String dstCallMethodName;
        List<CtTypeReference> dstCallArguments;

        if (dstNode instanceof CtInvocation) {
            dstCallMethodName = ((CtInvocation) dstNode).getExecutable().getSimpleName();
            dstCallArguments = ((CtInvocation) dstNode).getActualTypeArguments();
        } else {
            String[] namespace = ((CtConstructorCall) dstNode).getExecutable().getSignature().split("\\(")[0].split("\\.");
            dstCallMethodName = namespace[namespace.length - 1];
            dstCallArguments = ((CtConstructorCall) dstNode).getActualTypeArguments();
        }

        for (Operation operation2 : operations) {
            if (operation2 instanceof InsertOperation) {
                CtElement insertedNode = operation2.getSrcNode();
                if (insertedNode instanceof CtParameter) {
                    CtElement ctElement = ((InsertOperation) operation2).getParent();
                    if (ctElement instanceof CtMethod) {
                        CtMethod oldMethod = (CtMethod) ctElement;
                        CtMethod newMethod = insertedNode.getParent(CtMethod.class);

                        if (oldMethod.getSimpleName().equals(srcCallMethodName)
                                && newMethod.getSimpleName().equals(dstCallMethodName)) {
                            boolean oldParEquals = true;
                            List<CtParameter> oldMethodPars = oldMethod.getParameters();
                            for (int k = 0; k < oldMethodPars.size(); k++) {
                                CtTypeReference methodParType = oldMethodPars.get(k).getType();
                                if (k < srcCallArguments.size()) {
                                    CtTypeReference methodCallArgType = srcCallArguments.get(k);
                                    if (!methodParType.getQualifiedName()
                                            .equals(methodCallArgType.getQualifiedName())) {
                                        oldParEquals = false;
                                        break;
                                    }
                                }
                            }

                            if (oldParEquals) {
                                boolean newParEquals = true;
                                List<CtParameter> newMethodPars = newMethod.getParameters();
                                for (int k = 0; k < newMethodPars.size(); k++) {
                                    CtTypeReference methodParType = newMethodPars.get(k).getType();
                                    if (k < dstCallArguments.size()) {
                                        CtTypeReference methodCallArgType = dstCallArguments.get(k);
                                        if (!methodParType.getQualifiedName()
                                                .equals(methodCallArgType.getQualifiedName())) {
                                            newParEquals = false;
                                            break;
                                        }
                                    }
                                }

                                if (newParEquals) {
                                    wasMethodDefUpdated = true;
                                }
                            }
                        }
                    }

                    if (ctElement instanceof CtConstructor) {
                        CtConstructor oldConstructor = (CtConstructor) ctElement;
                        CtConstructor newConstructor = insertedNode.getParent(CtConstructor.class);

                        if (oldConstructor.getSimpleName().equals(srcCallMethodName)
                                && newConstructor.getSimpleName().equals(dstCallMethodName)) {
                            boolean oldParEquals = true;
                            List<CtParameter> oldMethodPars = oldConstructor.getParameters();
                            for (int k = 0; k < oldMethodPars.size(); k++) {
                                CtTypeReference methodParType = oldMethodPars.get(k).getType();
                                if (k < srcCallArguments.size()) {
                                    CtTypeReference methodCallArgType = srcCallArguments.get(k);
                                    if (!methodParType.getQualifiedName()
                                            .equals(methodCallArgType.getQualifiedName())) {
                                        oldParEquals = false;
                                        break;
                                    }
                                }
                            }

                            if (oldParEquals) {
                                boolean newParEquals = true;
                                List<CtParameter> newMethodPars = newConstructor.getParameters();
                                for (int k = 0; k < newMethodPars.size(); k++) {
                                    CtTypeReference methodParType = newMethodPars.get(k).getType();
                                    if (k < dstCallArguments.size()) {
                                        CtTypeReference methodCallArgType = dstCallArguments.get(k);
                                        if (!methodParType.getQualifiedName().equals(methodCallArgType.getQualifiedName())) {
                                            newParEquals = false;
                                            break;
                                        }
                                    }
                                }

                                if (newParEquals) {
                                    wasMethodDefUpdated = true;
                                }
                            }
                        }
                    }
                }
            }

            if (operation2 instanceof DeleteOperation) {
                CtElement deleteNode = operation2.getSrcNode();
                if (deleteNode instanceof CtParameter) {
                    CtElement ctElement = deleteNode.getParent(CtMethod.class);
                    if (ctElement != null) {
                        CtMethod oldMethod = (CtMethod) ctElement;
                        CtMethod newMethod;

                        ITree rightTreeMethod = MappingAnalysis.getRightFromLeftNodeMapped(diff, oldMethod);

                        if (rightTreeMethod == null)
                            newMethod = null;
                        else {
                            newMethod = (CtMethod) rightTreeMethod.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);
                        }

                        if (oldMethod.getSimpleName().equals(srcCallMethodName)
                                && newMethod.getSimpleName().equals(dstCallMethodName)) {
                            boolean oldParEquals = true;
                            List<CtParameter> oldMethodPars = oldMethod.getParameters();
                            for (int k = 0; k < oldMethodPars.size(); k++) {
                                CtTypeReference methodParType = oldMethodPars.get(k).getType();
                                if (k < srcCallArguments.size()) {
                                    CtTypeReference methodCallArgType = srcCallArguments.get(k);
                                    if (!methodParType.getQualifiedName()
                                            .equals(methodCallArgType.getQualifiedName())) {
                                        oldParEquals = false;
                                        break;
                                    }
                                }
                            }

                            if (oldParEquals) {
                                boolean newParEquals = true;
                                List<CtParameter> newMethodPars = newMethod.getParameters();
                                for (int k = 0; k < newMethodPars.size(); k++) {
                                    CtTypeReference methodParType = newMethodPars.get(k).getType();
                                    if (k < dstCallArguments.size()) {
                                        CtTypeReference methodCallArgType = dstCallArguments.get(k);
                                        if (!methodParType.getQualifiedName()
                                                .equals(methodCallArgType.getQualifiedName())) {
                                            newParEquals = false;
                                            break;
                                        }
                                    }
                                }

                                if (newParEquals) {
                                    wasMethodDefUpdated = true;
                                }
                            }
                        }
                    }

                    ctElement = deleteNode.getParent(CtConstructor.class);
                    if (ctElement != null) {
                        CtConstructor oldConstructor = (CtConstructor) ctElement;
                        CtConstructor newConstructor;

                        ITree rightTreeMethod = MappingAnalysis.getRightFromLeftNodeMapped(diff, oldConstructor);

                        if (rightTreeMethod == null)
                            newConstructor = null;
                        else
                            newConstructor = (CtConstructor) rightTreeMethod.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);

                        if (oldConstructor.getSimpleName().equals(srcCallMethodName)
                                && newConstructor.getSimpleName().equals(dstCallMethodName)) {
                            boolean oldParEquals = true;
                            List<CtParameter> oldMethodPars = oldConstructor.getParameters();
                            for (int k = 0; k < oldMethodPars.size(); k++) {
                                CtTypeReference methodParType = oldMethodPars.get(k).getType();
                                if (k < srcCallArguments.size()) {
                                    CtTypeReference methodCallArgType = srcCallArguments.get(k);
                                    if (!methodParType.getQualifiedName()
                                            .equals(methodCallArgType.getQualifiedName())) {
                                        oldParEquals = false;
                                        break;
                                    }
                                }
                            }
                            if (oldParEquals) {
                                boolean newParEquals = true;
                                List<CtParameter> newMethodPars = newConstructor.getParameters();
                                for (int k = 0; k < newMethodPars.size(); k++) {
                                    CtTypeReference methodParType = newMethodPars.get(k).getType();
                                    if (k < dstCallArguments.size()) {
                                        CtTypeReference methodCallArgType = dstCallArguments.get(k);
                                        if (!methodParType.getQualifiedName()
                                                .equals(methodCallArgType.getQualifiedName())) {
                                            newParEquals = false;
                                            break;
                                        }
                                    }
                                }

                                if (newParEquals) {
                                    wasMethodDefUpdated = true;
                                }
                            }
                        }
                    }
                }
            }
        }

        return wasMethodDefUpdated;
    }

    public boolean whetherParentChanged(CtElement parent) {
        boolean parentchanged = false;
        for (Operation operation : diff.getAllOperations()) {
            if (operation.getSrcNode().equals(parent)) {
                parentchanged = true;
                break;
            }
        }
        return parentchanged;
    }

    public boolean whetherMethodNewlyDeclared(CtInvocation methodCall) {
        for (Operation operation2 : operations) {
            if (operation2 instanceof InsertOperation) {
                CtElement insertedNode = operation2.getSrcNode();
                if (insertedNode instanceof CtMethod) {
                    if (((CtMethod) insertedNode).getSignature().equals(methodCall.getExecutable().getSignature())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public boolean whetherVariableNewlyDeclared(CtVariableAccess variableAccess) {
        for (Operation operation2 : operations) {
            if (operation2 instanceof InsertOperation) {
                CtElement insertedNode = operation2.getSrcNode();
                // See whether a method signature was modified
                if (insertedNode instanceof CtParameter) {
                    if (((CtParameter) insertedNode).getSimpleName().equals(variableAccess.getVariable().getSimpleName())) {
                        CtMethod parentParameter = insertedNode.getParent(CtMethod.class);

                        CtMethod parentVarAccess = variableAccess.getParent(CtMethod.class);

                        if (parentParameter != null && parentParameter.equals(parentVarAccess)) {
                            return true;
                        }

                        CtConstructor parentParameterConstructor = insertedNode.getParent(CtConstructor.class);

                        CtConstructor parentVarAccessConstructor = variableAccess.getParent(CtConstructor.class);

                        if (parentParameterConstructor != null && parentParameterConstructor.equals(parentVarAccessConstructor)) {
                            return true;
                        }
                    }
                }

                if (insertedNode instanceof CtLocalVariable) {
                    if (((CtLocalVariable) insertedNode).getSimpleName().equals(variableAccess.getVariable().getSimpleName())) {
                        CtMethod parentLocalVariable = insertedNode.getParent(CtMethod.class);
                        CtMethod parentVarAccess = variableAccess.getParent(CtMethod.class);
                        if (parentLocalVariable != null && parentLocalVariable.equals(parentVarAccess))
                            return true;

                        CtConstructor parentLocalVariableConstructor = insertedNode.getParent(CtConstructor.class);

                        CtConstructor parentVarAccessConstructor = variableAccess.getParent(CtConstructor.class);

                        if (parentLocalVariableConstructor != null && parentLocalVariableConstructor.equals(parentVarAccessConstructor))
                            return true;
                    }
                }

                if (insertedNode instanceof CtField) {

                    if (((CtField) insertedNode).getSimpleName().equals(variableAccess.getVariable().getSimpleName()))
                        return true;
                }
            }
        }

        return false;
    }

    public boolean whetherVariableDeclarationDeleted(CtVariableAccess variableAccess) {
        for (Operation operation2 : operations) {
            if (operation2 instanceof DeleteOperation) {
                CtElement deleteNode = operation2.getSrcNode();
                // See whether a method signature was modified
                if (deleteNode instanceof CtParameter) {

                    if (((CtParameter) deleteNode).getSimpleName().equals(variableAccess.getVariable().getSimpleName())) {
                        CtMethod parentParameter = deleteNode.getParent(CtMethod.class);
                        CtMethod parentVarAccess = variableAccess.getParent(CtMethod.class);
                        if (parentParameter != null && parentParameter.equals(parentVarAccess)) {
                            return true;
                        }

                        CtConstructor parentParameterConstructor = deleteNode.getParent(CtConstructor.class);

                        CtConstructor parentVarAccessConstructor = variableAccess.getParent(CtConstructor.class);

                        if (parentParameterConstructor != null && parentParameterConstructor.equals(parentVarAccessConstructor))
                            return true;
                    }
                }

                if (deleteNode instanceof CtLocalVariable) {

                    if (((CtLocalVariable) deleteNode).getSimpleName().equals(variableAccess.getVariable().getSimpleName())) {

                        CtMethod parentLocalVariable = deleteNode.getParent(CtMethod.class);

                        CtMethod parentVarAccess = variableAccess.getParent(CtMethod.class);

                        if (parentLocalVariable != null && parentLocalVariable.equals(parentVarAccess)) {
                            return true;
                        }

                        CtConstructor parentLocalVariableConstructor = deleteNode.getParent(CtConstructor.class);
                        CtConstructor parentVarAccessConstructor = variableAccess.getParent(CtConstructor.class);

                        if (parentLocalVariableConstructor != null && parentLocalVariableConstructor.equals(parentVarAccessConstructor)) {
                            return true;
                        }
                    }
                }

                if (deleteNode instanceof CtField) {

                    if (((CtField) deleteNode).getSimpleName().equals(variableAccess.getVariable().getSimpleName()))
                        return true;
                }
            }
        }

        return false;
    }

    public boolean whetherVariableDeclarationUpdated(CtVariableAccess varAccessOriginal, CtVariableAccess varAccessNew) {

        for (Operation operation2 : operations) {

            if (operation2 instanceof UpdateOperation) {

                CtElement srcNode = operation2.getSrcNode();
                CtElement dstNode = operation2.getDstNode();

                CtElement originalParent = getVarDeclaration(srcNode);
                CtElement newParent = getVarDeclaration(dstNode);

                if (originalParent != null && newParent != null) {
                    if (((CtVariable) originalParent).getSimpleName().equals(varAccessOriginal.getVariable().getSimpleName())
                            && ((CtVariable) newParent).getSimpleName().equals(varAccessNew.getVariable().getSimpleName())) {

                        if (originalParent instanceof CtParameter) {
                            if (newParent instanceof CtParameter) {
                                CtMethod parentParameterOriginal = originalParent.getParent(CtMethod.class);

                                CtMethod parentVarAccessOriginal = varAccessOriginal.getParent(CtMethod.class);

                                CtMethod parentParameterNew = newParent.getParent(CtMethod.class);

                                CtMethod parentVarAccessNew = varAccessNew.getParent(CtMethod.class);

                                if (parentParameterOriginal != null && parentParameterOriginal.equals(parentVarAccessOriginal) &&
                                        parentParameterNew != null && parentParameterNew.equals(parentVarAccessNew))
                                    return true;

                                CtConstructor parentParameterOriginalCon = originalParent.getParent(CtConstructor.class);

                                CtConstructor parentVarAccessOriginalCon = varAccessOriginal.getParent(CtConstructor.class);

                                CtConstructor parentParameterNewCon = newParent.getParent(CtConstructor.class);

                                CtConstructor parentVarAccessNewCon = varAccessNew.getParent(CtConstructor.class);

                                if (parentParameterOriginalCon != null && parentParameterOriginalCon.equals(parentVarAccessOriginalCon) &&
                                        parentParameterNewCon != null && parentParameterNewCon.equals(parentVarAccessNewCon))
                                    return true;
                            }
                        }

                        if (originalParent instanceof CtLocalVariable) {
                            if (newParent instanceof CtLocalVariable) {
                                CtMethod parentLocalVariableOriginal = originalParent.getParent(CtMethod.class);

                                CtMethod parentVarAccessOriginal = varAccessOriginal.getParent(CtMethod.class);

                                CtMethod parentLocalVariableNew = newParent.getParent(CtMethod.class);

                                CtMethod parentVarAccessNew = varAccessNew.getParent(CtMethod.class);

                                if (parentLocalVariableOriginal != null && parentLocalVariableOriginal.equals(parentVarAccessOriginal) &&
                                        parentLocalVariableNew != null && parentLocalVariableNew.equals(parentVarAccessNew)) {
                                    return true;
                                }

                                CtConstructor parentLocalVariableOriginalCon = originalParent.getParent(CtConstructor.class);

                                CtConstructor parentVarAccessOriginalCon = varAccessOriginal.getParent(CtConstructor.class);

                                CtConstructor parentLocalVariableNewCon = newParent.getParent(CtConstructor.class);

                                CtConstructor parentVarAccessNewCon = varAccessNew.getParent(CtConstructor.class);

                                if (parentLocalVariableOriginalCon != null && parentLocalVariableOriginalCon.equals(parentVarAccessOriginalCon) &&
                                        parentLocalVariableNewCon != null && parentLocalVariableNewCon.equals(parentVarAccessNewCon)) {
                                    return true;
                                }
                            }
                        }

                        if (originalParent instanceof CtField) {
                            if (newParent instanceof CtField) {
                                return true;
                            }
                        }

                    }
                }
            }
        }

        return false;
    }

    public CtElement getVarDeclaration(CtElement input) {
        CtElement varparent = input;

        while (varparent != null) {
            if (varparent instanceof CtParameter || varparent instanceof CtLocalVariable || varparent instanceof CtField) {
                return varparent;
            } else varparent = varparent.getParent();
        }

        return null;
    }

    private boolean whetherVarWrapped(CtElement srcNode, CtElement node2) {
        boolean wasVariableWrapped = false;

        List<CtExpression> invocationArguments = new ArrayList<>();
        if (node2 instanceof CtInvocation) {
            invocationArguments = ((CtInvocation) node2).getArguments();
        }
        if (node2 instanceof CtConstructorCall) {
            invocationArguments = ((CtConstructorCall) node2).getArguments();
        }

        for (CtExpression ctExpression : invocationArguments) {
            if (srcNode instanceof CtVariableAccess
                    && ctExpression instanceof CtVariableAccess) {
                CtVariableAccess srcVariableAccess = (CtVariableAccess) srcNode;
                CtVariableAccess dstVariableAccess = (CtVariableAccess) ctExpression;
                if (srcVariableAccess.getVariable().getSimpleName().equals(
                        dstVariableAccess.getVariable().getSimpleName())) {
                    wasVariableWrapped = true;
                    break;
                }
            } else if (srcNode instanceof CtTypeAccess && ctExpression instanceof CtTypeAccess) {
                CtTypeAccess srcTypeAccess = (CtTypeAccess) srcNode;
                CtTypeAccess dstTypeAccess = (CtTypeAccess) ctExpression;
                if (srcTypeAccess.getAccessedType().getSimpleName().equals(
                        dstTypeAccess.getAccessedType().getSimpleName())) {
                    wasVariableWrapped = true;
                    break;
                }
            } else if (srcNode instanceof CtInvocation && ctExpression instanceof CtInvocation) {
                CtInvocation originalInvocation = (CtInvocation) srcNode;
                CtInvocation newInvocation = (CtInvocation) ctExpression;
                if (originalInvocation.getExecutable().getSignature().equals(
                        newInvocation.getExecutable().getSignature())) {
                    wasVariableWrapped = true;
                    break;
                }
            } else if (srcNode instanceof CtConstructorCall
                    && ctExpression instanceof CtConstructorCall) {
                CtConstructorCall originalCall = (CtConstructorCall) srcNode;
                CtConstructorCall newCall = (CtConstructorCall) ctExpression;

                if (originalCall.getExecutable().getSignature().equals(newCall.getExecutable().getSignature())) {
                    wasVariableWrapped = true;
                    break;
                }
            }
        }

        return wasVariableWrapped;
    }

    private boolean whethervarunwrapped(CtElement srcNode, CtElement node2) {

        boolean wasVariableUnWrapped = false;

        List<CtExpression> invocationArguments = new ArrayList<>();
        if (srcNode instanceof CtInvocation) {
            invocationArguments = ((CtInvocation) srcNode).getArguments();
        }
        if (srcNode instanceof CtConstructorCall) {
            invocationArguments = ((CtConstructorCall) srcNode).getArguments();
        }

        for (CtExpression ctExpression : invocationArguments) {
            if (node2 instanceof CtVariableAccess
                    && ctExpression instanceof CtVariableAccess) {
                CtVariableAccess dstVariableAccess = (CtVariableAccess) node2;
                CtVariableAccess srcVariableAccess = (CtVariableAccess) ctExpression;
                if (srcVariableAccess.getVariable().getSimpleName().equals(
                        dstVariableAccess.getVariable().getSimpleName())) {
                    wasVariableUnWrapped = true;
                    break;
                }
            } else if (node2 instanceof CtTypeAccess
                    && ctExpression instanceof CtTypeAccess) {
                CtTypeAccess dstTypeAccess = (CtTypeAccess) node2;
                CtTypeAccess srcTypeAccess = (CtTypeAccess) ctExpression;
                if (srcTypeAccess.getAccessedType().getSimpleName().equals(
                        dstTypeAccess.getAccessedType().getSimpleName())) {
                    wasVariableUnWrapped = true;
                    break;
                }
            } else if (node2 instanceof CtInvocation
                    && ctExpression instanceof CtInvocation) {
                CtInvocation newinvocation = (CtInvocation) node2;
                CtInvocation otherinvocation = (CtInvocation) ctExpression;
                if (newinvocation.getExecutable().getSignature().equals(
                        otherinvocation.getExecutable().getSignature())) {
                    wasVariableUnWrapped = true;
                    break;
                }
            } else if (node2 instanceof CtConstructorCall
                    && ctExpression instanceof CtConstructorCall) {
                CtConstructorCall newcall = (CtConstructorCall) node2;
                CtConstructorCall othercall = (CtConstructorCall) ctExpression;

                if (newcall.getExecutable().getSignature().equals(
                        othercall.getExecutable().getSignature())) {
                    wasVariableUnWrapped = true;
                    break;
                }
            }
        }

        return wasVariableUnWrapped;
    }

    private boolean whetherargumentnumberdifferent(CtElement inputelement) {

        if (inputelement instanceof CtInvocation) {
            ITree treeoldinvocation = MappingAnalysis.getRightFromLeftNodeMapped(diff, inputelement);
            if (treeoldinvocation != null) {
                CtElement newinvocation = (CtElement) treeoldinvocation.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);

                if (newinvocation != null && newinvocation instanceof CtInvocation && ((CtInvocation) inputelement).getArguments().size()
                        != ((CtInvocation) newinvocation).getArguments().size())
                    return true;
            }
        }

        if (inputelement instanceof CtConstructorCall) {
            ITree treeoldcon = MappingAnalysis.getRightFromLeftNodeMapped(diff, inputelement);
            if (treeoldcon != null) {
                CtElement newcon = (CtElement) treeoldcon.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT);

                return newcon != null && newcon instanceof CtConstructorCall && ((CtConstructorCall) inputelement).getArguments().size()
                        != ((CtConstructorCall) newcon).getArguments().size();
            }
        }

        return false;
    }
}
