package net.minecraft.advancements.predicates.entity;

import com.mojang.serialization.Codec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public record PeriodicEntityTickPredicate(int periodicTick) implements EntitySubPredicate {
   public static final Codec<PeriodicEntityTickPredicate> CODEC = ExtraCodecs.POSITIVE_INT
      .xmap(PeriodicEntityTickPredicate::new, PeriodicEntityTickPredicate::periodicTick);

   @Override
   public boolean matches(final Entity entity, final ServerLevel level, final @Nullable Vec3 position) {
      return entity.tickCount % this.periodicTick == 0;
   }
}
