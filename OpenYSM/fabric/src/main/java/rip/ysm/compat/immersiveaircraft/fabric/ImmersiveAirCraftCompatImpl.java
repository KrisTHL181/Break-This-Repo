package rip.ysm.compat.immersiveaircraft.fabric;

import com.elfmcys.yesstevemodel.client.entity.GeckoVehicleEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import org.joml.Vector3f;

import java.util.Optional;

public final class ImmersiveAirCraftCompatImpl {

    private ImmersiveAirCraftCompatImpl() {
    }

    public static boolean isLoaded() {
        return false;
    }

    public static Optional<Vector3f> getAircraftRotation(AnimationEvent<GeckoVehicleEntity> event) {
        return Optional.empty();
    }
}
