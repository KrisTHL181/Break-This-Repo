package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.capability.ModelInfoCapability;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;

public class MobEffectEvent {

    private MobEffectEvent() {
    }

    public static void onEffectAdded(LivingEntity entity, MobEffect effect, int amplifier) {
        if (!YesSteveModel.isAvailable() || entity.level().isClientSide()) {
            return;
        }
        if (entity instanceof ServerPlayer serverPlayer && effect != null) {
            ModelInfoCapability.get(serverPlayer).ifPresent(cap -> cap.getAnimSync().syncEffectAdded(serverPlayer, effect, amplifier + 1));
        }
    }

    public static void onEffectRemoved(LivingEntity entity, MobEffect effect) {
        if (!YesSteveModel.isAvailable() || entity.level().isClientSide()) {
            return;
        }
        if (entity instanceof ServerPlayer serverPlayer && effect != null) {
            ModelInfoCapability.get(serverPlayer).ifPresent(cap -> cap.getAnimSync().syncEffectRemoved(serverPlayer, effect));
        }
    }
}
