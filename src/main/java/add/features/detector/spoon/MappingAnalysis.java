package add.features.detector.spoon;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Addition;
import com.github.gumtreediff.actions.model.Insert;
import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.tree.ITree;
import gumtree.spoon.builder.SpoonGumTreeBuilder;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.operations.MoveOperation;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtDo;
import spoon.reflect.code.CtFor;
import spoon.reflect.code.CtForEach;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtSwitch;
import spoon.reflect.code.CtWhile;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.visitor.filter.LineFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Matias Martinez
 */
public class MappingAnalysis {

    // Those represent the max number of children we want to show in the AST i.e.,
    // the first X children from the list of children.
    public static final int MAX_CHILDREN_WHILE = 1;
    public static final int MAX_CHILDREN_DO = 1;
    public static final int MAX_CHILDREN_FOR = 3;
    public static final int MAX_CHILDREN_FOREACH = 2;
    public static final int MAX_CHILDREN_SWITCH = 1;

    public static ITree getParentInRight(Diff diff, Action affectedAction) {
        ITree parentInLeft = diff.getMappingsComp().firstMappedSrcParent(affectedAction.getNode());
        if (parentInLeft != null) {
            return diff.getMappingsComp().getDst(parentInLeft);
        }
        return diff.getMappingsComp().firstMappedDstParent(affectedAction.getNode());
    }

    public static CtElement getParentLine(LineFilter filter, CtElement element) {
        if (element instanceof CtStatement && element.getParent() instanceof CtBlock) {
            return element;
        }

        CtElement parentLine = element.getParent(filter);
        if (parentLine != null) {
            return parentLine;
            // the parent is not a statement
        } else {
            // let's see if the parent is a field
            CtField parentField = element.getParent(CtField.class);
            if (parentField != null) {
                return parentField;
            } else {
                // If the parent is neither field nor statement, return the same element
                return element;
            }
        }
    }

    public static ITree getFormattedTreeFromControlFlow(CtElement parentLine) {
        ITree lineTree = (ITree) ((parentLine.getMetadata("tree") != null) ? parentLine.getMetadata("tree")
                : parentLine.getMetadata("gtnode"));

        return getFormattedTreeFromControlFlow(lineTree, parentLine);
    }

    public static ITree getFormattedTreeFromControlFlow(ITree lineTree, CtElement parentLine) {
        if (parentLine instanceof CtIf) {
            ITree copiedIfTree = lineTree.deepCopy();
            // We keep only the first child (the condition)

            // We remove the else
            if (copiedIfTree.getChildren().size() == 3) {
                copiedIfTree.getChildren().remove(2);
            }
            // we remove the then
            if (copiedIfTree.getChildren().size() == 2) {
                copiedIfTree.getChildren().remove(1);
            }

            return copiedIfTree;
        } else if (parentLine instanceof CtWhile) {
            ITree copiedIfTree = lineTree.deepCopy();

            // The first children is the condition, the rest the then body and then
            if (copiedIfTree.getChildren().size() >= MappingAnalysis.MAX_CHILDREN_WHILE) {
                for (int i = copiedIfTree.getChildren().size() - 1; i >= MappingAnalysis.MAX_CHILDREN_WHILE; i--) {
                    copiedIfTree.getChildren().remove(i);
                }
            }

            return copiedIfTree;
        }
        if (parentLine instanceof CtFor) {
            ITree copiedIfTree = lineTree.deepCopy();

            for (int i = lineTree.getChildren().size() - 1; i >= MappingAnalysis.MAX_CHILDREN_FOR; i--) {
                copiedIfTree.getChildren().remove(i);
            }
            return copiedIfTree;
        }
        if (parentLine instanceof CtForEach) {
            ITree copiedIfTree = lineTree.deepCopy();

            for (int i = lineTree.getChildren().size() - 1; i >= MappingAnalysis.MAX_CHILDREN_FOREACH; i--) {
                copiedIfTree.getChildren().remove(i);
            }
            return copiedIfTree;
        }
        if (parentLine instanceof CtDo) {
            ITree copiedIfTree = lineTree.deepCopy();

            for (int i = lineTree.getChildren().size() - 1; i >= MappingAnalysis.MAX_CHILDREN_DO; i--) {
                copiedIfTree.getChildren().remove(i);
            }
            return copiedIfTree;
        }
        if (parentLine instanceof CtSwitch) {
            ITree copiedIfTree = lineTree.deepCopy();

            for (int i = lineTree.getChildren().size() - 1; i >= MappingAnalysis.MAX_CHILDREN_SWITCH; i--) {
                copiedIfTree.getChildren().remove(i);
            }
            return copiedIfTree;
        }

        return lineTree;
    }

    /**
     * @param diff
     * @param maction
     * @return
     */
    public static List<ITree> getFollowStatementsInLeft(Diff diff, Addition maction) {
        List<ITree> followingInLeft = new ArrayList();
        ITree parentRight;
        if (maction instanceof Insert) {
            // Node at right
            ITree affectedRight = maction.getNode();
            // Parent at right
            parentRight = affectedRight.getParent();
            int position = getPositionInParent(parentRight, affectedRight);
            if (position >= 0) {
                int nrSiblings = parentRight.getChildren().size();
                if (position == nrSiblings - 1) {
                    // The last element, let's suppose suspicious
                    List<ITree> followingSiblingsInRing = new ArrayList((parentRight.getChildren()));
                    Collections.reverse(followingSiblingsInRing);
                    computeLeftFromRight(diff, followingInLeft, followingSiblingsInRing);

                } else {
                    List<ITree> followingSiblingsInRing = parentRight.getChildren().subList(position + 1, nrSiblings);
                    computeLeftFromRight(diff, followingInLeft, followingSiblingsInRing);

                    if (followingInLeft.isEmpty()) {
                        // all the following are inserted, let's find the last one
                        // The last element, let's suppose suspicious
                        followingSiblingsInRing = new ArrayList((parentRight.getChildren()));
                        Collections.reverse(followingSiblingsInRing);
                        computeLeftFromRight(diff, followingInLeft, followingSiblingsInRing);
                    }
                }
            } else {
                System.out.println("Inserted node Not found in parent");
            }
        }

        return followingInLeft;
    }

    public static void computeLeftFromRight(Diff diff, List<ITree> followingInLeft,
                                            List<ITree> followingSiblingsInRing) {
        for (ITree siblingRight : followingSiblingsInRing) {
            // The mapped at the left
            ITree mappedSiblingLeft = getLeftFromRightNodeMapped(diff, siblingRight);

            if (mappedSiblingLeft != null) {
                // lets check if it's null
                boolean affectedByMoved = diff.getRootOperations().stream()
                        .filter(e -> (e instanceof MoveOperation && mappedSiblingLeft.equals(e.getAction().getNode())))
                        .findFirst().isPresent();
                // If mapped left is not moved
                if (!affectedByMoved) {
                    followingInLeft.add(mappedSiblingLeft);
                }
            }

        }
    }

    public static int getPositionInParent(ITree parent, ITree element) {
        int i = 0;
        for (ITree child : parent.getChildren()) {
            if (child == element)
                return i;
            i++;
        }
        return -1;
    }

    public static ITree getLeftFromRightNodeMapped(Diff diff, CtElement element) {
        ITree leftMoved = MappingAnalysis.getLeftFromRightNodeMapped(diff, (ITree) element.getMetadata("gtnode"));

        return getLeftFromRightNodeMapped(diff, leftMoved);
    }

    public static ITree getLeftFromRightNodeMapped(Diff diff, ITree iTree) {
        for (Mapping map : diff.getMappingsComp().asSet()) {
            if (map.getSecond().equals(iTree)) {
                return map.getFirst();
            }

            // if it's in left, we return it
            if (map.getFirst().equals(iTree)) {
                return iTree;
            }
        }

        return null;
    }

    public static ITree getRightFromLeftNodeMapped(Diff diff, CtElement element) {
        ITree rightMoved = MappingAnalysis.getRightFromLeftNodeMapped(diff, (ITree) element.getMetadata("gtnode"));

        return getRightFromLeftNodeMapped(diff, rightMoved);
    }

    public static ITree getRightFromLeftNodeMapped(Diff diff, ITree iTree) {
        for (Mapping map : diff.getMappingsComp().asSet()) {
            if (map.getFirst().equals(iTree)) {
                return map.getSecond();
            }
            // if its in right, we return it
            if (map.getSecond().equals(iTree)) {
                return iTree;
            }
        }
        return null;
    }

    public static List<CtElement> getTreeLeftMovedFromRight(Diff diff, CtElement element) {
        // Get the nodes moved in the right
        List<CtElement> movesInRight = element
                .getElements(e -> e.getMetadata("isMoved") != null && e.getMetadata("root") != null);

        List<CtElement> suspLeft = new ArrayList();
        for (CtElement ctElement : movesInRight) {

            ITree mappedLeft = MappingAnalysis.getLeftFromRightNodeMapped(diff,
                    (ITree) ctElement.getMetadata("gtnode"));
            if (mappedLeft != null) {
                suspLeft.add((CtElement) mappedLeft.getMetadata(SpoonGumTreeBuilder.SPOON_OBJECT));
            } else {
                return null;
            }
        }
        return suspLeft;
    }
}
