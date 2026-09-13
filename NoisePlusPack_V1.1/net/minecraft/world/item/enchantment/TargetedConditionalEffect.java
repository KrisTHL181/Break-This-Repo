package net.minecraft.world.item.enchantment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public record TargetedConditionalEffect<T>(EnchantmentTarget enchanted, EnchantmentTarget affected, T effect, Optional<LootItemCondition> requirements) {
   public static <S> Codec<TargetedConditionalEffect<S>> codec(Codec<S> p_345149_, ContextKeySet p_365659_) {
      return RecordCodecBuilder.create(
         p_359932_ -> p_359932_.group(
               EnchantmentTarget.CODEC.fieldOf("enchanted").forGetter(TargetedConditionalEffect::enchanted),
               EnchantmentTarget.CODEC.fieldOf("affected").forGetter(TargetedConditionalEffect::affected),
               p_345149_.fieldOf("effect").forGetter(TargetedConditionalEffect::effect),
               ConditionalEffect.conditionCodec(p_365659_).optionalFieldOf("requirements").forGetter(TargetedConditionalEffect::requirements)
            )
            .apply(p_359932_, TargetedConditionalEffect::new)
      );
   }

   public static <S> Codec<TargetedConditionalEffect<S>> equipmentDropsCodec(Codec<S> p_343050_, ContextKeySet p_363330_) {
      return RecordCodecBuilder.create(
         p_359935_ -> p_359935_.group(
               EnchantmentTarget.CODEC
                  .validate(
                     p_342627_ -> p_342627_ != EnchantmentTarget.DAMAGING_ENTITY
                        ? DataResult.success(p_342627_)
                        : DataResult.error(() -> "enchanted must be attacker or victim")
                  )
                  .fieldOf("enchanted")
                  .forGetter(TargetedConditionalEffect::enchanted),
               p_343050_.fieldOf("effect").forGetter(TargetedConditionalEffect::effect),
               ConditionalEffect.conditionCodec(p_363330_).optionalFieldOf("requirements").forGetter(TargetedConditionalEffect::requirements)
            )
            .apply(p_359935_, (p_345323_, p_343604_, p_343198_) -> new TargetedConditionalEffect<>(p_345323_, EnchantmentTarget.VICTIM, p_343604_, p_343198_))
      );
   }

   public boolean matches(LootContext p_343867_) {
      return this.requirements.isEmpty() ? true : this.requirements.get().test(p_343867_);
   }
}
