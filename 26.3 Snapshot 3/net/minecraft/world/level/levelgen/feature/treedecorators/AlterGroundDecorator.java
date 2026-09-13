package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public class AlterGroundDecorator extends TreeDecorator {
   public static final MapCodec<AlterGroundDecorator> CODEC = BlockStateProvider.CODEC.fieldOf("provider").xmap(AlterGroundDecorator::new, d -> d.provider);
   private final BlockStateProvider provider;

   public AlterGroundDecorator(final BlockStateProvider provider) {
      this.provider = provider;
   }

   @Override
   protected TreeDecoratorType<?> type() {
      return TreeDecoratorType.ALTER_GROUND;
   }

   @Override
   public void place(final TreeDecorator.Context context) {
      List<BlockPos> blockPositions = TreeFeature.getLowestTrunkOrRootOfTree(context);
      if (!blockPositions.isEmpty()) {
         int minY = blockPositions.getFirst().getY();
         blockPositions.stream().filter(pos -> pos.getY() == minY).forEach(pos -> {
            this.placeCircle(context, pos.west().north());
            this.placeCircle(context, pos.east(2).north());
            this.placeCircle(context, pos.west().south(2));
            this.placeCircle(context, pos.east(2).south(2));

            for (int i = 0; i < 5; i++) {
               int placement = context.random().nextInt(64);
               int xx = placement % 8;
               int zz = placement / 8;
               if (xx == 0 || xx == 7 || zz == 0 || zz == 7) {
                  this.placeCircle(context, pos.offset(-3 + xx, 0, -3 + zz));
               }
            }
         });
      }
   }

   private void placeCircle(final TreeDecorator.Context context, final BlockPos pos) {
      for (int xx = -2; xx <= 2; xx++) {
         for (int zz = -2; zz <= 2; zz++) {
            if (Math.abs(xx) != 2 || Math.abs(zz) != 2) {
               this.placeBlockAt(context, pos.offset(xx, 0, zz));
            }
         }
      }
   }

   private void placeBlockAt(final TreeDecorator.Context context, final BlockPos pos) {
      for (int dy = 2; dy >= -3; dy--) {
         BlockPos cursor = pos.above(dy);
         BlockState replaceWith = this.provider.getOptionalState(context.level(), context.random(), cursor);
         if (replaceWith != null) {
            context.setBlock(cursor, replaceWith);
            break;
         }

         if (!context.isAir(cursor) && dy < 0) {
            break;
         }
      }
   }
}
