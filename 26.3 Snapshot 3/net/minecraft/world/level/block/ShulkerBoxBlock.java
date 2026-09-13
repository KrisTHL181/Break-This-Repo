package net.minecraft.world.level.block;

import java.util.List;
import java.util.Map;
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
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class ShulkerBoxBlock extends BaseEntityBlock {
   public static final Map<Direction, VoxelShape> SHAPES_OPEN_SUPPORT = Shapes.rotateAll(Block.boxZ(16.0, 0.0, 1.0));
   public static final EnumProperty<Direction> FACING = DirectionalBlock.FACING;
   public static final Identifier CONTENTS = Identifier.withDefaultNamespace("contents");
   private final @Nullable DyeColor color;

   public ShulkerBoxBlock(final @Nullable DyeColor color, final BlockBehaviour.Properties properties) {
      super(properties);
      this.color = color;
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP));
   }

   @Override
   public BlockEntity newBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
      return new ShulkerBoxBlockEntity(this.color, worldPosition, blockState);
   }

   @Override
   public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(final Level level, final BlockState blockState, final BlockEntityType<T> type) {
      return createTickerHelper(type, BlockEntityTypes.SHULKER_BOX, ShulkerBoxBlockEntity::tick);
   }

   @Override
   protected InteractionResult useWithoutItem(
      final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult
   ) {
      if (level instanceof ServerLevel serverLevel
         && level.getBlockEntity(pos) instanceof ShulkerBoxBlockEntity shulkerBoxBlockEntity
         && canOpen(state, level, pos, shulkerBoxBlockEntity)) {
         player.openMenu(shulkerBoxBlockEntity);
         player.awardStat(Stats.OPEN_SHULKER_BOX);
         PiglinAi.angerNearbyPiglins(serverLevel, player, true);
      }

      return InteractionResult.SUCCESS;
   }

   private static boolean canOpen(final BlockState state, final Level level, final BlockPos pos, final ShulkerBoxBlockEntity blockEntity) {
      if (blockEntity.getAnimationStatus() != ShulkerBoxBlockEntity.AnimationStatus.CLOSED) {
         return true;
      }

      AABB lidOpenBoundingBox = Shulker.getProgressDeltaAabb(1.0F, state.getValue(FACING), 0.0F, 0.5F, Vec3.atBottomCenterOf(pos)).deflate(1.0E-6);
      return level.noCollision(lidOpenBoundingBox);
   }

   @Override
   public BlockState getStateForPlacement(final BlockPlaceContext context) {
      return this.defaultBlockState().setValue(FACING, context.getClickedFace());
   }

   @Override
   protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(FACING);
   }

   @Override
   public BlockState playerWillDestroy(final Level level, final BlockPos pos, final BlockState state, final Player player) {
      BlockEntity blockEntity = level.getBlockEntity(pos);
      if (blockEntity instanceof ShulkerBoxBlockEntity shulkerBoxBlockEntity) {
         if (!level.isClientSide() && player.preventsBlockDrops() && !shulkerBoxBlockEntity.isEmpty()) {
            ItemStack itemStack = new ItemStack(state.getBlock());
            itemStack.applyComponents(blockEntity.collectComponents());
            ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, itemStack);
            entity.setDefaultPickUpDelay();
            level.addFreshEntity(entity);
         } else {
            shulkerBoxBlockEntity.unpackLootTable(player);
         }
      }

      return super.playerWillDestroy(level, pos, state, player);
   }

   @Override
   protected List<ItemStack> getDrops(final BlockState state, LootParams.Builder params) {
      BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
      if (blockEntity instanceof ShulkerBoxBlockEntity shulkerBoxBlockEntity) {
         params = params.withDynamicDrop(CONTENTS, output -> {
            for (int i = 0; i < shulkerBoxBlockEntity.getContainerSize(); i++) {
               output.accept(shulkerBoxBlockEntity.getItem(i));
            }
         });
      }

      return super.getDrops(state, params);
   }

   @Override
   protected void affectNeighborsAfterRemoval(final BlockState state, final ServerLevel level, final BlockPos pos, final boolean movedByPiston) {
      Containers.updateNeighboursAfterDestroy(state, level, pos);
   }

   @Override
   protected VoxelShape getBlockSupportShape(final BlockState state, final BlockGetter level, final BlockPos pos) {
      return level.getBlockEntity(pos) instanceof ShulkerBoxBlockEntity shulker && !shulker.isClosed()
         ? SHAPES_OPEN_SUPPORT.get(state.getValue(FACING).getOpposite())
         : Shapes.block();
   }

   @Override
   protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      return level.getBlockEntity(pos) instanceof ShulkerBoxBlockEntity shulkerBoxBlockEntity
         ? Shapes.create(shulkerBoxBlockEntity.getBoundingBox(state))
         : Shapes.block();
   }

   @Override
   protected boolean propagatesSkylightDown(final BlockState state) {
      return false;
   }

   @Override
   protected boolean hasAnalogOutputSignal(final BlockState state) {
      return true;
   }

   @Override
   protected int getAnalogOutputSignal(final BlockState state, final Level level, final BlockPos pos, final Direction direction) {
      return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
   }

   public @Nullable DyeColor getColor() {
      return this.color;
   }

   @Override
   protected BlockState rotate(final BlockState state, final Rotation rotation) {
      return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
   }

   @Override
   protected BlockState mirror(final BlockState state, final Mirror mirror) {
      return state.rotate(mirror.getRotation(state.getValue(FACING)));
   }
}
