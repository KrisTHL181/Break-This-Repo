package rip.ysm.compat.immersivemelodies.fabric;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import net.minecraft.world.entity.LivingEntity;
import rip.ysm.compat.immersivemelodies.ImmersiveMelodiesCompat;

public final class ImmersiveMelodiesCompatImpl {

    private ImmersiveMelodiesCompatImpl() {
    }

    public static boolean isLoaded() {
        return false;
    }

    public static void updateMelodyProgress(LivingEntity livingEntity, ImmersiveMelodiesCompat.ImmersiveMelodiesData imData) {
    }

    public static void registerBindings(CtrlBinding binding) {
        binding.livingEntityVar("im_pitch", ctx -> 0.0f);
        binding.livingEntityVar("im_volume", ctx -> 0.0f);
        binding.livingEntityVar("im_current", ctx -> 0.0f);
        binding.livingEntityVar("im_delta", ctx -> 0L);
        binding.livingEntityVar("im_time", ctx -> 0L);
    }
}
