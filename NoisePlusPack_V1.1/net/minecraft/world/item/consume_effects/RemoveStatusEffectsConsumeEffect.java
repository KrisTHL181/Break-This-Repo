package net.minecraft.world.item.consume_effects;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public record RemoveStatusEffectsConsumeEffect(HolderSet<MobEffect> effects) implements ConsumeEffect {
   public static final MapCodec<RemoveStatusEffectsConsumeEffect> CODEC = RecordCodecBuilder.mapCodec(
      p_362123_ -> p_362123_.group(
            RegistryCodecs.homogeneousList(Registries.MOB_EFFECT).fieldOf("effects").forGetter(RemoveStatusEffectsConsumeEffect::effects)
         )
         .apply(p_362123_, RemoveStatusEffectsConsumeEffect::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, RemoveStatusEffectsConsumeEffect> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.holderSet(Registries.MOB_EFFECT), RemoveStatusEffectsConsumeEffect::effects, RemoveStatusEffectsConsumeEffect::new
   );

   public RemoveStatusEffectsConsumeEffect(Holder<MobEffect> p_368449_) {
      this(HolderSet.direct(p_368449_));
   }

   @Override
   public ConsumeEffect.Type<RemoveStatusEffectsConsumeEffect> getType() {
      return ConsumeEffect.Type.REMOVE_EFFECTS;
   }

   @Override
   public boolean apply(Level p_367717_, ItemStack p_363103_, LivingEntity p_362475_) {
      boolean flag = false;

      for (Holder<MobEffect> holder : this.effects) {
         if (p_362475_.removeEffect(holder)) {
            flag = true;
         }
      }

      return flag;
   }
}
