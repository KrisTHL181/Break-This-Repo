package rip.ysm.api.config.fabric;

import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;

public final class ConfigRegistrationImpl {

    private ConfigRegistrationImpl() {
    }

    public static void register(String modId, ModConfig.Type type, ForgeConfigSpec spec) {
        ForgeConfigRegistry.INSTANCE.register(modId, type, spec);
    }
}
