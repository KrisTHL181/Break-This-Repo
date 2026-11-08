package rip.ysm.compat.ironsspellbooks.forge;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.client.entity.LivingAnimatable;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.enums.PlayState;
import net.minecraft.world.entity.LivingEntity;
import com.elfmcys.yesstevemodel.client.compat.ironsspellbooks.SpellbooksCompat;

public final class SpellbooksCompatImpl {

    private SpellbooksCompatImpl() {
    }

    public static boolean isLoaded() {
        return SpellbooksCompat.isLoaded();
    }

    public static void registerBindings(CtrlBinding binding) {
        SpellbooksCompat.registerBindings(binding);
    }

    public static PlayState resolvePlayState(AnimationEvent<LivingAnimatable<?>> event, LivingEntity entity) {
        return SpellbooksCompat.resolvePlayState(event, entity);
    }
}
