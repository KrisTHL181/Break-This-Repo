package com.elfmcys.yesstevemodel.forge;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.AuthModelsCapability;
import com.elfmcys.yesstevemodel.capability.ModelInfoCapability;
import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import com.elfmcys.yesstevemodel.capability.ProjectileCapability;
import com.elfmcys.yesstevemodel.capability.ProjectileModelCapability;
import com.elfmcys.yesstevemodel.capability.StarModelsCapability;
import com.elfmcys.yesstevemodel.capability.VehicleCapability;
import com.elfmcys.yesstevemodel.capability.VehicleModelCapability;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import rip.ysm.api.PlatformAPI;

@Mod(YesSteveModel.MOD_ID)
public final class YesSteveModelForge {

    public YesSteveModelForge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        EventBuses.registerModEventBus(YesSteveModel.MOD_ID, modBus);
        modBus.addListener(YesSteveModelForge::onRegisterCapabilities);
        YesSteveModel.init();
    }

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        event.register(ModelInfoCapability.class);
        event.register(ProjectileModelCapability.class);
        event.register(VehicleModelCapability.class);
        event.register(AuthModelsCapability.class);
        event.register(StarModelsCapability.class);
        if (!PlatformAPI.isServer()) {
            event.register(PlayerCapability.class);
            event.register(ProjectileCapability.class);
            event.register(VehicleCapability.class);
        }
    }
}
