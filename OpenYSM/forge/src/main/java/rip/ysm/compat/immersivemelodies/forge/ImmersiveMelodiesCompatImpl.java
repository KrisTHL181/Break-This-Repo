package rip.ysm.compat.immersivemelodies.forge;

import com.elfmcys.yesstevemodel.client.animation.molang.CtrlBinding;
import com.elfmcys.yesstevemodel.client.compat.immersivemelodies.ImmersiveMelodiesCompat;
import net.minecraft.world.entity.LivingEntity;

public final class ImmersiveMelodiesCompatImpl {

    private ImmersiveMelodiesCompatImpl() {
    }

    public static boolean isLoaded() {
        return ImmersiveMelodiesCompat.isLoaded();
    }

    public static void updateMelodyProgress(LivingEntity livingEntity, rip.ysm.compat.immersivemelodies.ImmersiveMelodiesCompat.ImmersiveMelodiesData imData) {
        ImmersiveMelodiesCompat.ImmersiveMelodiesData raw = new ImmersiveMelodiesCompat.ImmersiveMelodiesData();
        raw.pitch = imData.pitch;
        raw.volume = imData.volume;
        raw.current = imData.current;
        raw.delta = imData.delta;
        raw.time = imData.time;
        ImmersiveMelodiesCompat.updateMelodyProgress(livingEntity, raw);
        imData.pitch = raw.pitch;
        imData.volume = raw.volume;
        imData.current = raw.current;
        imData.delta = raw.delta;
        imData.time = raw.time;
    }

    public static void registerBindings(CtrlBinding binding) {
        ImmersiveMelodiesCompat.registerBindings(binding);
    }
}
