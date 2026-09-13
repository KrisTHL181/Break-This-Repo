package net.minecraft.advancements.predicates.entity;

import com.mojang.serialization.Codec;
import net.minecraft.advancements.predicates.LocationPredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public record SteppingOnPredicate(LocationPredicate predicate) implements EntitySubPredicate {
   public static final Codec<SteppingOnPredicate> CODEC = LocationPredicate.CODEC.xmap(SteppingOnPredicate::new, SteppingOnPredicate::predicate);

   @Override
   public boolean matches(final Entity entity, final ServerLevel level, final @Nullable Vec3 position) {
      if (!entity.onGround()) {
         return false;
      }

      Vec3 onPos = Vec3.atCenterOf(entity.getOnPos());
      return this.predicate.matches(level, onPos.x(), onPos.y(), onPos.z());
   }
}
