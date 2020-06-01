package add.features.detector.repairpatterns;

import add.entities.RepairPatterns;
import add.main.Config;
import add.utils.TestUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Created by fermadeiral
 *
 * Tests for the features wrapsIf, wrapsIfElse, wrapsElse, wrapsTryCatch, wrapsMethod, wrapsLoop, unwrapIfElse, unwrapTryCatch and unwrapMethod
 */
public class WrapsWithDetectorTest {

    @Test
    public void chart18() {
        Config config = TestUtils.setupConfig("chart_18");

        RepairPatternDetector detector = new RepairPatternDetector(config);
        RepairPatterns repairPatterns = detector.analyze();

        Assert.assertTrue(repairPatterns.getFeatureCounter("wrapsIf") > 0);
    }

    @Ignore
    @Test
    public void lang31() {
        Config config = TestUtils.setupConfig("lang_31");

        RepairPatternDetector detector = new RepairPatternDetector(config);
        RepairPatterns repairPatterns = detector.analyze();

        Assert.assertTrue(repairPatterns.getFeatureCounter("wrapsIfElse") > 0);
    }

    @Test
    public void closure2() {
        Config config = TestUtils.setupConfig("closure_2");

        RepairPatternDetector detector = new RepairPatternDetector(config);
        RepairPatterns repairPatterns = detector.analyze();

        Assert.assertTrue(repairPatterns.getFeatureCounter("wrapsIfElse") > 0);
    }

    @Test
    public void lang33() {
        Config config = TestUtils.setupConfig("lang_33");

        RepairPatternDetector detector = new RepairPatternDetector(config);
        RepairPatterns repairPatterns = detector.analyze();

        Assert.assertTrue(repairPatterns.getFeatureCounter("wrapsIfElse") > 0);
    }

    @Test
    public void closure111() {
        Config config = TestUtils.setupConfig("closure_111");

        RepairPatternDetector detector = new RepairPatternDetector(config);
        RepairPatterns repairPatterns = detector.analyze();

        Assert.assertTrue(repairPatterns.getFeatureCounter("wrapsIfElse") > 0);
    }

    @Test
    public void chart21() {
        Config config = TestUtils.setupConfig("chart_21");

        RepairPatternDetector detector = new RepairPatternDetector(config);
        RepairPatterns repairPatterns = detector.analyze();

        Assert.assertTrue(repairPatterns.getFeatureCounter("wrapsElse") > 0);
    }

    @Test
    public void lang17() {
        Config config = TestUtils.setupConfig("lang_17");

        RepairPatternDetector detector = new RepairPatternDetector(config);
        RepairPatterns repairPatterns = detector.analyze();

        Assert.assertTrue(repairPatterns.getFeatureCounter("unwrapIfElse") > 0);
        Assert.assertTrue(repairPatterns.getFeatureCounter("unwrapMethod") > 0);
    }

    @Test
    public void math46() {
        Config config = TestUtils.setupConfig("math_46");

        RepairPatternDetector detector = new RepairPatternDetector(config);
        RepairPatterns repairPatterns = detector.analyze();

        Assert.assertTrue(repairPatterns.getFeatureCounter("unwrapIfElse") > 0);
    }

    @Test
    public void time18() {
        Config config = TestUtils.setupConfig("time_18");

        RepairPatternDetector detector = new RepairPatternDetector(config);
        RepairPatterns repairPatterns = detector.analyze();

        Assert.assertTrue(repairPatterns.getFeatureCounter("wrapsTryCatch") > 0);
    }

    @Test
    public void closure83() {
        Config config = TestUtils.setupConfig("closure_83");

        RepairPatternDetector detector = new RepairPatternDetector(config);
        RepairPatterns repairPatterns = detector.analyze();

        Assert.assertTrue(repairPatterns.getFeatureCounter("wrapsTryCatch") > 0);
    }

    @Test
    public void math60() {
        Config config = TestUtils.setupConfig("math_60");

        RepairPatternDetector detector = new RepairPatternDetector(config);
        RepairPatterns repairPatterns = detector.analyze();

        Assert.assertTrue(repairPatterns.getFeatureCounter("unwrapTryCatch") > 0);
    }

    @Test
    public void lang13() {
        Config config = TestUtils.setupConfig("lang_13");

        RepairPatternDetector detector = new RepairPatternDetector(config);
        RepairPatterns repairPatterns = detector.analyze();

        Assert.assertTrue(repairPatterns.getFeatureCounter("unwrapTryCatch") == 0);
        Assert.assertTrue(repairPatterns.getFeatureCounter("wrapsTryCatch") > 0);
    }

    @Test
    public void lang37() {
        Config config = TestUtils.setupConfig("lang_37");

        RepairPatternDetector detector = new RepairPatternDetector(config);
        RepairPatterns repairPatterns = detector.analyze();

        Assert.assertTrue(repairPatterns.getFeatureCounter("unwrapTryCatch") == 0);
        Assert.assertTrue(repairPatterns.getFeatureCounter("wrapsTryCatch") > 0);
    }

    @Test
    public void chart10() {
        Config config = TestUtils.setupConfig("chart_10");

        RepairPatternDetector detector = new RepairPatternDetector(config);
        RepairPatterns repairPatterns = detector.analyze();

        Assert.assertTrue(repairPatterns.getFeatureCounter("wrapsMethod") > 0);
    }

    @Test
    public void chart12() {
        Config config = TestUtils.setupConfig("chart_12");

        RepairPatternDetector detector = new RepairPatternDetector(config);
        RepairPatterns repairPatterns = detector.analyze();

        Assert.assertTrue(repairPatterns.getFeatureCounter("wrapsMethod") > 0);
    }

    @Test
    public void math103() {
        Config config = TestUtils.setupConfig("math_103");

        RepairPatternDetector detector = new RepairPatternDetector(config);
        RepairPatterns repairPatterns = detector.analyze();

        Assert.assertTrue(repairPatterns.getFeatureCounter("unwrapTryCatch") == 0);
        Assert.assertTrue(repairPatterns.getFeatureCounter("wrapsTryCatch") > 0);
    }

    @Test
    public void math105() {
        Config config = TestUtils.setupConfig("math_105");

        RepairPatternDetector detector = new RepairPatternDetector(config);
        RepairPatterns repairPatterns = detector.analyze();

        Assert.assertTrue(repairPatterns.getFeatureCounter("wrapsMethod") > 0);
    }

    @Test
    public void time8() {
        Config config = TestUtils.setupConfig("time_8");

        RepairPatternDetector detector = new RepairPatternDetector(config);
        RepairPatterns repairPatterns = detector.analyze();

        Assert.assertTrue(repairPatterns.getFeatureCounter("wrapsMethod") > 0);
    }

    @Ignore
    @Test
    public void mockito14() {
        Config config = TestUtils.setupConfig("mockito_14");

        RepairPatternDetector detector = new RepairPatternDetector(config);
        RepairPatterns repairPatterns = detector.analyze();

        Assert.assertTrue(repairPatterns.getFeatureCounter("wrapsMethod") > 0);
    }

    @Ignore
    @Test
    public void math27() {
        Config config = TestUtils.setupConfig("math_27");

        RepairPatternDetector detector = new RepairPatternDetector(config);
        RepairPatterns repairPatterns = detector.analyze();

        Assert.assertTrue(repairPatterns.getFeatureCounter("unwrapMethod") > 0);
    }

    @Test
    public void time17() {
        Config config = TestUtils.setupConfig("time_17");

        RepairPatternDetector detector = new RepairPatternDetector(config);
        RepairPatterns repairPatterns = detector.analyze();

        Assert.assertTrue(repairPatterns.getFeatureCounter("unwrapMethod") > 0);
    }

    @Test
    public void closure124() {
        Config config = TestUtils.setupConfig("closure_124");

        RepairPatternDetector detector = new RepairPatternDetector(config);
        RepairPatterns repairPatterns = detector.analyze();

        Assert.assertTrue(repairPatterns.getFeatureCounter("wrapsLoop") > 0);
    }

    @Test
    public void math7() {
        Config config = TestUtils.setupConfig("math_7");

        RepairPatternDetector detector = new RepairPatternDetector(config);
        RepairPatterns repairPatterns = detector.analyze();

        Assert.assertTrue(repairPatterns.getFeatureCounter("wrapsLoop") > 0);
    }

}
