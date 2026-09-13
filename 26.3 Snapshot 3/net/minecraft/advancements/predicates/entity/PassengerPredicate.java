package net.minecraft.advancements.predicates.entity;

import com.mojang.serialization.Codec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public record PassengerPredicate(EntityPredicate passenger) implements EntitySubPredicate {
   public static final Codec<PassengerPredicate> CODEC = EntityPredicate.CODEC.xmap(PassengerPredicate::new, PassengerPredicate::passenger);

   @Override
   public boolean matches(final Entity entity, final ServerLevel level, final @Nullable Vec3 position) {
      for (Entity passenger : entity.getPassengers()) {
         if (this.passenger.matches(level, position, passenger)) {
            return true;
         }
      }

      return false;
   }
}
