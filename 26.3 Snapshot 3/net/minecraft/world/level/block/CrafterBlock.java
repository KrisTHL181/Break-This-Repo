package net.minecraft.world.level.block;

import java.util.Optional;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeCache;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class CrafterBlock extends BaseEntityBlock {
   public static final BooleanProperty CRAFTING = BlockStateProperties.CRAFTING;
   public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;
   private static final EnumProperty<FrontAndTop> ORIENTATION = BlockStateProperties.ORIENTATION;
   private static final int MAX_CRAFTING_TICKS = 6;
   private static final int CRAFTING_TICK_DELAY = 4;
   private static final RecipeCache RECIPE_CACHE = new RecipeCache(10);
   private static final int CRAFTER_ADVANCEMENT_DIAMETER = 17;

   public CrafterBlock(final BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState(this.stateDefinition.any().setValue(ORIENTATION, FrontAndTop.NORTH_UP).setValue(TRIGGERED, false).setValue(CRAFTING, false));
   }

   @Override
   protected boolean hasAnalogOutputSignal(final BlockState state) {
      return true;
   }

   @Override
   protected int getAnalogOutputSignal(final BlockState state, final Level level, final BlockPos pos, final Direction direction) {
      return level.getBlockEntity(pos) instanceof CrafterBlockEntity crafterBlockEntity ? crafterBlockEntity.getRedstoneSignal() : 0;
   }

   @Override
   protected void neighborChanged(
      final BlockState state, final Level level, final BlockPos pos, final Block block, final @Nullable Orientation orientation, final boolean movedByPiston
   ) {
      boolean shouldTrigger = level.hasNeighborSignal(pos);
      boolean isTriggered = state.getValue(TRIGGERED);
      BlockEntity blockEntity = level.getBlockEntity(pos);
      if (shouldTrigger && !isTriggered) {
         level.scheduleTick(pos, this, 4);
         level.setBlock(pos, state.setValue(TRIGGERED, true), 2);
         this.setBlockEntityTriggered(blockEntity, true);
      } else if (!shouldTrigger && isTriggered) {
         level.setBlock(pos, state.setValue(TRIGGERED, false).setValue(CRAFTING, false), 2);
         this.setBlockEntityTriggered(blockEntity, false);
      }
   }

   @Override
   protected void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
      this.dispenseFrom(state, level, pos);
   }

   @Override
   public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(final Level level, final BlockState blockState, final BlockEntityType<T> type) {
      return level.isClientSide() ? null : createTickerHelper(type, BlockEntityTypes.CRAFTER, CrafterBlockEntity::serverTick);
   }

   private void setBlockEntityTriggered(final @Nullable BlockEntity blockEntity, final boolean triggered) {
      if (blockEntity instanceof CrafterBlockEntity crafterBlockEntity) {
         crafterBlockEntity.setTriggered(triggered);
      }
   }

   @Override
   public BlockEntity newBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
      CrafterBlockEntity crafterBlockEntity = new CrafterBlockEntity(worldPosition, blockState);
      crafterBlockEntity.setTriggered(blockState.hasProperty(TRIGGERED) && blockState.getValue(TRIGGERED));
      return crafterBlockEntity;
   }

   @Override
   public BlockState getStateForPlacement(final BlockPlaceContext context) {
      Direction nearestLookingDirection = context.getNearestLookingDirection().getOpposite();

      Direction verticalDirection = switch (nearestLookingDirection) {
         case DOWN -> context.getHorizontalDirection().getOpposite();
         case UP -> context.getHorizontalDirection();
         case NORTH, SOUTH, WEST, EAST -> Direction.UP;
      };
      return this.defaultBlockState()
         .setValue(ORIENTATION, FrontAndTop.fromFrontAndTop(nearestLookingDirection, verticalDirection))
         .setValue(TRIGGERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
   }

   @Override
   public void setPlacedBy(final Level level, final BlockPos pos, final BlockState state, final @Nullable LivingEntity by, final ItemStack itemStack) {
      if (state.getValue(TRIGGERED)) {
         level.scheduleTick(pos, this, 4);
      }
   }

   @Override
   protected void affectNeighborsAfterRemoval(final BlockState state, final ServerLevel level, final BlockPos pos, final boolean movedByPiston) {
      Containers.updateNeighboursAfterDestroy(state, level, pos);
   }

   @Override
   protected InteractionResult useWithoutItem(
      final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult
   ) {
      if (!level.isClientSide() && level.getBlockEntity(pos) instanceof CrafterBlockEntity crafter) {
         player.openMenu(crafter);
      }

      return InteractionResult.SUCCESS;
   }

   protected void dispenseFrom(final BlockState state, final ServerLevel level, final BlockPos pos) {
      if (level.getBlockEntity(pos) instanceof CrafterBlockEntity blockEntity) {
         CraftingInput var11 = blockEntity.asCraftInput();
         Optional<RecipeHolder<CraftingRecipe>> recipe = getPotentialResults(level, var11);
         if (recipe.isEmpty()) {
            level.levelEvent(1050, pos, 0);
         } else {
            RecipeHolder<CraftingRecipe> pickedRecipe = recipe.get();
            ItemStack results = pickedRecipe.value().assemble(var11);
            if (results.isEmpty()) {
               level.levelEvent(1050, pos, 0);
            } else {
               blockEntity.setCraftingTicksRemaining(6);
               level.setBlock(pos, state.setValue(CRAFTING, true), 2);
               results.onCraftedBySystem(level);
               this.dispenseItem(level, pos, blockEntity, results, state, pickedRecipe);

               for (ItemStack remainingItem : pickedRecipe.value().getRemainingItems(var11)) {
                  if (!remainingItem.isEmpty()) {
                     this.dispenseItem(level, pos, blockEntity, remainingItem, state, pickedRecipe);
                  }
               }

               blockEntity.getItems().forEach(it -> {
                  if (!it.isEmpty()) {
                     it.shrink(1);
                  }
               });
               blockEntity.setChanged();
            }
         }
      }
   }

   public static Optional<RecipeHolder<CraftingRecipe>> getPotentialResults(final ServerLevel level, final CraftingInput input) {
      return RECIPE_CACHE.get(level, input);
   }

   private void dispenseItem(
      final ServerLevel level,
      final BlockPos pos,
      final CrafterBlockEntity blockEntity,
      final ItemStack results,
      final BlockState blockState,
      final RecipeHolder<?> recipe
   ) {
      Direction direction = blockState.getValue(ORIENTATION).front();
      Container into = HopperBlockEntity.getContainerAt(level, pos.relative(direction));
      ItemStack remaining = results.copy();
      if (into != null && (into instanceof CrafterBlockEntity || results.getCount() > into.getMaxStackSize(results))) {
         while (!remaining.isEmpty()) {
            ItemStack copy = remaining.copyWithCount(1);
            ItemStack itemStack = HopperBlockEntity.addItem(blockEntity, into, copy, direction.getOpposite());
            if (!itemStack.isEmpty()) {
               break;
            }

            remaining.shrink(1);
         }
      } else if (into != null) {
         while (!remaining.isEmpty()) {
            int oldSize = remaining.getCount();
            remaining = HopperBlockEntity.addItem(blockEntity, into, remaining, direction.getOpposite());
            if (oldSize == remaining.getCount()) {
               break;
            }
         }
      }

      if (!remaining.isEmpty()) {
         Vec3 centerPos = Vec3.atCenterOf(pos);
         Vec3 itemSpawnOffset = centerPos.relative(direction, 0.7);
         DefaultDispenseItemBehavior.spawnItem(level, remaining, 6, direction, itemSpawnOffset);

         for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, AABB.ofSize(centerPos, 17.0, 17.0, 17.0))) {
            CriteriaTriggers.CRAFTER_RECIPE_CRAFTED.trigger(player, recipe.id(), blockEntity.getItems());
         }

         level.levelEvent(1049, pos, 0);
         level.levelEvent(2010, pos, direction.get3DDataValue());
      }
   }

   @Override
   protected BlockState rotate(final BlockState state, final Rotation rotation) {
      return state.setValue(ORIENTATION, rotation.rotation().rotate(state.getValue(ORIENTATION)));
   }

   @Override
   protected BlockState mirror(final BlockState state, final Mirror mirror) {
      return state.setValue(ORIENTATION, mirror.rotation().rotate(state.getValue(ORIENTATION)));
   }

   @Override
   protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(ORIENTATION, TRIGGERED, CRAFTING);
   }
}
