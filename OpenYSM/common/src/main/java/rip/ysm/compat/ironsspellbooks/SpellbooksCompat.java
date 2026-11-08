package rip.ysm.compat.ironsspellbooks;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.client.entity.LivingAnimatable;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.enums.PlayState;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.entity.LivingEntity;

public final class SpellbooksCompat {

    private SpellbooksCompat() {
    }

    @ExpectPlatform
    public static boolean isLoaded() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void registerBindings(CtrlBinding binding) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static PlayState resolvePlayState(AnimationEvent<LivingAnimatable<?>> event, LivingEntity entity) {
        throw new AssertionError();
    }
}
