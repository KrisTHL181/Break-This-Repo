package rip.ysm.compat.ironsspellbooks.fabric;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.client.entity.LivingAnimatable;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.enums.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import net.minecraft.world.entity.LivingEntity;

public final class SpellbooksCompatImpl {

    private SpellbooksCompatImpl() {
    }

    public static boolean isLoaded() {
        return false;
    }

    public static void registerBindings(CtrlBinding binding) {
        binding.clientPlayerEntityVar("iss_animation", ctx -> StringPool.EMPTY);
    }

    public static PlayState resolvePlayState(AnimationEvent<LivingAnimatable<?>> event, LivingEntity entity) {
        return null;
    }
}
