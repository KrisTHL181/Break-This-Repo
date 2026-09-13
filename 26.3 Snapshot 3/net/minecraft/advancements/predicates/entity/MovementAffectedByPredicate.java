package net.minecraft.advancements.predicates.entity;

import com.mojang.serialization.Codec;
import net.minecraft.advancements.predicates.LocationPredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public record MovementAffectedByPredicate(LocationPredicate predicate) implements EntitySubPredicate {
   public static final Codec<MovementAffectedByPredicate> CODEC = LocationPredicate.CODEC
      .xmap(MovementAffectedByPredicate::new, MovementAffectedByPredicate::predicate);

   @Override
   public boolean matches(final Entity entity, final ServerLevel level, final @Nullable Vec3 position) {
      Vec3 onPos = Vec3.atCenterOf(entity.getBlockPosBelowThatAffectsMyMovement());
      return this.predicate.matches(level, onPos.x(), onPos.y(), onPos.z());
   }
}
