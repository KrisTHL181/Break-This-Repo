package net.minecraft.gametest.framework;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TestBlock;
import net.minecraft.world.level.block.entity.TestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.TestBlockMode;

public class BlockBasedTestInstance extends GameTestInstance {
   public static final MapCodec<BlockBasedTestInstance> CODEC = RecordCodecBuilder.mapCodec(
      p_393515_ -> p_393515_.group(TestData.CODEC.forGetter(GameTestInstance::info)).apply(p_393515_, BlockBasedTestInstance::new)
   );

   public BlockBasedTestInstance(TestData<Holder<TestEnvironmentDefinition>> p_391955_) {
      super(p_391955_);
   }

   @Override
   public void run(GameTestHelper p_396526_) {
      BlockPos blockpos = this.findStartBlock(p_396526_);
      TestBlockEntity testblockentity = p_396526_.getBlockEntity(blockpos, TestBlockEntity.class);
      testblockentity.trigger();
      p_396526_.onEachTick(() -> {
         List<BlockPos> list = this.findTestBlocks(p_396526_, TestBlockMode.ACCEPT);
         if (list.isEmpty()) {
            p_396526_.fail(Component.translatable("test_block.error.missing", TestBlockMode.ACCEPT.getDisplayName()));
         }

         boolean flag = list.stream().map(p_397365_ -> p_396526_.getBlockEntity(p_397365_, TestBlockEntity.class)).anyMatch(TestBlockEntity::hasTriggered);
         if (flag) {
            p_396526_.succeed();
         } else {
            this.forAllTriggeredTestBlocks(p_396526_, TestBlockMode.FAIL, p_391295_ -> p_396526_.fail(Component.literal(p_391295_.getMessage())));
            this.forAllTriggeredTestBlocks(p_396526_, TestBlockMode.LOG, TestBlockEntity::trigger);
         }
      });
   }

   private void forAllTriggeredTestBlocks(GameTestHelper p_393740_, TestBlockMode p_393443_, Consumer<TestBlockEntity> p_391235_) {
      for (BlockPos blockpos : this.findTestBlocks(p_393740_, p_393443_)) {
         TestBlockEntity testblockentity = p_393740_.getBlockEntity(blockpos, TestBlockEntity.class);
         if (testblockentity.hasTriggered()) {
            p_391235_.accept(testblockentity);
            testblockentity.reset();
         }
      }
   }

   private BlockPos findStartBlock(GameTestHelper p_391948_) {
      List<BlockPos> list = this.findTestBlocks(p_391948_, TestBlockMode.START);
      if (list.isEmpty()) {
         p_391948_.fail(Component.translatable("test_block.error.missing", TestBlockMode.START.getDisplayName()));
      }

      if (list.size() != 1) {
         p_391948_.fail(Component.translatable("test_block.error.too_many", TestBlockMode.START.getDisplayName()));
      }

      return list.getFirst();
   }

   private List<BlockPos> findTestBlocks(GameTestHelper p_397003_, TestBlockMode p_394543_) {
      List<BlockPos> list = new ArrayList<>();
      p_397003_.forEveryBlockInStructure(p_395968_ -> {
         BlockState blockstate = p_397003_.getBlockState(p_395968_);
         if (blockstate.is(Blocks.TEST_BLOCK) && blockstate.getValue(TestBlock.MODE) == p_394543_) {
            list.add(p_395968_.immutable());
         }
      });
      return list;
   }

   @Override
   public MapCodec<BlockBasedTestInstance> codec() {
      return CODEC;
   }

   @Override
   protected MutableComponent typeDescription() {
      return Component.translatable("test_instance.type.block_based");
   }
}
