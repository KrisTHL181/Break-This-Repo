package net.minecraft.advancements.predicates.entity;

import com.mojang.serialization.Codec;
import net.minecraft.advancements.predicates.MobEffectsPredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public record EntityEffectsPredicate(MobEffectsPredicate effects) implements EntitySubPredicate {
   public static final Codec<EntityEffectsPredicate> CODEC = MobEffectsPredicate.CODEC.xmap(EntityEffectsPredicate::new, EntityEffectsPredicate::effects);

   @Override
   public boolean matches(final Entity entity, final ServerLevel level, final @Nullable Vec3 position) {
      return this.effects.matches(entity);
   }
}
