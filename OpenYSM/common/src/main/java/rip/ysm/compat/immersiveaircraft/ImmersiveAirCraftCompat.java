package rip.ysm.compat.immersiveaircraft;

import com.elfmcys.yesstevemodel.client.entity.GeckoVehicleEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import dev.architectury.injectables.annotations.ExpectPlatform;
import org.joml.Vector3f;

import java.util.Optional;

public final class ImmersiveAirCraftCompat {

    private ImmersiveAirCraftCompat() {
    }

    @ExpectPlatform
    public static boolean isLoaded() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static Optional<Vector3f> getAircraftRotation(AnimationEvent<GeckoVehicleEntity> event) {
        throw new AssertionError();
    }
}
