package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Comparator;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public abstract class TreeDecorator {
   public static final Codec<TreeDecorator> CODEC = BuiltInRegistries.TREE_DECORATOR_TYPE.byNameCodec().dispatch(TreeDecorator::type, TreeDecoratorType::codec);

   protected abstract TreeDecoratorType<?> type();

   public abstract void place(final TreeDecorator.Context context);

   public static final class Context {
      private final WorldGenLevel level;
      private final BiConsumer<BlockPos, BlockState> decorationSetter;
      private final RandomSource random;
      private final ObjectArrayList<BlockPos> logs;
      private final ObjectArrayList<BlockPos> leaves;
      private final ObjectArrayList<BlockPos> roots;

      public Context(
         final WorldGenLevel level,
         final BiConsumer<BlockPos, BlockState> decorationSetter,
         final RandomSource random,
         final Set<BlockPos> trunkSet,
         final Set<BlockPos> foliageSet,
         final Set<BlockPos> rootSet
      ) {
         this.level = level;
         this.decorationSetter = decorationSetter;
         this.random = random;
         this.roots = new ObjectArrayList(rootSet);
         this.logs = new ObjectArrayList(trunkSet);
         this.leaves = new ObjectArrayList(foliageSet);
         this.logs.sort(Comparator.comparingInt(Vec3i::getY));
         this.leaves.sort(Comparator.comparingInt(Vec3i::getY));
         this.roots.sort(Comparator.comparingInt(Vec3i::getY));
      }

      public void placeVine(final BlockPos pos, final BooleanProperty direction) {
         this.setBlock(pos, Blocks.VINE.defaultBlockState().setValue(direction, true));
      }

      public void setBlock(final BlockPos pos, final BlockState state) {
         this.decorationSetter.accept(pos, state);
      }

      public boolean isAir(final BlockPos pos) {
         return this.level.isStateAtPosition(pos, BlockBehaviour.BlockStateBase::isAir);
      }

      public boolean checkBlock(final BlockPos pos, final Predicate<BlockState> predicate) {
         return this.level.isStateAtPosition(pos, predicate);
      }

      public boolean isReplaceable(final BlockPos pos) {
         return this.checkBlock(pos, BlockBehaviour.BlockStateBase::canBeReplaced);
      }

      public boolean isWaterOrWaterNearby(final BlockPos pos) {
         return this.checkBlock(pos, state -> state.is(Blocks.WATER))
            || this.checkBlock(pos.east(), state -> state.is(Blocks.WATER))
            || this.checkBlock(pos.west(), state -> state.is(Blocks.WATER))
            || this.checkBlock(pos.north(), state -> state.is(Blocks.WATER))
            || this.checkBlock(pos.south(), state -> state.is(Blocks.WATER));
      }

      public WorldGenLevel level() {
         return this.level;
      }

      public RandomSource random() {
         return this.random;
      }

      public ObjectArrayList<BlockPos> logs() {
         return this.logs;
      }

      public ObjectArrayList<BlockPos> leaves() {
         return this.leaves;
      }

      public ObjectArrayList<BlockPos> roots() {
         return this.roots;
      }
   }
}
