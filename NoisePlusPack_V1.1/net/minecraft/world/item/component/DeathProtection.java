package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ClearAllStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ConsumeEffect;

public record DeathProtection(List<ConsumeEffect> deathEffects) {
   public static final Codec<DeathProtection> CODEC = RecordCodecBuilder.create(
      p_370114_ -> p_370114_.group(ConsumeEffect.CODEC.listOf().optionalFieldOf("death_effects", List.of()).forGetter(DeathProtection::deathEffects))
         .apply(p_370114_, DeathProtection::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, DeathProtection> STREAM_CODEC = StreamCodec.composite(
      ConsumeEffect.STREAM_CODEC.apply(ByteBufCodecs.list()), DeathProtection::deathEffects, DeathProtection::new
   );
   public static final DeathProtection TOTEM_OF_UNDYING = new DeathProtection(
      List.of(
         new ClearAllStatusEffectsConsumeEffect(),
         new ApplyStatusEffectsConsumeEffect(
            List.of(
               new MobEffectInstance(MobEffects.REGENERATION, 900, 1),
               new MobEffectInstance(MobEffects.ABSORPTION, 100, 1),
               new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0)
            )
         )
      )
   );

   public void applyEffects(ItemStack p_368337_, LivingEntity p_363527_) {
      for (ConsumeEffect consumeeffect : this.deathEffects) {
         consumeeffect.apply(p_363527_.level(), p_368337_, p_363527_);
      }
   }
}
