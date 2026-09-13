package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.Set;
import net.minecraft.advancements.criterion.DamageSourcePredicate;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public record DamageSourceCondition(Optional<DamageSourcePredicate> predicate) implements LootItemCondition {
   public static final MapCodec<DamageSourceCondition> CODEC = RecordCodecBuilder.mapCodec(
      p_450103_ -> p_450103_.group(DamageSourcePredicate.CODEC.optionalFieldOf("predicate").forGetter(DamageSourceCondition::predicate))
         .apply(p_450103_, DamageSourceCondition::new)
   );

   @Override
   public LootItemConditionType getType() {
      return LootItemConditions.DAMAGE_SOURCE_PROPERTIES;
   }

   @Override
   public Set<ContextKey<?>> getReferencedContextParams() {
      return Set.of(LootContextParams.ORIGIN, LootContextParams.DAMAGE_SOURCE);
   }

   public boolean test(LootContext p_81592_) {
      DamageSource damagesource = p_81592_.getOptionalParameter(LootContextParams.DAMAGE_SOURCE);
      Vec3 vec3 = p_81592_.getOptionalParameter(LootContextParams.ORIGIN);
      return vec3 != null && damagesource != null ? this.predicate.isEmpty() || this.predicate.get().matches(p_81592_.getLevel(), vec3, damagesource) : false;
   }

   public static LootItemCondition.Builder hasDamageSource(DamageSourcePredicate.Builder p_457679_) {
      return () -> new DamageSourceCondition(Optional.of(p_457679_.build()));
   }
}
