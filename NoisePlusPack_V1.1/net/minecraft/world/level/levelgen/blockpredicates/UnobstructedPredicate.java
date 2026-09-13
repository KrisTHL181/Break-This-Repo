package net.minecraft.world.level.levelgen.blockpredicates;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.phys.shapes.Shapes;

record UnobstructedPredicate(Vec3i offset) implements BlockPredicate {
   public static MapCodec<UnobstructedPredicate> CODEC = RecordCodecBuilder.mapCodec(
      p_344628_ -> p_344628_.group(Vec3i.CODEC.optionalFieldOf("offset", Vec3i.ZERO).forGetter(UnobstructedPredicate::offset))
         .apply(p_344628_, UnobstructedPredicate::new)
   );

   @Override
   public BlockPredicateType<?> type() {
      return BlockPredicateType.UNOBSTRUCTED;
   }

   public boolean test(WorldGenLevel p_343967_, BlockPos p_344792_) {
      return p_343967_.isUnobstructed(null, Shapes.block().move(p_344792_));
   }
}
