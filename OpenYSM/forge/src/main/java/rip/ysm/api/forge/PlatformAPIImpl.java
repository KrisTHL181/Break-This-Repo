package rip.ysm.api.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

public final class PlatformAPIImpl {
    private PlatformAPIImpl() {
    }

    public static boolean isServer() {
        return FMLEnvironment.dist == Dist.DEDICATED_SERVER;
    }

    public static String getPlatformName() {
        return "Forge";
    }
}
