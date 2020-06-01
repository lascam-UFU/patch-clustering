package add.features.detector.repairpatterns;

import add.entities.RepairPatterns;
import add.main.Config;
import add.utils.TestUtils;
import org.junit.Assert;
import org.junit.Test;

public class AssignmentDetectorTest {

    @Test
    public void math5() {
        Config config = TestUtils.setupConfig("math_5");

        RepairPatternDetector detector = new RepairPatternDetector(config);
        RepairPatterns repairPatterns = detector.analyze();

        Assert.assertTrue(repairPatterns.getFeatureCounter(AssignmentDetector.ADD_ASSIGNMENT) == 0);
    }

    @Test
    public void chart2() {
        Config config = TestUtils.setupConfig("chart_2");

        RepairPatternDetector detector = new RepairPatternDetector(config);
        RepairPatterns repairPatterns = detector.analyze();

        Assert.assertTrue(repairPatterns.getFeatureCounter(AssignmentDetector.ADD_ASSIGNMENT) > 0);
    }
}