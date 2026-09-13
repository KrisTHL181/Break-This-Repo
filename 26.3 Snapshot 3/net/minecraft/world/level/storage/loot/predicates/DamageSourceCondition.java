package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.Set;
import net.minecraft.advancements.predicates.DamageSourcePredicate;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public record DamageSourceCondition(Optional<DamageSourcePredicate> predicate) implements LootItemCondition {
   public static final MapCodec<DamageSourceCondition> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(DamageSourcePredicate.CODEC.optionalFieldOf("predicate").forGetter(DamageSourceCondition::predicate)).apply(i, DamageSourceCondition::new)
   );

   @Override
   public MapCodec<DamageSourceCondition> codec() {
      return MAP_CODEC;
   }

   @Override
   public Set<ContextKey<?>> getReferencedContextParams() {
      return Set.of(LootContextParams.ORIGIN, LootContextParams.DAMAGE_SOURCE);
   }

   public boolean test(final LootContext context) {
      DamageSource damageSource = context.getOptionalParameter(LootContextParams.DAMAGE_SOURCE);
      Vec3 pos = context.getOptionalParameter(LootContextParams.ORIGIN);
      return pos != null && damageSource != null ? this.predicate.isEmpty() || this.predicate.get().matches(context.getLevel(), pos, damageSource) : false;
   }

   public static LootItemCondition.Builder hasDamageSource(final DamageSourcePredicate.Builder builder) {
      return () -> new DamageSourceCondition(Optional.of(builder.build()));
   }
}
