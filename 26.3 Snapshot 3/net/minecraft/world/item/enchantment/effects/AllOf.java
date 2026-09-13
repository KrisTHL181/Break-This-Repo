package net.minecraft.world.item.enchantment.effects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Function;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.phys.Vec3;

public interface AllOf {
   static <T, A extends T> MapCodec<A> codec(final Codec<T> topLevelCodec, final Function<List<T>, A> constructor, final Function<A, List<T>> accessor) {
      return RecordCodecBuilder.mapCodec(i -> i.group(topLevelCodec.listOf().fieldOf("effects").forGetter(accessor)).apply(i, constructor));
   }

   static AllOf.EntityEffects entityEffects(final EnchantmentEntityEffect... effects) {
      return new AllOf.EntityEffects(List.of(effects));
   }

   static AllOf.LocationBasedEffects locationBasedEffects(final EnchantmentLocationBasedEffect... effects) {
      return new AllOf.LocationBasedEffects(List.of(effects));
   }

   static AllOf.ValueEffects valueEffects(final EnchantmentValueEffect... effects) {
      return new AllOf.ValueEffects(List.of(effects));
   }

   record EntityEffects(List<EnchantmentEntityEffect> effects) implements EnchantmentEntityEffect {
      public static final MapCodec<AllOf.EntityEffects> CODEC = AllOf.codec(
         EnchantmentEntityEffect.CODEC, AllOf.EntityEffects::new, AllOf.EntityEffects::effects
      );

      @Override
      public void apply(final ServerLevel serverLevel, final int enchantmentLevel, final EnchantedItemInUse item, final Entity entity, final Vec3 position) {
         for (EnchantmentEntityEffect effect : this.effects) {
            effect.apply(serverLevel, enchantmentLevel, item, entity, position);
         }
      }

      @Override
      public MapCodec<AllOf.EntityEffects> codec() {
         return CODEC;
      }
   }

   record LocationBasedEffects(List<EnchantmentLocationBasedEffect> effects) implements EnchantmentLocationBasedEffect {
      public static final MapCodec<AllOf.LocationBasedEffects> CODEC = AllOf.codec(
         EnchantmentLocationBasedEffect.CODEC, AllOf.LocationBasedEffects::new, AllOf.LocationBasedEffects::effects
      );

      @Override
      public void onChangedBlock(
         final ServerLevel serverLevel,
         final int enchantmentLevel,
         final EnchantedItemInUse item,
         final Entity entity,
         final Vec3 position,
         final boolean becameActive
      ) {
         for (EnchantmentLocationBasedEffect effect : this.effects) {
            effect.onChangedBlock(serverLevel, enchantmentLevel, item, entity, position, becameActive);
         }
      }

      @Override
      public void onDeactivated(final EnchantedItemInUse item, final Entity entity, final Vec3 position, final int level) {
         for (EnchantmentLocationBasedEffect effect : this.effects) {
            effect.onDeactivated(item, entity, position, level);
         }
      }

      @Override
      public MapCodec<AllOf.LocationBasedEffects> codec() {
         return CODEC;
      }
   }

   record ValueEffects(List<EnchantmentValueEffect> effects) implements EnchantmentValueEffect {
      public static final MapCodec<AllOf.ValueEffects> CODEC = AllOf.codec(EnchantmentValueEffect.CODEC, AllOf.ValueEffects::new, AllOf.ValueEffects::effects);

      @Override
      public float process(final int enchantmentLevel, final RandomSource random, float value) {
         for (EnchantmentValueEffect effect : this.effects) {
            value = effect.process(enchantmentLevel, random, value);
         }

         return value;
      }

      @Override
      public MapCodec<AllOf.ValueEffects> codec() {
         return CODEC;
      }
   }
}
