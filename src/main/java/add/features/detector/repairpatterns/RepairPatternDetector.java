package add.features.detector.repairpatterns;

import add.entities.RepairPatterns;
import add.features.detector.EditScriptBasedDetector;
import add.main.Config;
import gumtree.spoon.diff.Diff;
import gumtree.spoon.diff.operations.Operation;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by fermadeiral
 */
public class RepairPatternDetector extends EditScriptBasedDetector {

    private RepairPatterns repairPatterns;

    public RepairPatternDetector(Config config, Diff editScript) {
        super(config, editScript);
        this.repairPatterns = new RepairPatterns();
    }

    public RepairPatternDetector(Config config) {
        super(config);
        this.repairPatterns = new RepairPatterns();
    }

    @Override
    public RepairPatterns analyze() {
        List<Operation> operations = this.editScript.getRootOperations();

        List<AbstractPatternDetector> detectors = new ArrayList<>();
        detectors.add(new MissingNullCheckDetector(operations));
        detectors.add(new SingleLineDetector(this.config, operations));
        detectors.add(new ConditionalBlockDetector(operations));
        detectors.add(new WrapsWithDetector(operations));
        detectors.add(new CopyPasteDetector(operations));
        detectors.add(new ConstantChangeDetector(operations));
        detectors.add(new CodeMovingDetector(operations));
        detectors.add(new ExpressionFixDetector(operations));
        detectors.add(new WrongReferenceDetector(this.config, operations));
        detectors.add(new AssignmentDetector(operations));

        for (AbstractPatternDetector detector : detectors) {
            detector.setDiff(editScript);
            detector.detect(this.repairPatterns);
        }

        return this.repairPatterns;
    }

}
