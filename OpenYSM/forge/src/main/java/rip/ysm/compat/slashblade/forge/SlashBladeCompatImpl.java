package rip.ysm.compat.slashblade.forge;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.client.entity.LivingAnimatable;
import com.elfmcys.yesstevemodel.geckolib3.core.builder.ILoopType;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.core.enums.PlayState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import com.elfmcys.yesstevemodel.client.compat.slashblade.SlashBladeCompat;

public final class SlashBladeCompatImpl {

    private SlashBladeCompatImpl() {
    }

    public static boolean isLoaded() {
        return SlashBladeCompat.isLoaded();
    }

    public static boolean isSlashBladeItem(ItemStack itemStack) {
        return SlashBladeCompat.isSlashBladeItem(itemStack);
    }

    public static String getComboAnimName(AnimationEvent<? extends LivingAnimatable<?>> event) {
        return SlashBladeCompat.getComboAnimName(event);
    }

    public static PlayState handleSlashBladeAnim(LivingEntity livingEntity, AnimationEvent<? extends LivingAnimatable<?>> event, String str, ILoopType loopType) {
        return SlashBladeCompat.handleSlashBladeAnim(livingEntity, event, str, loopType);
    }

    public static void registerControllerFunctions(CtrlBinding ctrlBinding) {
        SlashBladeCompat.registerControllerFunctions(ctrlBinding);
    }

    public static boolean hasNewApi() {
        return SlashBladeCompat.hasNewApi();
    }
}
