package rip.ysm.compat.touhoulittlemaid.forge;

import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.capability.MaidCapabilityProvider;
import net.minecraft.world.entity.Entity;

import java.util.Optional;

public final class MaidCapabilityBridgeImpl {

    private MaidCapabilityBridgeImpl() {
    }

    public static Optional<Object> get(Entity entity) {
        return entity.getCapability(MaidCapabilityProvider.MAID_CAP).resolve().map(c -> (Object) c);
    }
}
