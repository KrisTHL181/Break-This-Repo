package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.Set;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public record LootItemEntityPropertyCondition(Optional<EntityPredicate> predicate, LootContext.EntityTarget entityTarget) implements LootItemCondition {
   public static final MapCodec<LootItemEntityPropertyCondition> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            EntityPredicate.CODEC.optionalFieldOf("predicate").forGetter(LootItemEntityPropertyCondition::predicate),
            LootContext.EntityTarget.CODEC.fieldOf("entity").forGetter(LootItemEntityPropertyCondition::entityTarget)
         )
         .apply(i, LootItemEntityPropertyCondition::new)
   );

   @Override
   public MapCodec<LootItemEntityPropertyCondition> codec() {
      return MAP_CODEC;
   }

   @Override
   public Set<ContextKey<?>> getReferencedContextParams() {
      return Set.of(LootContextParams.ORIGIN, this.entityTarget.contextParam());
   }

   public boolean test(final LootContext context) {
      Entity entity = context.getOptionalParameter(this.entityTarget.contextParam());
      Vec3 pos = context.getOptionalParameter(LootContextParams.ORIGIN);
      return this.predicate.isEmpty() || this.predicate.get().matches(context.getLevel(), pos, entity);
   }

   public static LootItemCondition.Builder entityPresent(final LootContext.EntityTarget target) {
      return hasProperties(target, EntityPredicate.Builder.entity());
   }

   public static LootItemCondition.Builder hasProperties(final LootContext.EntityTarget target, final EntityPredicate.Builder predicate) {
      return () -> new LootItemEntityPropertyCondition(Optional.of(predicate.build()), target);
   }

   public static LootItemCondition.Builder hasProperties(final LootContext.EntityTarget target, final EntityPredicate predicate) {
      return () -> new LootItemEntityPropertyCondition(Optional.of(predicate), target);
   }
}
