package rip.ysm.compat.simpleplanes.forge;

import com.elfmcys.yesstevemodel.client.entity.GeckoVehicleEntity;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import org.joml.Vector3f;

import java.util.Optional;
import com.elfmcys.yesstevemodel.client.compat.simpleplanes.SimplePlanesCompat;

public final class SimplePlanesCompatImpl {

    private SimplePlanesCompatImpl() {
    }

    public static boolean isLoaded() {
        return SimplePlanesCompat.isLoaded();
    }

    public static Optional<Vector3f> getSimplePlanesRotation(AnimationEvent<GeckoVehicleEntity> event) {
        return SimplePlanesCompat.getSimplePlanesRotation(event);
    }
}
