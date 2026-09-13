package net.minecraft.world.level.levelgen.blockpredicates;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.LevelAccessor;

public class HasSturdyFacePredicate implements BlockPredicate {
   private final Vec3i offset;
   private final Direction direction;
   public static final MapCodec<HasSturdyFacePredicate> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            Vec3i.offsetCodec(16).optionalFieldOf("offset", Vec3i.ZERO).forGetter(c -> c.offset),
            Direction.CODEC.fieldOf("direction").forGetter(c -> c.direction)
         )
         .apply(i, HasSturdyFacePredicate::new)
   );

   public HasSturdyFacePredicate(final Vec3i offset, final Direction direction) {
      this.offset = offset;
      this.direction = direction;
   }

   public boolean test(final LevelAccessor level, final BlockPos origin) {
      BlockPos testPosition = origin.offset(this.offset);
      return level.getBlockState(testPosition).isFaceSturdy(level, testPosition, this.direction);
   }

   @Override
   public BlockPredicateType<?> type() {
      return BlockPredicateType.HAS_STURDY_FACE;
   }
}
