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
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public abstract class TreeDecorator {
   public static final Codec<TreeDecorator> CODEC = BuiltInRegistries.TREE_DECORATOR_TYPE.byNameCodec().dispatch(TreeDecorator::type, TreeDecoratorType::codec);

   protected abstract TreeDecoratorType<?> type();

   public abstract void place(TreeDecorator.Context var1);

   public static final class Context {
      private final LevelSimulatedReader level;
      private final BiConsumer<BlockPos, BlockState> decorationSetter;
      private final RandomSource random;
      private final ObjectArrayList<BlockPos> logs;
      private final ObjectArrayList<BlockPos> leaves;
      private final ObjectArrayList<BlockPos> roots;

      public Context(
         LevelSimulatedReader p_226052_,
         BiConsumer<BlockPos, BlockState> p_226053_,
         RandomSource p_226054_,
         Set<BlockPos> p_226055_,
         Set<BlockPos> p_226056_,
         Set<BlockPos> p_226057_
      ) {
         this.level = p_226052_;
         this.decorationSetter = p_226053_;
         this.random = p_226054_;
         this.roots = new ObjectArrayList(p_226057_);
         this.logs = new ObjectArrayList(p_226055_);
         this.leaves = new ObjectArrayList(p_226056_);
         this.logs.sort(Comparator.comparingInt(Vec3i::getY));
         this.leaves.sort(Comparator.comparingInt(Vec3i::getY));
         this.roots.sort(Comparator.comparingInt(Vec3i::getY));
      }

      public void placeVine(BlockPos p_226065_, BooleanProperty p_226066_) {
         this.setBlock(p_226065_, Blocks.VINE.defaultBlockState().setValue(p_226066_, true));
      }

      public void setBlock(BlockPos p_226062_, BlockState p_226063_) {
         this.decorationSetter.accept(p_226062_, p_226063_);
      }

      public boolean isAir(BlockPos p_226060_) {
         return this.level.isStateAtPosition(p_226060_, BlockBehaviour.BlockStateBase::isAir);
      }

      public boolean checkBlock(BlockPos p_368437_, Predicate<BlockState> p_366855_) {
         return this.level.isStateAtPosition(p_368437_, p_366855_);
      }

      public LevelSimulatedReader level() {
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
