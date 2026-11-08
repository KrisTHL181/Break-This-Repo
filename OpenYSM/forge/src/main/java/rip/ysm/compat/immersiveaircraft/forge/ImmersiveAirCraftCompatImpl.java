package rip.ysm.compat.immersiveaircraft.forge;

import com.elfmcys.yesstevemodel.client.entity.GeckoVehicleEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import org.joml.Vector3f;

import java.util.Optional;
import com.elfmcys.yesstevemodel.client.compat.immersiveaircraft.ImmersiveAirCraftCompat;

public final class ImmersiveAirCraftCompatImpl {

    private ImmersiveAirCraftCompatImpl() {
    }

    public static boolean isLoaded() {
        return ImmersiveAirCraftCompat.isLoaded();
    }

    public static Optional<Vector3f> getAircraftRotation(AnimationEvent<GeckoVehicleEntity> event) {
        return ImmersiveAirCraftCompat.getAircraftRotation(event);
    }
}
