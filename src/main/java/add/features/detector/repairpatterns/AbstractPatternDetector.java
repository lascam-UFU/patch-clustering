package add.features.detector.repairpatterns;

import add.entities.RepairPatterns;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.operations.Operation;

import java.util.List;

/**
 * Created by fermadeiral
 */
public abstract class AbstractPatternDetector {

    protected List<Operation> operations;

    protected Diff diff;

    AbstractPatternDetector(List<Operation> operations) {
        this.operations = operations;
    }

    public abstract void detect(RepairPatterns repairPatterns);

    public Diff getDiff() {
        return diff;
    }

    public void setDiff(Diff diff) {
        this.diff = diff;
    }

}
