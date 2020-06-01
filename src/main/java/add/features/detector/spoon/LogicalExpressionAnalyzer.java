package add.features.detector.spoon;

import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtConditional;
import spoon.reflect.code.CtDo;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFor;
import spoon.reflect.code.CtForEach;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtSwitch;
import spoon.reflect.code.CtWhile;
import spoon.reflect.declaration.CtElement;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LogicalExpressionAnalyzer {

    public static List<CtExpression> getAllRootLogicalExpressions(CtElement parentLine) {
        List<CtExpression> logicalExpressions = new ArrayList();

        CtElement elementToStudy = retrieveElementToStudy(parentLine);

        List<CtExpression> expressionsFromFaultyLine = elementToStudy.getElements(e -> (e instanceof CtExpression)).stream()
                .map(CtExpression.class::cast).collect(Collectors.toList());

        for (int index = 0; index < expressionsFromFaultyLine.size(); index++) {
            if (isBooleanExpression(expressionsFromFaultyLine.get(index)) &&
                    !whetherParentBoolean(expressionsFromFaultyLine.get(index)) &&
                    !logicalExpressions.contains(expressionsFromFaultyLine.get(index))) {
                logicalExpressions.add(expressionsFromFaultyLine.get(index));
            }
        }

        return logicalExpressions;
    }

    public static CtElement retrieveElementToStudy(CtElement element) {

        if (element instanceof CtIf) {
            return (((CtIf) element).getCondition());
        }
        if (element instanceof CtWhile) {
            return (((CtWhile) element).getLoopingExpression());
        }
        if (element instanceof CtFor) {
            return (((CtFor) element).getExpression());
        }
        if (element instanceof CtDo) {
            return (((CtDo) element).getLoopingExpression());
        }
        if (element instanceof CtForEach) {
            return (((CtForEach) element).getExpression());
        }
        if (element instanceof CtSwitch) {
            return (((CtSwitch) element).getSelector());
        }
        return (element);
    }

    public static boolean whetherParentBoolean(CtExpression expression) {
        CtElement parent = expression;
        while (parent != null) {
            parent = parent.getParent();
            if (isBooleanExpression(parent)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isBooleanExpression(CtElement currentExpression) {
        if (currentExpression == null) {
            return false;
        }

        if (isLogicalExpression(currentExpression)) {
            return true;
        }

        if (currentExpression instanceof CtExpression) {
            CtExpression exper = (CtExpression) currentExpression;
            try {
                if (exper.getType() != null && exper.getType().unbox().toString().equals("boolean")) {
                    return true;
                }
            } catch (Exception e) {
                return false;
            }
        }

        return false;
    }

    public static boolean isLogicalExpression(CtElement currentElement) {
        if (currentElement == null) {
            return false;
        }

        if ((currentElement instanceof CtBinaryOperator)) {
            CtBinaryOperator binOp = (CtBinaryOperator) currentElement;

            if (binOp.getKind().equals(BinaryOperatorKind.AND) || binOp.getKind().equals(BinaryOperatorKind.OR)
                    || binOp.getKind().equals(BinaryOperatorKind.EQ)
                    || binOp.getKind().equals(BinaryOperatorKind.GE)
                    || binOp.getKind().equals(BinaryOperatorKind.GT)
                    || binOp.getKind().equals(BinaryOperatorKind.INSTANCEOF)
                    || binOp.getKind().equals(BinaryOperatorKind.LE)
                    || binOp.getKind().equals(BinaryOperatorKind.LT)
                    || binOp.getKind().equals(BinaryOperatorKind.NE)
                    || (binOp.getType() != null &&
                    binOp.getType().unbox().getSimpleName().equals("boolean"))) {
                return true;
            }
        }

        if (currentElement.getParent() instanceof CtConditional) {
            CtConditional cond = (CtConditional) currentElement.getParent();
            if (currentElement.equals(cond.getCondition())) {
                return true;
            }
        }

        if (currentElement.getParent() instanceof CtIf) {
            CtIf ifCond = (CtIf) currentElement.getParent();
            if (currentElement.equals(ifCond.getCondition())) {
                return true;
            }
        }

        if (currentElement.getParent() instanceof CtWhile) {
            CtWhile whileCond = (CtWhile) currentElement.getParent();
            if (currentElement.equals(whileCond.getLoopingExpression())) {
                return true;
            }
        }

        if (currentElement.getParent() instanceof CtDo) {
            CtDo doCond = (CtDo) currentElement.getParent();
            if (currentElement.equals(doCond.getLoopingExpression())) {
                return true;
            }
        }

        if (currentElement.getParent() instanceof CtFor) {
            CtFor forCond = (CtFor) currentElement.getParent();
            return currentElement.equals(forCond.getExpression());
        }

        return false;
    }
}
