package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class StructureBlock extends BaseEntityBlock implements GameMasterBlock {
   public static final EnumProperty<StructureMode> MODE = BlockStateProperties.STRUCTUREBLOCK_MODE;

   protected StructureBlock(final BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState(this.stateDefinition.any().setValue(MODE, StructureMode.LOAD));
   }

   @Override
   public BlockEntity newBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
      return new StructureBlockEntity(worldPosition, blockState);
   }

   @Override
   protected InteractionResult useWithoutItem(
      final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult
   ) {
      if (level.getBlockEntity(pos) instanceof StructureBlockEntity structureBlockEntity) {
         return structureBlockEntity.usedBy(player) ? InteractionResult.SUCCESS : InteractionResult.PASS;
      } else {
         return InteractionResult.PASS;
      }
   }

   @Override
   public void setPlacedBy(final Level level, final BlockPos pos, final BlockState state, final @Nullable LivingEntity by, final ItemStack itemStack) {
      if (!level.isClientSide()) {
         if (by != null && level.getBlockEntity(pos) instanceof StructureBlockEntity structureBlockEntity) {
            structureBlockEntity.createdBy(by);
         }
      }
   }

   @Override
   protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(MODE);
   }

   @Override
   protected void neighborChanged(
      final BlockState state, final Level level, final BlockPos pos, final Block block, final @Nullable Orientation orientation, final boolean movedByPiston
   ) {
      if (level instanceof ServerLevel serverLevel) {
         if (level.getBlockEntity(pos) instanceof StructureBlockEntity structureBlock) {
            boolean shouldTrigger = level.hasNeighborSignal(pos);
            boolean isPowered = structureBlock.isPowered();
            if (shouldTrigger && !isPowered) {
               structureBlock.setPowered(true);
               this.trigger(serverLevel, structureBlock);
            } else if (!shouldTrigger && isPowered) {
               structureBlock.setPowered(false);
            }
         }
      }
   }

   private void trigger(final ServerLevel level, final StructureBlockEntity structureBlock) {
      switch (structureBlock.getMode()) {
         case SAVE:
            structureBlock.saveStructure(false);
            break;
         case LOAD:
            structureBlock.placeStructure(level);
            break;
         case CORNER:
            structureBlock.unloadStructure();
         case DATA:
      }
   }
}
