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
      i -> i.group(
            MobEffectInstance.CODEC.listOf().fieldOf("effects").forGetter(ApplyStatusEffectsConsumeEffect::effects),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("probability", 1.0F).forGetter(ApplyStatusEffectsConsumeEffect::probability)
         )
         .apply(i, ApplyStatusEffectsConsumeEffect::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, ApplyStatusEffectsConsumeEffect> STREAM_CODEC = StreamCodec.composite(
      MobEffectInstance.STREAM_CODEC.apply(ByteBufCodecs.list()),
      ApplyStatusEffectsConsumeEffect::effects,
      ByteBufCodecs.FLOAT,
      ApplyStatusEffectsConsumeEffect::probability,
      ApplyStatusEffectsConsumeEffect::new
   );

   public ApplyStatusEffectsConsumeEffect(final MobEffectInstance effect, final float probability) {
      this(List.of(effect), probability);
   }

   public ApplyStatusEffectsConsumeEffect(final List<MobEffectInstance> effects) {
      this(effects, 1.0F);
   }

   public ApplyStatusEffectsConsumeEffect(final MobEffectInstance effect) {
      this(effect, 1.0F);
   }

   @Override
   public ConsumeEffect.Type<ApplyStatusEffectsConsumeEffect> getType() {
      return ConsumeEffect.Type.APPLY_EFFECTS;
   }

   @Override
   public boolean apply(final Level level, final ItemStack stack, final LivingEntity user) {
      if (user.getRandom().nextFloat() >= this.probability) {
         return false;
      }

      boolean anyApplied = false;

      for (MobEffectInstance effect : this.effects) {
         if (user.addEffect(new MobEffectInstance(effect))) {
            anyApplied = true;
         }
      }

      return anyApplied;
   }
}
