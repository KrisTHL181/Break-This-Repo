package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTypes;

public class BeehiveDecorator extends TreeDecorator {
   public static final MapCodec<BeehiveDecorator> CODEC = Codec.floatRange(0.0F, 1.0F).fieldOf("probability").xmap(BeehiveDecorator::new, d -> d.probability);
   private static final Direction WORLDGEN_FACING = Direction.SOUTH;
   private static final Direction[] SPAWN_DIRECTIONS = Direction.Plane.HORIZONTAL
      .stream()
      .filter(dir -> dir != WORLDGEN_FACING.getOpposite())
      .toArray(Direction[]::new);
   private final float probability;

   public BeehiveDecorator(final float probability) {
      this.probability = probability;
   }

   @Override
   protected TreeDecoratorType<?> type() {
      return TreeDecoratorType.BEEHIVE;
   }

   @Override
   public void place(final TreeDecorator.Context context) {
      List<BlockPos> leaves = context.leaves();
      List<BlockPos> logs = context.logs();
      if (!logs.isEmpty()) {
         RandomSource random = context.random();
         if (!(random.nextFloat() >= this.probability)) {
            int hiveY = !leaves.isEmpty()
               ? Math.max(leaves.getFirst().getY() - 1, logs.getFirst().getY() + 1)
               : Math.min(logs.getFirst().getY() + 1 + random.nextInt(3), logs.getLast().getY());
            List<BlockPos> hivePlacements = logs.stream()
               .filter(pos -> pos.getY() == hiveY)
               .flatMap(pos -> Stream.of(SPAWN_DIRECTIONS).map(pos::relative))
               .collect(Collectors.toList());
            if (!hivePlacements.isEmpty()) {
               Util.shuffle(hivePlacements, random);
               Optional<BlockPos> hivePos = hivePlacements.stream()
                  .filter(pos -> context.isAir(pos) && context.isAir(pos.relative(WORLDGEN_FACING)))
                  .findFirst();
               if (!hivePos.isEmpty()) {
                  context.setBlock(hivePos.get(), Blocks.BEE_NEST.defaultBlockState().setValue(BeehiveBlock.FACING, WORLDGEN_FACING));
                  context.level().getBlockEntity(hivePos.get(), BlockEntityTypes.BEEHIVE).ifPresent(beehive -> {
                     int numBees = 2 + random.nextInt(2);

                     for (int count = 0; count < numBees; count++) {
                        beehive.storeBee(BeehiveBlockEntity.Occupant.create(random.nextInt(599)));
                     }
                  });
               }
            }
         }
      }
   }
}
