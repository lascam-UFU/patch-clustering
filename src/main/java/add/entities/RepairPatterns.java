package add.entities;

import utils.MapList;
import gumtree.spoon.diff.operations.Operation;

/**
 * Created by tdurieux
 */
public class RepairPatterns extends Feature {

    MapList<String, Operation> operationsPerFeature = new MapList<>();

    MapList<String, PatternInstance> patternInstances = new MapList<>();

    public void incrementFeatureCounter(String key, Operation operations) {
        super.incrementFeatureCounter(key);
        operationsPerFeature.add(key, operations);
    }

    public void incrementFeatureCounterInstance(String key, PatternInstance operations) {
        super.incrementFeatureCounter(key);
        patternInstances.add(key, operations);
    }

    @FeatureAnnotation(key = "condBlockOthersAdd", name = "Conditional block addition")
    private int condBlockOthersAdd = 0;

    @FeatureAnnotation(key = "condBlockRetAdd", name = "Conditional block addition with return statement")
    private int condBlockRetAdd = 0;

    @FeatureAnnotation(key = "condBlockExcAdd", name = "Conditional block addition with exception throwing")
    private int condBlockExcAdd = 0;

    @FeatureAnnotation(key = "condBlockRem", name = "Conditional block removal")
    private int condBlockRem = 0;

    @FeatureAnnotation(key = "expLogicExpand", name = "Logic expression expansion")
    private int expLogicExpand = 0;

    @FeatureAnnotation(key = "expLogicReduce", name = "Logic expression reduction")
    private int expLogicReduce = 0;

    @FeatureAnnotation(key = "expLogicMod", name = "Logic expression modification")
    private int expLogicMod = 0;

    @FeatureAnnotation(key = "expArithMod", name = "Arithmetic expression modification")
    private int expArithMod = 0;

    @FeatureAnnotation(key = "wrapsIf", name = "Wraps-with if statement")
    private int wrapsIf = 0;

    @FeatureAnnotation(key = "wrapsIfElse", name = "Wraps-with if-else statement")
    private int wrapsIfElse = 0;

    @FeatureAnnotation(key = "wrapsElse", name = "Wraps-with else statement")
    private int wrapsElse = 0;

    @FeatureAnnotation(key = "wrapsTryCatch", name = "Wraps-with try-catch block")
    private int wrapsTryCatch = 0;

    @FeatureAnnotation(key = "wrapsMethod", name = "Wraps-with method call")
    private int wrapsMethod = 0;

    @FeatureAnnotation(key = "wrapsLoop", name = "Wraps-with loop")
    private int wrapsLoop = 0;

    @FeatureAnnotation(key = "unwrapIfElse", name = "Unwraps-from if-else statement")
    private int unwrapIfElse = 0;

    @FeatureAnnotation(key = "unwrapMethod", name = "Unwraps-from method call")
    private int unwrapMethod = 0;

    @FeatureAnnotation(key = "unwrapTryCatch", name = "Unwraps-from try-catch block")
    private int unwrapTryCatch = 0;

    @FeatureAnnotation(key = "wrongVarRef", name = "Wrong Variable Reference")
    private int wrongVarRef = 0;

    @FeatureAnnotation(key = "wrongMethodRef", name = "Wrong Method Reference")
    private int wrongMethodRef = 0;

    @FeatureAnnotation(key = "missNullCheckP", name = "Missing null check addition")
    private int missNullCheckP = 0;

    @FeatureAnnotation(key = "missNullCheckN", name = "Missing non-null check addition")
    private int missNullCheckN = 0;

    @FeatureAnnotation(key = "singleLine", name = "Single Line")
    private int singleLine = 0;

    @FeatureAnnotation(key = "copyPaste", name = "Copy/Paste")
    private int copyPaste = 0;

    @FeatureAnnotation(key = "constChange", name = "Constant Change")
    private int constChange = 0;

    @FeatureAnnotation(key = "codeMove", name = "Code Moving")
    private int codeMove = 0;

    @FeatureAnnotation(key = "notClassified", name = "undefined")
    private int notClassified = 0;

    @FeatureAnnotation(key = "binOperatorMod", name = "Modify binary operator")
    private int binOperatorMod = 0;

    @FeatureAnnotation(key = "addAssignment", name = "Add assignment")
    private int addAssignment = 0;

    public MapList<String, PatternInstance> getPatternInstances() {
        return patternInstances;
    }
}
