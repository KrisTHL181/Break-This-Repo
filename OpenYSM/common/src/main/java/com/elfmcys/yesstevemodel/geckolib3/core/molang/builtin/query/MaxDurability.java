package com.elfmcys.yesstevemodel.geckolib3.core.molang.builtin.query;

import rip.ysm.compat.cosmeticarmorreworked.CosmeticArmorHelper;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.IContext;
import com.elfmcys.yesstevemodel.geckolib3.util.MolangUtils;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.funciton.entity.LivingEntityFunction;
import com.elfmcys.yesstevemodel.molang.runtime.ExecutionContext;
import net.minecraft.world.entity.LivingEntity;

public class MaxDurability extends LivingEntityFunction {
    @Override
    public Object eval(ExecutionContext<IContext<LivingEntity>> context, ArgumentCollection arguments) {
        return CosmeticArmorHelper.getArmorItem(context.entity().entity(), MolangUtils.parseSlotType(context, arguments, 0)).getMaxDamage();
    }

    @Override
    public boolean validateArgumentSize(int size) {
        return size == 1;
    }
}