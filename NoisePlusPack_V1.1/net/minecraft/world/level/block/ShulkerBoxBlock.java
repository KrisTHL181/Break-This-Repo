package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class ShulkerBoxBlock extends BaseEntityBlock {
   public static final MapCodec<ShulkerBoxBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_422123_ -> p_422123_.group(DyeColor.CODEC.optionalFieldOf("color").forGetter(p_309293_ -> Optional.ofNullable(p_309293_.color)), propertiesCodec())
         .apply(p_422123_, (p_309290_, p_309291_) -> new ShulkerBoxBlock((DyeColor)p_309290_.orElse(null), p_309291_))
   );
   public static final Map<Direction, VoxelShape> SHAPES_OPEN_SUPPORT = Shapes.rotateAll(Block.boxZ(16.0, 0.0, 1.0));
   public static final EnumProperty<Direction> FACING = DirectionalBlock.FACING;
   public static final Identifier CONTENTS = Identifier.withDefaultNamespace("contents");
   private final @Nullable DyeColor color;

   @Override
   public MapCodec<ShulkerBoxBlock> codec() {
      return CODEC;
   }

   public ShulkerBoxBlock(@Nullable DyeColor p_56188_, BlockBehaviour.Properties p_56189_) {
      super(p_56189_);
      this.color = p_56188_;
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP));
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos p_154552_, BlockState p_154553_) {
      return new ShulkerBoxBlockEntity(this.color, p_154552_, p_154553_);
   }

   @Override
   public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level p_154543_, BlockState p_154544_, BlockEntityType<T> p_154545_) {
      return createTickerHelper(p_154545_, BlockEntityType.SHULKER_BOX, ShulkerBoxBlockEntity::tick);
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState p_56227_, Level p_56228_, BlockPos p_56229_, Player p_56230_, BlockHitResult p_56232_) {
      if (p_56228_ instanceof ServerLevel serverlevel
         && p_56228_.getBlockEntity(p_56229_) instanceof ShulkerBoxBlockEntity shulkerboxblockentity
         && canOpen(p_56227_, p_56228_, p_56229_, shulkerboxblockentity)) {
         p_56230_.openMenu(shulkerboxblockentity);
         p_56230_.awardStat(Stats.OPEN_SHULKER_BOX);
         PiglinAi.angerNearbyPiglins(serverlevel, p_56230_, true);
      }

      return InteractionResult.SUCCESS;
   }

   private static boolean canOpen(BlockState p_154547_, Level p_154548_, BlockPos p_154549_, ShulkerBoxBlockEntity p_154550_) {
      if (p_154550_.getAnimationStatus() != ShulkerBoxBlockEntity.AnimationStatus.CLOSED) {
         return true;
      }

      AABB aabb = Shulker.getProgressDeltaAabb(1.0F, p_154547_.getValue(FACING), 0.0F, 0.5F, p_154549_.getBottomCenter()).deflate(1.0E-6);
      return p_154548_.noCollision(aabb);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext p_56198_) {
      return this.defaultBlockState().setValue(FACING, p_56198_.getClickedFace());
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_56249_) {
      p_56249_.add(FACING);
   }

   @Override
   public BlockState playerWillDestroy(Level p_56212_, BlockPos p_56213_, BlockState p_56214_, Player p_56215_) {
      BlockEntity blockentity = p_56212_.getBlockEntity(p_56213_);
      if (blockentity instanceof ShulkerBoxBlockEntity shulkerboxblockentity) {
         if (!p_56212_.isClientSide() && p_56215_.preventsBlockDrops() && !shulkerboxblockentity.isEmpty()) {
            ItemStack itemstack = getColoredItemStack(this.getColor());
            itemstack.applyComponents(blockentity.collectComponents());
            ItemEntity itementity = new ItemEntity(p_56212_, p_56213_.getX() + 0.5, p_56213_.getY() + 0.5, p_56213_.getZ() + 0.5, itemstack);
            itementity.setDefaultPickUpDelay();
            p_56212_.addFreshEntity(itementity);
         } else {
            shulkerboxblockentity.unpackLootTable(p_56215_);
         }
      }

      return super.playerWillDestroy(p_56212_, p_56213_, p_56214_, p_56215_);
   }

   @Override
   protected List<ItemStack> getDrops(BlockState p_287632_, LootParams.Builder p_287691_) {
      BlockEntity blockentity = p_287691_.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
      if (blockentity instanceof ShulkerBoxBlockEntity shulkerboxblockentity) {
         p_287691_ = p_287691_.withDynamicDrop(CONTENTS, p_56219_ -> {
            for (int i = 0; i < shulkerboxblockentity.getContainerSize(); i++) {
               p_56219_.accept(shulkerboxblockentity.getItem(i));
            }
         });
      }

      return super.getDrops(p_287632_, p_287691_);
   }

   @Override
   protected void affectNeighborsAfterRemoval(BlockState p_393214_, ServerLevel p_391858_, BlockPos p_393972_, boolean p_396098_) {
      Containers.updateNeighboursAfterDestroy(p_393214_, p_391858_, p_393972_);
   }

   @Override
   protected VoxelShape getBlockSupportShape(BlockState p_259177_, BlockGetter p_260305_, BlockPos p_259168_) {
      return p_260305_.getBlockEntity(p_259168_) instanceof ShulkerBoxBlockEntity shulkerboxblockentity && !shulkerboxblockentity.isClosed()
         ? SHAPES_OPEN_SUPPORT.get(p_259177_.getValue(FACING).getOpposite())
         : Shapes.block();
   }

   @Override
   protected VoxelShape getShape(BlockState p_56257_, BlockGetter p_56258_, BlockPos p_56259_, CollisionContext p_56260_) {
      return p_56258_.getBlockEntity(p_56259_) instanceof ShulkerBoxBlockEntity shulkerboxblockentity
         ? Shapes.create(shulkerboxblockentity.getBoundingBox(p_56257_))
         : Shapes.block();
   }

   @Override
   protected boolean propagatesSkylightDown(BlockState p_330948_) {
      return false;
   }

   @Override
   protected boolean hasAnalogOutputSignal(BlockState p_56221_) {
      return true;
   }

   @Override
   protected int getAnalogOutputSignal(BlockState p_56223_, Level p_56224_, BlockPos p_56225_, Direction p_429270_) {
      return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(p_56224_.getBlockEntity(p_56225_));
   }

   public static Block getBlockByColor(@Nullable DyeColor p_56191_) {
      if (p_56191_ == null) {
         return Blocks.SHULKER_BOX;
      }

      return switch (p_56191_) {
         case WHITE -> Blocks.WHITE_SHULKER_BOX;
         case ORANGE -> Blocks.ORANGE_SHULKER_BOX;
         case MAGENTA -> Blocks.MAGENTA_SHULKER_BOX;
         case LIGHT_BLUE -> Blocks.LIGHT_BLUE_SHULKER_BOX;
         case YELLOW -> Blocks.YELLOW_SHULKER_BOX;
         case LIME -> Blocks.LIME_SHULKER_BOX;
         case PINK -> Blocks.PINK_SHULKER_BOX;
         case GRAY -> Blocks.GRAY_SHULKER_BOX;
         case LIGHT_GRAY -> Blocks.LIGHT_GRAY_SHULKER_BOX;
         case CYAN -> Blocks.CYAN_SHULKER_BOX;
         case BLUE -> Blocks.BLUE_SHULKER_BOX;
         case BROWN -> Blocks.BROWN_SHULKER_BOX;
         case GREEN -> Blocks.GREEN_SHULKER_BOX;
         case RED -> Blocks.RED_SHULKER_BOX;
         case BLACK -> Blocks.BLACK_SHULKER_BOX;
         case PURPLE -> Blocks.PURPLE_SHULKER_BOX;
      };
   }

   public @Nullable DyeColor getColor() {
      return this.color;
   }

   public static ItemStack getColoredItemStack(@Nullable DyeColor p_56251_) {
      return new ItemStack(getBlockByColor(p_56251_));
   }

   @Override
   protected BlockState rotate(BlockState p_56243_, Rotation p_56244_) {
      return p_56243_.setValue(FACING, p_56244_.rotate(p_56243_.getValue(FACING)));
   }

   @Override
   protected BlockState mirror(BlockState p_56240_, Mirror p_56241_) {
      return p_56240_.rotate(p_56241_.getRotation(p_56240_.getValue(FACING)));
   }
}
