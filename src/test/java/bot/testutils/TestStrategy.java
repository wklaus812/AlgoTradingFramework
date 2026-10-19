package bot.testutils;

import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;

public class TestStrategy implements Strategy {

    private boolean enterNext;
    private boolean exitNext;

    public TestStrategy() {}

    @Override
    public boolean shouldEnter(int index) {
        return enterNext;
    }

    @Override
    public boolean shouldExit(int index) {
        return exitNext;
    }

    public void setEnterNext(boolean enterNext) {
        this.enterNext = enterNext;
    }

    public void setExitNext(boolean exitNext) {
        this.exitNext = exitNext;
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public Rule getEntryRule() {
        return null;
    }

    @Override
    public Rule getExitRule() {
        return null;
    }

    @Override
    public Strategy and(Strategy strategy) {
        return null;
    }

    @Override
    public Strategy or(Strategy strategy) {
        return null;
    }

    @Override
    public Strategy and(String name, Strategy strategy, int unstableBars) {
        return null;
    }

    @Override
    public Strategy or(String name, Strategy strategy, int unstableBars) {
        return null;
    }

    @Override
    public Strategy opposite() {
        return null;
    }

    @Override
    public void setUnstableBars(int unstableBars) {

    }

    @Override
    public int getUnstableBars() {
        return 0;
    }

    @Override
    public boolean isUnstableAt(int index) {
        return false;
    }
}
