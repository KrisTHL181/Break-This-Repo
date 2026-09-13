package net.minecraft.advancements.predicates.entity;

import com.mojang.serialization.Codec;
import java.util.Map;
import net.minecraft.core.component.predicates.DataComponentPredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public record EntityPartialComponentsPredicate(Map<DataComponentPredicate.Type<?>, DataComponentPredicate> predicates) implements EntitySubPredicate {
   public static final Codec<EntityPartialComponentsPredicate> CODEC = DataComponentPredicate.CODEC
      .xmap(EntityPartialComponentsPredicate::new, EntityPartialComponentsPredicate::predicates);

   @Override
   public boolean matches(final Entity entity, final ServerLevel level, final @Nullable Vec3 position) {
      for (DataComponentPredicate predicate : this.predicates.values()) {
         if (!predicate.matches(entity)) {
            return false;
         }
      }

      return true;
   }
}
