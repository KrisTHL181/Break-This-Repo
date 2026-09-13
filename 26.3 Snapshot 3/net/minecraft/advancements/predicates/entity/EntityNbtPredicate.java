package net.minecraft.advancements.predicates.entity;

import com.mojang.serialization.Codec;
import net.minecraft.advancements.predicates.NbtPredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public record EntityNbtPredicate(NbtPredicate nbt) implements EntitySubPredicate {
   public static final Codec<EntityNbtPredicate> CODEC = NbtPredicate.CODEC.xmap(EntityNbtPredicate::new, EntityNbtPredicate::nbt);

   @Override
   public boolean matches(final Entity entity, final ServerLevel level, final @Nullable Vec3 position) {
      return this.nbt.matches(NbtPredicate.getEntityTagToCompare(entity));
   }
}
