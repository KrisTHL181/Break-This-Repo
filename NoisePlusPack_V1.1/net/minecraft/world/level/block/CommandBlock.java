package net.minecraft.world.level.block;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class CommandBlock extends BaseEntityBlock implements GameMasterBlock {
   public static final MapCodec<CommandBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_422098_ -> p_422098_.group(Codec.BOOL.fieldOf("automatic").forGetter(p_311238_ -> p_311238_.automatic), propertiesCodec())
         .apply(p_422098_, CommandBlock::new)
   );
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final EnumProperty<Direction> FACING = DirectionalBlock.FACING;
   public static final BooleanProperty CONDITIONAL = BlockStateProperties.CONDITIONAL;
   private final boolean automatic;

   @Override
   public MapCodec<CommandBlock> codec() {
      return CODEC;
   }

   public CommandBlock(boolean p_153081_, BlockBehaviour.Properties p_153080_) {
      super(p_153080_);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CONDITIONAL, false));
      this.automatic = p_153081_;
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos p_153083_, BlockState p_153084_) {
      CommandBlockEntity commandblockentity = new CommandBlockEntity(p_153083_, p_153084_);
      commandblockentity.setAutomatic(this.automatic);
      return commandblockentity;
   }

   @Override
   protected void neighborChanged(BlockState p_51838_, Level p_51839_, BlockPos p_51840_, Block p_51841_, @Nullable Orientation p_361911_, boolean p_51843_) {
      if (!p_51839_.isClientSide() && p_51839_.getBlockEntity(p_51840_) instanceof CommandBlockEntity commandblockentity) {
         this.setPoweredAndUpdate(p_51839_, p_51840_, commandblockentity, p_51839_.hasNeighborSignal(p_51840_));
      }
   }

   private void setPoweredAndUpdate(Level p_365801_, BlockPos p_368914_, CommandBlockEntity p_368404_, boolean p_362448_) {
      boolean flag = p_368404_.isPowered();
      if (p_362448_ != flag) {
         p_368404_.setPowered(p_362448_);
         if (p_362448_) {
            if (p_368404_.isAutomatic() || p_368404_.getMode() == CommandBlockEntity.Mode.SEQUENCE) {
               return;
            }

            p_368404_.markConditionMet();
            p_365801_.scheduleTick(p_368914_, this, 1);
         }
      }
   }

   @Override
   protected void tick(BlockState p_221005_, ServerLevel p_221006_, BlockPos p_221007_, RandomSource p_221008_) {
      if (p_221006_.getBlockEntity(p_221007_) instanceof CommandBlockEntity commandblockentity) {
         BaseCommandBlock basecommandblock = commandblockentity.getCommandBlock();
         boolean flag = !StringUtil.isNullOrEmpty(basecommandblock.getCommand());
         CommandBlockEntity.Mode commandblockentity$mode = commandblockentity.getMode();
         boolean flag1 = commandblockentity.wasConditionMet();
         if (commandblockentity$mode == CommandBlockEntity.Mode.AUTO) {
            commandblockentity.markConditionMet();
            if (flag1) {
               this.execute(p_221005_, p_221006_, p_221007_, basecommandblock, flag);
            } else if (commandblockentity.isConditional()) {
               basecommandblock.setSuccessCount(0);
            }

            if (commandblockentity.isPowered() || commandblockentity.isAutomatic()) {
               p_221006_.scheduleTick(p_221007_, this, 1);
            }
         } else if (commandblockentity$mode == CommandBlockEntity.Mode.REDSTONE) {
            if (flag1) {
               this.execute(p_221005_, p_221006_, p_221007_, basecommandblock, flag);
            } else if (commandblockentity.isConditional()) {
               basecommandblock.setSuccessCount(0);
            }
         }

         p_221006_.updateNeighbourForOutputSignal(p_221007_, this);
      }
   }

   private void execute(BlockState p_51832_, ServerLevel p_369285_, BlockPos p_51834_, BaseCommandBlock p_51835_, boolean p_51836_) {
      if (p_51836_) {
         p_51835_.performCommand(p_369285_);
      } else {
         p_51835_.setSuccessCount(0);
      }

      executeChain(p_369285_, p_51834_, p_51832_.getValue(FACING));
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState p_51825_, Level p_51826_, BlockPos p_51827_, Player p_51828_, BlockHitResult p_51830_) {
      BlockEntity blockentity = p_51826_.getBlockEntity(p_51827_);
      if (blockentity instanceof CommandBlockEntity && p_51828_.canUseGameMasterBlocks()) {
         p_51828_.openCommandBlock((CommandBlockEntity)blockentity);
         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }

   @Override
   protected boolean hasAnalogOutputSignal(BlockState p_51814_) {
      return true;
   }

   @Override
   protected int getAnalogOutputSignal(BlockState p_51821_, Level p_51822_, BlockPos p_51823_, Direction p_426756_) {
      BlockEntity blockentity = p_51822_.getBlockEntity(p_51823_);
      return blockentity instanceof CommandBlockEntity ? ((CommandBlockEntity)blockentity).getCommandBlock().getSuccessCount() : 0;
   }

   @Override
   public void setPlacedBy(Level p_51804_, BlockPos p_51805_, BlockState p_51806_, @Nullable LivingEntity p_51807_, ItemStack p_51808_) {
      if (p_51804_.getBlockEntity(p_51805_) instanceof CommandBlockEntity commandblockentity) {
         BaseCommandBlock $$8 = commandblockentity.getCommandBlock();
         if (p_51804_ instanceof ServerLevel serverlevel) {
            if (!p_51808_.has(DataComponents.BLOCK_ENTITY_DATA)) {
               $$8.setTrackOutput(serverlevel.getGameRules().get(GameRules.SEND_COMMAND_FEEDBACK));
               commandblockentity.setAutomatic(this.automatic);
            }

            boolean flag = p_51804_.hasNeighborSignal(p_51805_);
            this.setPoweredAndUpdate(p_51804_, p_51805_, commandblockentity, flag);
         }
      }
   }

   @Override
   protected BlockState rotate(BlockState p_51848_, Rotation p_51849_) {
      return p_51848_.setValue(FACING, p_51849_.rotate(p_51848_.getValue(FACING)));
   }

   @Override
   protected BlockState mirror(BlockState p_51845_, Mirror p_51846_) {
      return p_51845_.rotate(p_51846_.getRotation(p_51845_.getValue(FACING)));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_51851_) {
      p_51851_.add(FACING, CONDITIONAL);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_51800_) {
      return this.defaultBlockState().setValue(FACING, p_51800_.getNearestLookingDirection().getOpposite());
   }

   private static void executeChain(ServerLevel p_365531_, BlockPos p_51811_, Direction p_51812_) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = p_51811_.mutable();
      GameRules gamerules = p_365531_.getGameRules();
      int i = gamerules.get(GameRules.MAX_COMMAND_SEQUENCE_LENGTH);

      while (i-- > 0) {
         blockpos$mutableblockpos.move(p_51812_);
         BlockState blockstate = p_365531_.getBlockState(blockpos$mutableblockpos);
         Block block = blockstate.getBlock();
         if (!blockstate.is(Blocks.CHAIN_COMMAND_BLOCK)
            || !(p_365531_.getBlockEntity(blockpos$mutableblockpos) instanceof CommandBlockEntity commandblockentity)
            || commandblockentity.getMode() != CommandBlockEntity.Mode.SEQUENCE) {
            break;
         }

         if (commandblockentity.isPowered() || commandblockentity.isAutomatic()) {
            BaseCommandBlock basecommandblock = commandblockentity.getCommandBlock();
            if (commandblockentity.markConditionMet()) {
               if (!basecommandblock.performCommand(p_365531_)) {
                  break;
               }

               p_365531_.updateNeighbourForOutputSignal(blockpos$mutableblockpos, block);
            } else if (commandblockentity.isConditional()) {
               basecommandblock.setSuccessCount(0);
            }
         }

         p_51812_ = blockstate.getValue(FACING);
      }

      if (i <= 0) {
         int j = Math.max(gamerules.get(GameRules.MAX_COMMAND_SEQUENCE_LENGTH), 0);
         LOGGER.warn("Command Block chain tried to execute more than {} steps!", j);
      }
   }
}
