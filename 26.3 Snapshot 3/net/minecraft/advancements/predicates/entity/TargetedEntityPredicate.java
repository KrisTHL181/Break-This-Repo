package net.minecraft.advancements.predicates.entity;

import com.mojang.serialization.Codec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public record TargetedEntityPredicate(EntityPredicate targetedEntity) implements EntitySubPredicate {
   public static final Codec<TargetedEntityPredicate> CODEC = EntityPredicate.CODEC.xmap(TargetedEntityPredicate::new, TargetedEntityPredicate::targetedEntity);

   @Override
   public boolean matches(final Entity entity, final ServerLevel level, final @Nullable Vec3 position) {
      return this.targetedEntity.matches(level, position, entity instanceof Mob mob ? mob.getTarget() : null);
   }
}
