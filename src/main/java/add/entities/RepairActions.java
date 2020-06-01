package add.entities;

import utils.MapList;
import spoon.reflect.declaration.CtElement;

/**
 * Created by tdurieux
 */
public class RepairActions extends Feature {

    MapList<String, CtElement> elementPerFeature = new MapList<>();

    public void incrementFeatureCounter(String key, CtElement element) {
        super.incrementFeatureCounter(key);
        elementPerFeature.add(key, element);
    }

    @FeatureAnnotation(key = "assignAdd", name = "Assignment addition")
    private int assignAdd = 0;

    @FeatureAnnotation(key = "assignRem", name = "Assignment removal")
    private int assignRem = 0;

    @FeatureAnnotation(key = "assignExpChange", name = "Assignment expression modification")
    private int assignExpChange = 0;

    @FeatureAnnotation(key = "condBranIfAdd", name = "Conditional (if) branch addition")
    private int condBranIfAdd = 0;

    @FeatureAnnotation(key = "condBranIfElseAdd", name = "Conditional (if-else) branches addition")
    private int condBranIfElseAdd = 0;

    @FeatureAnnotation(key = "condBranElseAdd", name = "Conditional (else) branch addition")
    private int condBranElseAdd = 0;

    @FeatureAnnotation(key = "condBranCaseAdd", name = "Conditional (case in switch) branch addition")
    private int condBranCaseAdd = 0;

    @FeatureAnnotation(key = "condBranRem", name = "Conditional (if or else) branch removal")
    private int condBranRem = 0;

    @FeatureAnnotation(key = "condExpExpand", name = "Conditional expression expansion")
    private int condExpExpand = 0;

    @FeatureAnnotation(key = "condExpRed", name = "Conditional expression reduction")
    private int condExpRed = 0;

    @FeatureAnnotation(key = "condExpMod", name = "Conditional expression modification")
    private int condExpMod = 0;

    @FeatureAnnotation(key = "loopAdd", name = "Loop addition")
    private int loopAdd = 0;

    @FeatureAnnotation(key = "loopRem", name = "Loop removal")
    private int loopRem = 0;

    @FeatureAnnotation(key = "loopCondChange", name = "Loop conditional expression modification")
    private int loopCondChange = 0;

    @FeatureAnnotation(key = "loopInitChange", name = "Loop initialization field modification")
    private int loopInitChange = 0;

    @FeatureAnnotation(key = "mcAdd", name = "Method call addition")
    private int mcAdd = 0;

    @FeatureAnnotation(key = "mcRem", name = "Method call removal")
    private int mcRem = 0;

    @FeatureAnnotation(key = "mcRepl", name = "Method call replacement")
    private int mcRepl = 0;

    @FeatureAnnotation(key = "mcMove", name = "Method call moving")
    private int mcMove = 0;

    @FeatureAnnotation(key = "mcParAdd", name = "Method call parameter addition")
    private int mcParAdd = 0;

    @FeatureAnnotation(key = "mcParRem", name = "Method call parameter removal")
    private int mcParRem = 0;

    @FeatureAnnotation(key = "mcParSwap", name = "Method call parameter value swapping")
    private int mcParSwap = 0;

    @FeatureAnnotation(key = "mcParValChange", name = "Method call parameter value modification")
    private int mcParValChange = 0;

    @FeatureAnnotation(key = "mdAdd", name = "Method definition addition")
    private int mdAdd = 0;

    @FeatureAnnotation(key = "mdRem", name = "Method definition removal")
    private int mdRem = 0;

    @FeatureAnnotation(key = "mdRen", name = "Method definition renaming")
    private int mdRen = 0;

    @FeatureAnnotation(key = "mdParAdd", name = "Parameter addition in method definition")
    private int mdParAdd = 0;

    @FeatureAnnotation(key = "mdParRem", name = "Parameter removal from method definition")
    private int mdParRem = 0;

    @FeatureAnnotation(key = "mdParTyChange", name = "Parameter type modification in method definition")
    private int mdParTyChange = 0;

    @FeatureAnnotation(key = "mdRetTyChange", name = "Method return type modification")
    private int mdRetTyChange = 0;

    @FeatureAnnotation(key = "mdModChange", name = "Method modifier change")
    private int mdModChange = 0;

    @FeatureAnnotation(key = "mdOverride", name = "Method overriding addition or removal")
    private int mdOverride = 0;

    @FeatureAnnotation(key = "objInstAdd", name = "Object instantiation addition")
    private int objInstAdd = 0;

    @FeatureAnnotation(key = "objInstRem", name = "Object instantiation removal")
    private int objInstRem = 0;

    @FeatureAnnotation(key = "objInstMod", name = "Object instantiation modification")
    private int objInstMod = 0;

    @FeatureAnnotation(key = "exTryCatchAdd", name = "undefined")
    private int exTryCatchAdd = 0;

    @FeatureAnnotation(key = "exTryCatchRem", name = "undefined")
    private int exTryCatchRem = 0;

    @FeatureAnnotation(key = "exThrowsAdd", name = "undefined")
    private int exThrowsAdd = 0;

    @FeatureAnnotation(key = "exThrowsRem", name = "undefined")
    private int exThrowsRem = 0;

    @FeatureAnnotation(key = "retBranchAdd", name = "Return statement addition")
    private int retBranchAdd = 0;

    @FeatureAnnotation(key = "retRem", name = "Return statement removal")
    private int retRem = 0;

    @FeatureAnnotation(key = "retExpChange", name = "Return expression modification")
    private int retExpChange = 0;

    @FeatureAnnotation(key = "varAdd", name = "Variable addition")
    private int varAdd = 0;

    @FeatureAnnotation(key = "varRem", name = "Variable removal")
    private int varRem = 0;

    @FeatureAnnotation(key = "varTyChange", name = "Variable type change")
    private int varTyChange = 0;

    @FeatureAnnotation(key = "varModChange", name = "Variable modifier change")
    private int varModChange = 0;

    @FeatureAnnotation(key = "varReplVar", name = "Variable replacement by another variable")
    private int varReplVar = 0;

    @FeatureAnnotation(key = "varReplMc", name = "Variable replacement by method call")
    private int varReplMc = 0;

    @FeatureAnnotation(key = "tyAdd", name = "Type addition")
    private int tyAdd = 0;

    @FeatureAnnotation(key = "tyImpInterf", name = "Type implemented interface modification")
    private int tyImpInterf = 0;

}
