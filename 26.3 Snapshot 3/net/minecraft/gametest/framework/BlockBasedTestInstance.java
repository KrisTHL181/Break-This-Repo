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
      i -> i.group(TestData.CODEC.forGetter(GameTestInstance::info)).apply(i, BlockBasedTestInstance::new)
   );

   public BlockBasedTestInstance(final TestData<Holder<TestEnvironmentDefinition<?>>> testData) {
      super(testData);
   }

   @Override
   public void run(final GameTestHelper helper) {
      BlockPos startPos = this.findStartBlock(helper);
      TestBlockEntity blockEntity = helper.getBlockEntity(startPos, TestBlockEntity.class);
      blockEntity.trigger();
      helper.onEachTick(() -> {
         List<BlockPos> acceptBlocks = this.findTestBlocks(helper, TestBlockMode.ACCEPT);
         if (acceptBlocks.isEmpty()) {
            helper.fail(Component.translatable("test_block.error.missing", TestBlockMode.ACCEPT.getDisplayName()));
         }

         boolean acceptTriggered = acceptBlocks.stream().map(pos -> helper.getBlockEntity(pos, TestBlockEntity.class)).anyMatch(TestBlockEntity::hasTriggered);
         if (acceptTriggered) {
            helper.succeed();
         } else {
            this.forAllTriggeredTestBlocks(helper, TestBlockMode.FAIL, failEntity -> helper.fail(Component.literal(failEntity.getMessage())));
            this.forAllTriggeredTestBlocks(helper, TestBlockMode.LOG, TestBlockEntity::trigger);
         }
      });
   }

   private void forAllTriggeredTestBlocks(final GameTestHelper helper, final TestBlockMode mode, final Consumer<TestBlockEntity> action) {
      for (BlockPos failBlock : this.findTestBlocks(helper, mode)) {
         TestBlockEntity blockEntity = helper.getBlockEntity(failBlock, TestBlockEntity.class);
         if (blockEntity.hasTriggered()) {
            action.accept(blockEntity);
            blockEntity.reset();
         }
      }
   }

   private BlockPos findStartBlock(final GameTestHelper helper) {
      List<BlockPos> testBlocks = this.findTestBlocks(helper, TestBlockMode.START);
      if (testBlocks.isEmpty()) {
         helper.fail(Component.translatable("test_block.error.missing", TestBlockMode.START.getDisplayName()));
      }

      if (testBlocks.size() != 1) {
         helper.fail(Component.translatable("test_block.error.too_many", TestBlockMode.START.getDisplayName()));
      }

      return testBlocks.getFirst();
   }

   private List<BlockPos> findTestBlocks(final GameTestHelper helper, final TestBlockMode mode) {
      List<BlockPos> blocks = new ArrayList<>();
      helper.forEveryBlockInStructure(pos -> {
         BlockState state = helper.getBlockState(pos);
         if (state.is(Blocks.TEST_BLOCK) && state.getValue(TestBlock.MODE) == mode) {
            blocks.add(pos.immutable());
         }
      });
      return blocks;
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
