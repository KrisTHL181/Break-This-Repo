package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

public record DamageResistant(HolderSet<DamageType> types) {
   public static final Codec<DamageResistant> CODEC = RecordCodecBuilder.create(
      i -> i.group(RegistryCodecs.homogeneousList(Registries.DAMAGE_TYPE).fieldOf("types").forGetter(DamageResistant::types)).apply(i, DamageResistant::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, DamageResistant> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.holderSet(Registries.DAMAGE_TYPE), DamageResistant::types, DamageResistant::new
   );

   public boolean isResistantTo(final DamageSource source) {
      return this.types.contains(source.typeHolder());
   }
}
