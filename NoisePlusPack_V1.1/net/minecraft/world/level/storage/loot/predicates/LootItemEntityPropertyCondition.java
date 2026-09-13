package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.Set;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public record LootItemEntityPropertyCondition(Optional<EntityPredicate> predicate, LootContext.EntityTarget entityTarget) implements LootItemCondition {
   public static final MapCodec<LootItemEntityPropertyCondition> CODEC = RecordCodecBuilder.mapCodec(
      p_450112_ -> p_450112_.group(
            EntityPredicate.CODEC.optionalFieldOf("predicate").forGetter(LootItemEntityPropertyCondition::predicate),
            LootContext.EntityTarget.CODEC.fieldOf("entity").forGetter(LootItemEntityPropertyCondition::entityTarget)
         )
         .apply(p_450112_, LootItemEntityPropertyCondition::new)
   );

   @Override
   public LootItemConditionType getType() {
      return LootItemConditions.ENTITY_PROPERTIES;
   }

   @Override
   public Set<ContextKey<?>> getReferencedContextParams() {
      return Set.of(LootContextParams.ORIGIN, this.entityTarget.contextParam());
   }

   public boolean test(LootContext p_81871_) {
      Entity entity = p_81871_.getOptionalParameter(this.entityTarget.contextParam());
      Vec3 vec3 = p_81871_.getOptionalParameter(LootContextParams.ORIGIN);
      return this.predicate.isEmpty() || this.predicate.get().matches(p_81871_.getLevel(), vec3, entity);
   }

   public static LootItemCondition.Builder entityPresent(LootContext.EntityTarget p_81863_) {
      return hasProperties(p_81863_, EntityPredicate.Builder.entity());
   }

   public static LootItemCondition.Builder hasProperties(LootContext.EntityTarget p_81868_, EntityPredicate.Builder p_455399_) {
      return () -> new LootItemEntityPropertyCondition(Optional.of(p_455399_.build()), p_81868_);
   }

   public static LootItemCondition.Builder hasProperties(LootContext.EntityTarget p_81865_, EntityPredicate p_452276_) {
      return () -> new LootItemEntityPropertyCondition(Optional.of(p_452276_), p_81865_);
   }
}
