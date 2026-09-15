package net.minecraft.world.item.enchantment.effects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

public record DamageImmunity() {
    public static final DamageImmunity INSTANCE = new DamageImmunity();
    public static final Codec<DamageImmunity> CODEC = MapCodec.unitCodec(INSTANCE);
}