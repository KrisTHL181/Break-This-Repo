package net.minecraft.world.item.consume_effects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public record ApplyStatusEffectsConsumeEffect(List<MobEffectInstance> effects, float probability) implements ConsumeEffect {
   public static final MapCodec<ApplyStatusEffectsConsumeEffect> CODEC = RecordCodecBuilder.mapCodec(
      p_365227_ -> p_365227_.group(
            MobEffectInstance.CODEC.listOf().fieldOf("effects").forGetter(ApplyStatusEffectsConsumeEffect::effects),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("probability", 1.0F).forGetter(ApplyStatusEffectsConsumeEffect::probability)
         )
         .apply(p_365227_, ApplyStatusEffectsConsumeEffect::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, ApplyStatusEffectsConsumeEffect> STREAM_CODEC = StreamCodec.composite(
      MobEffectInstance.STREAM_CODEC.apply(ByteBufCodecs.list()),
      ApplyStatusEffectsConsumeEffect::effects,
      ByteBufCodecs.FLOAT,
      ApplyStatusEffectsConsumeEffect::probability,
      ApplyStatusEffectsConsumeEffect::new
   );

   public ApplyStatusEffectsConsumeEffect(MobEffectInstance p_367512_, float p_363703_) {
      this(List.of(p_367512_), p_363703_);
   }

   public ApplyStatusEffectsConsumeEffect(List<MobEffectInstance> p_369033_) {
      this(p_369033_, 1.0F);
   }

   public ApplyStatusEffectsConsumeEffect(MobEffectInstance p_365277_) {
      this(p_365277_, 1.0F);
   }

   @Override
   public ConsumeEffect.Type<ApplyStatusEffectsConsumeEffect> getType() {
      return ConsumeEffect.Type.APPLY_EFFECTS;
   }

   @Override
   public boolean apply(Level p_367772_, ItemStack p_364419_, LivingEntity p_360713_) {
      if (p_360713_.getRandom().nextFloat() >= this.probability) {
         return false;
      }

      boolean flag = false;

      for (MobEffectInstance mobeffectinstance : this.effects) {
         if (p_360713_.addEffect(new MobEffectInstance(mobeffectinstance))) {
            flag = true;
         }
      }

      return flag;
   }
}
