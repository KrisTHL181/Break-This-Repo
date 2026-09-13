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
      p_270460_ -> p_270460_.group(
            Codec.STRING.fieldOf("message_id").forGetter(DamageType::msgId),
            DamageScaling.CODEC.fieldOf("scaling").forGetter(DamageType::scaling),
            Codec.FLOAT.fieldOf("exhaustion").forGetter(DamageType::exhaustion),
            DamageEffects.CODEC.optionalFieldOf("effects", DamageEffects.HURT).forGetter(DamageType::effects),
            DeathMessageType.CODEC.optionalFieldOf("death_message_type", DeathMessageType.DEFAULT).forGetter(DamageType::deathMessageType)
         )
         .apply(p_270460_, DamageType::new)
   );
   public static final Codec<Holder<DamageType>> CODEC = RegistryFixedCodec.create(Registries.DAMAGE_TYPE);
   public static final StreamCodec<RegistryFriendlyByteBuf, Holder<DamageType>> STREAM_CODEC = ByteBufCodecs.holderRegistry(Registries.DAMAGE_TYPE);

   public DamageType(String p_270099_, DamageScaling p_270717_, float p_270846_) {
      this(p_270099_, p_270717_, p_270846_, DamageEffects.HURT, DeathMessageType.DEFAULT);
   }

   public DamageType(String p_270743_, DamageScaling p_270585_, float p_270555_, DamageEffects p_270608_) {
      this(p_270743_, p_270585_, p_270555_, p_270608_, DeathMessageType.DEFAULT);
   }

   public DamageType(String p_270473_, float p_270700_, DamageEffects p_270105_) {
      this(p_270473_, DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, p_270700_, p_270105_);
   }

   public DamageType(String p_270454_, float p_270889_) {
      this(p_270454_, DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, p_270889_);
   }
}
