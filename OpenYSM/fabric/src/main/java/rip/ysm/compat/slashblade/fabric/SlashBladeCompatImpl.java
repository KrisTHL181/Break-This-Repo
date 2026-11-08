package rip.ysm.compat.slashblade.fabric;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.client.entity.LivingAnimatable;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.ILoopType;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.enums.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class SlashBladeCompatImpl {

    private SlashBladeCompatImpl() {
    }

    public static boolean isLoaded() {
        return false;
    }

    public static boolean isSlashBladeItem(ItemStack itemStack) {
        return false;
    }

    public static String getComboAnimName(AnimationEvent<? extends LivingAnimatable<?>> event) {
        return "";
    }

    public static PlayState handleSlashBladeAnim(LivingEntity livingEntity, AnimationEvent<? extends LivingAnimatable<?>> event, String str, ILoopType loopType) {
        return null;
    }

    public static void registerControllerFunctions(CtrlBinding ctrlBinding) {
        ctrlBinding.livingEntityVar("slashblade_animation", it -> StringPool.EMPTY);
    }

    public static boolean hasNewApi() {
        return false;
    }
}
