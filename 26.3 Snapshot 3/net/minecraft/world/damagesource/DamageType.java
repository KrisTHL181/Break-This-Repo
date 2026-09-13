package net.minecraft.world.damagesource;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryFixedCodec;

public record DamageType(String msgId, DamageScaling scaling, float exhaustion, DamageEffects effects, DeathMessageType deathMessageType) {
   public static final Codec<DamageType> DIRECT_CODEC = RecordCodecBuilder.create(
      i -> i.group(
            Codec.STRING.fieldOf("message_id").forGetter(DamageType::msgId),
            DamageScaling.CODEC.fieldOf("scaling").forGetter(DamageType::scaling),
            Codec.FLOAT.fieldOf("exhaustion").forGetter(DamageType::exhaustion),
            DamageEffects.CODEC.optionalFieldOf("effects", DamageEffects.HURT).forGetter(DamageType::effects),
            DeathMessageType.CODEC.optionalFieldOf("death_message_type", DeathMessageType.DEFAULT).forGetter(DamageType::deathMessageType)
         )
         .apply(i, DamageType::new)
   );
   public static final Codec<Holder<DamageType>> CODEC = RegistryFixedCodec.create(Registries.DAMAGE_TYPE);
   public static final StreamCodec<RegistryFriendlyByteBuf, Holder<DamageType>> STREAM_CODEC = ByteBufCodecs.holderRegistry(Registries.DAMAGE_TYPE);

   public DamageType(final String msgdId, final DamageScaling scaling, final float exhaustion) {
      this(msgdId, scaling, exhaustion, DamageEffects.HURT, DeathMessageType.DEFAULT);
   }

   public DamageType(final String msgdId, final DamageScaling scaling, final float exhaustion, final DamageEffects effects) {
      this(msgdId, scaling, exhaustion, effects, DeathMessageType.DEFAULT);
   }

   public DamageType(final String msgdId, final float exhaustion, final DamageEffects effects) {
      this(msgdId, DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, exhaustion, effects);
   }

   public DamageType(final String msgdId, final float exhaustion) {
      this(msgdId, DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, exhaustion);
   }
}
