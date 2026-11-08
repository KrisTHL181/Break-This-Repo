package com.elfmcys.yesstevemodel.forge;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.forge.capability.AuthModelsCapabilityProvider;
import com.elfmcys.yesstevemodel.forge.capability.ModelInfoCapabilityProvider;
import com.elfmcys.yesstevemodel.forge.capability.PlayerCapabilityProvider;
import com.elfmcys.yesstevemodel.forge.capability.ProjectileCapabilityProvider;
import com.elfmcys.yesstevemodel.forge.capability.ProjectileModelCapabilityProvider;
import com.elfmcys.yesstevemodel.forge.capability.StarModelsCapabilityProvider;
import com.elfmcys.yesstevemodel.forge.capability.VehicleCapabilityProvider;
import com.elfmcys.yesstevemodel.forge.capability.VehicleModelCapabilityProvider;
import com.elfmcys.yesstevemodel.event.CapabilityEvent;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.S2CSetModelAndTexturePacket;
import com.elfmcys.yesstevemodel.network.message.S2CSyncProjectileModelPacket;
import com.elfmcys.yesstevemodel.network.message.S2CSyncVehicleModelPacket;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import rip.ysm.api.PlatformAPI;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

@Mod.EventBusSubscriber(modid = YesSteveModel.MOD_ID)
public final class ForgeCapabilityHooks {

    private static final ResourceLocation MODEL_INFO_CAP = new ResourceLocation(YesSteveModel.MOD_ID, "model_id");
    private static final ResourceLocation PROJECTILE_MODEL_CAP = new ResourceLocation(YesSteveModel.MOD_ID, "projectile_model_id");
    private static final ResourceLocation VEHICLE_MODEL_CAP = new ResourceLocation(YesSteveModel.MOD_ID, "vehicle_model_id");
    private static final ResourceLocation AUTH_MODELS_CAP = new ResourceLocation(YesSteveModel.MOD_ID, "own_models");
    private static final ResourceLocation STAR_MODELS_CAP = new ResourceLocation(YesSteveModel.MOD_ID, "star_models");
    private static final ResourceLocation PLAYER_CAP = new ResourceLocation(YesSteveModel.MOD_ID, "animatable");
    private static final ResourceLocation PROJECTILE_CAP = new ResourceLocation(YesSteveModel.MOD_ID, "projectile_animatable");
    private static final ResourceLocation VEHICLE_CAP = new ResourceLocation(YesSteveModel.MOD_ID, "vehicle_animatable");

    private ForgeCapabilityHooks() {
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        Entity entity = event.getObject();
        if (entity instanceof Player player) {
            if (!entity.level().isClientSide() && !player.getCapability(ModelInfoCapabilityProvider.MODEL_INFO_CAP).isPresent() && !event.getCapabilities().containsKey(MODEL_INFO_CAP)) {
                event.addCapability(MODEL_INFO_CAP, new ModelInfoCapabilityProvider());
            }
            if (!player.getCapability(AuthModelsCapabilityProvider.AUTH_MODELS_CAP).isPresent() && !event.getCapabilities().containsKey(AUTH_MODELS_CAP)) {
                event.addCapability(AUTH_MODELS_CAP, new AuthModelsCapabilityProvider());
            }
            if (!player.getCapability(StarModelsCapabilityProvider.STAR_MODELS_CAP).isPresent() && !event.getCapabilities().containsKey(STAR_MODELS_CAP)) {
                event.addCapability(STAR_MODELS_CAP, new StarModelsCapabilityProvider());
            }
        } else if (entity instanceof Projectile) {
            if (!entity.level().isClientSide() && !entity.getCapability(ProjectileModelCapabilityProvider.PROJECTILE_MODEL).isPresent() && !event.getCapabilities().containsKey(PROJECTILE_MODEL_CAP)) {
                event.addCapability(PROJECTILE_MODEL_CAP, new ProjectileModelCapabilityProvider());
            }
        } else if (!entity.level().isClientSide() && !entity.getCapability(VehicleModelCapabilityProvider.VEHICLE_MODEL_CAP).isPresent() && !event.getCapabilities().containsKey(VEHICLE_MODEL_CAP)) {
            event.addCapability(VEHICLE_MODEL_CAP, new VehicleModelCapabilityProvider());
        }
        if (!PlatformAPI.isServer() && entity.level().isClientSide()) {
            if (entity instanceof AbstractClientPlayer abstractClientPlayer) {
                if (!abstractClientPlayer.getCapability(PlayerCapabilityProvider.PLAYER_CAP).isPresent() && !event.getCapabilities().containsKey(PLAYER_CAP)) {
                    event.addCapability(PLAYER_CAP, new PlayerCapabilityProvider(abstractClientPlayer));
                    return;
                }
            }
            if (!entity.getCapability(VehicleCapabilityProvider.VEHICLE_CAP).isPresent() && !event.getCapabilities().containsKey(VEHICLE_CAP)) {
                event.addCapability(VEHICLE_CAP, new VehicleCapabilityProvider(entity));
                if (entity instanceof Projectile && !event.getCapabilities().containsKey(PROJECTILE_CAP)) {
                    event.addCapability(PROJECTILE_CAP, new ProjectileCapabilityProvider((Projectile) entity));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking startTracking) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        Entity target = startTracking.getTarget();
        if (target instanceof ServerPlayer trackPlayer) {
            Player entity = startTracking.getEntity();
            CapabilityEvent.getModelInfoCap(trackPlayer).ifPresent(cap -> {
                if (!NetworkHandler.isPlayerConnected(trackPlayer) && !cap.isMandatory()) {
                    return;
                }
                Optional<S2CSetModelAndTexturePacket> optional = cap.createSyncMessage(trackPlayer, false);
                Consumer<? super S2CSetModelAndTexturePacket> consumer = message -> NetworkHandler.sendToClientPlayer(message, entity);
                Objects.requireNonNull(cap);
                optional.ifPresentOrElse(consumer, cap::markDirty);
            });
            return;
        }
        if (target instanceof Projectile projectile) {
            projectile.getCapability(ProjectileModelCapabilityProvider.PROJECTILE_MODEL).ifPresent(cap -> {
                if (cap.isInitialized()) {
                    NetworkHandler.sendToClientPlayer(new S2CSyncProjectileModelPacket(projectile.getId(), cap), startTracking.getEntity());
                }
            });
        } else if (target != null) {
            target.getCapability(VehicleModelCapabilityProvider.VEHICLE_MODEL_CAP).ifPresent(cap -> {
                if (cap.isInitialized()) {
                    NetworkHandler.sendToClientPlayer(new S2CSyncVehicleModelPacket(target.getId(), cap), startTracking.getEntity());
                }
            });
        }
    }
}
