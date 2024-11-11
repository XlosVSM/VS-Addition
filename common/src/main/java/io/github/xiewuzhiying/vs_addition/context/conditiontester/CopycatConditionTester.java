package io.github.xiewuzhiying.vs_addition.context.conditiontester;

import io.github.xiewuzhiying.vs_addition.VSAdditionConfig;
import me.fallenbreath.conditionalmixin.api.mixin.ConditionTester;

public class CopycatConditionTester implements ConditionTester {
    @Override
    public boolean isSatisfied(String s) {
        return VSAdditionConfig.SERVER.getCreate().getCopycatMaterialMass();
    }
}