package rip.ysm.compat.slashblade;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.client.entity.LivingAnimatable;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.ILoopType;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.enums.PlayState;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class SlashBladeCompat {

    private SlashBladeCompat() {
    }

    @ExpectPlatform
    public static boolean isLoaded() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean isSlashBladeItem(ItemStack itemStack) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static String getComboAnimName(AnimationEvent<? extends LivingAnimatable<?>> event) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static PlayState handleSlashBladeAnim(LivingEntity livingEntity, AnimationEvent<? extends LivingAnimatable<?>> event, String str, ILoopType loopType) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void registerControllerFunctions(CtrlBinding ctrlBinding) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean hasNewApi() {
        throw new AssertionError();
    }
}
