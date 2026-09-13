package net.minecraft.advancements.predicates.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public record CubeMobPredicate(MinMaxBounds.Ints size) implements EntitySubPredicate {
   public static final Codec<CubeMobPredicate> CODEC = RecordCodecBuilder.create(
      i -> i.group(MinMaxBounds.Ints.CODEC.optionalFieldOf("size", MinMaxBounds.Ints.ANY).forGetter(CubeMobPredicate::size)).apply(i, CubeMobPredicate::new)
   );

   public static CubeMobPredicate sized(final MinMaxBounds.Ints size) {
      return new CubeMobPredicate(size);
   }

   @Override
   public boolean matches(final Entity entity, final ServerLevel level, final @Nullable Vec3 position) {
      return entity instanceof AbstractCubeMob cubeMob ? this.size.matches(cubeMob.getSize()) : false;
   }
}
