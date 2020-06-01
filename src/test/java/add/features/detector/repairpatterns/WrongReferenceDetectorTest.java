package add.features.detector.repairpatterns;

import add.entities.RepairPatterns;
import add.main.Config;
import add.utils.TestUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Created by tdurieux
 *
 * Tests for the features wrongVarRef and wrongMethodRef
 */
public class WrongReferenceDetectorTest {

    @Test
    public void closure30() {
        Config config = TestUtils.setupConfig("closure_30");

        RepairPatternDetector detector = new RepairPatternDetector(config);
        RepairPatterns repairPatterns = detector.analyze();

        Assert.assertTrue(repairPatterns.getFeatureCounter("wrongMethodRef") > 0);
    }

    @Test
    public void closure37() {
        Config config = TestUtils.setupConfig("closure_37");

        RepairPatternDetector detector = new RepairPatternDetector(config);
        RepairPatterns repairPatterns = detector.analyze();

        Assert.assertTrue(repairPatterns.getFeatureCounter("wrongMethodRef") > 0);
    }

    @Ignore
    @Test
    public void closure109() {
        Config config = TestUtils.setupConfig("closure_109");

        RepairPatternDetector detector = new RepairPatternDetector(config);
        RepairPatterns repairPatterns = detector.analyze();

        Assert.assertTrue(repairPatterns.getFeatureCounter("wrongMethodRef") > 0);
    }

    @Test
    public void lang26() {
        Config config = TestUtils.setupConfig("lang_26");

        RepairPatternDetector detector = new RepairPatternDetector(config);
        RepairPatterns repairPatterns = detector.analyze();

        Assert.assertTrue(repairPatterns.getFeatureCounter("wrongMethodRef") > 0);
    }

    @Test
    public void lang42() {
        Config config = TestUtils.setupConfig("lang_42");

        RepairPatternDetector detector = new RepairPatternDetector(config);
        RepairPatterns repairPatterns = detector.analyze();

        Assert.assertTrue(repairPatterns.getFeatureCounter("wrongMethodRef") > 0);
    }

    @Test
    public void math9() {
        Config config = TestUtils.setupConfig("math_9");

        RepairPatternDetector detector = new RepairPatternDetector(config);
        RepairPatterns repairPatterns = detector.analyze();

        Assert.assertTrue(repairPatterns.getFeatureCounter("wrongMethodRef") > 0);
    }

    @Test
    public void math58() {
        Config config = TestUtils.setupConfig("math_58");

        RepairPatternDetector detector = new RepairPatternDetector(config);
        RepairPatterns repairPatterns = detector.analyze();

        Assert.assertTrue(repairPatterns.getFeatureCounter("wrongMethodRef") > 0);
    }

    @Test
    public void closure3() {
        Config config = TestUtils.setupConfig("closure_3");

        RepairPatternDetector detector = new RepairPatternDetector(config);
        RepairPatterns repairPatterns = detector.analyze();

        Assert.assertTrue(repairPatterns.getFeatureCounter("wrongMethodRef") == 0);
    }

    @Test
    public void closure25() {
        Config config = TestUtils.setupConfig("closure_25");

        RepairPatternDetector detector = new RepairPatternDetector(config);
        RepairPatterns repairPatterns = detector.analyze();

        Assert.assertTrue(repairPatterns.getFeatureCounter("wrongMethodRef") > 0);
    }

    @Test
    public void chart8() {
        Config config = TestUtils.setupConfig("chart_8");

        RepairPatternDetector detector = new RepairPatternDetector(config);
        RepairPatterns repairPatterns = detector.analyze();

        Assert.assertTrue(repairPatterns.getFeatureCounter("wrongVarRef") > 0);
    }

    @Test
    public void math15() {
        Config config = TestUtils.setupConfig("math_15");

        RepairPatternDetector detector = new RepairPatternDetector(config);
        RepairPatterns repairPatterns = detector.analyze();

        Assert.assertTrue(repairPatterns.getFeatureCounter("wrongVarRef") > 0);
    }

    @Test
    public void math33() {
        Config config = TestUtils.setupConfig("math_33");

        RepairPatternDetector detector = new RepairPatternDetector(config);
        RepairPatterns repairPatterns = detector.analyze();

        Assert.assertTrue(repairPatterns.getFeatureCounter("wrongVarRef") > 0);
    }

    @Test
    public void math61() {
        Config config = TestUtils.setupConfig("math_61");

        RepairPatternDetector detector = new RepairPatternDetector(config);
        RepairPatterns repairPatterns = detector.analyze();

        Assert.assertTrue(repairPatterns.getFeatureCounter("wrongVarRef") > 0);
    }

    @Test
    public void math64() {
        Config config = TestUtils.setupConfig("math_64");

        RepairPatternDetector detector = new RepairPatternDetector(config);
        RepairPatterns repairPatterns = detector.analyze();

        Assert.assertTrue(repairPatterns.getFeatureCounter("wrongVarRef") > 0);
    }

    @Test
    public void chart10() {
        Config config = TestUtils.setupConfig("chart_10");

        RepairPatternDetector detector = new RepairPatternDetector(config);
        RepairPatterns repairPatterns = detector.analyze();

        Assert.assertTrue(repairPatterns.getFeatureCounter("wrongVarRef") == 0);
    }
}
