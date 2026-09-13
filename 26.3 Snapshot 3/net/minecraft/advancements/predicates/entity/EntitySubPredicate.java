package net.minecraft.advancements.predicates.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public interface EntitySubPredicate {
   EntitySubPredicate ALWAYS_TRUE = (var0, var1, var2) -> true;

   boolean matches(Entity entity, ServerLevel level, @Nullable Vec3 position);

   default EntitySubPredicate and(final EntitySubPredicate other) {
      return (entity, level, position) -> this.matches(entity, level, position) && other.matches(entity, level, position);
   }
}
