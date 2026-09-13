package net.minecraft.world.level.block;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Compostable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class ComposterBlock extends Block implements WorldlyContainerHolder {
   public static final int READY = 8;
   public static final int MIN_LEVEL = 0;
   public static final int MAX_LEVEL = 7;
   public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL_COMPOSTER;
   private static final int HOLE_WIDTH = 12;
   private static final VoxelShape[] SHAPES = Util.make(
      () -> {
         VoxelShape[] shapes = Block.boxes(
            8, level -> Shapes.join(Shapes.block(), Block.column(12.0, Math.clamp(1 + level * 2, 2, 16), 16.0), BooleanOp.ONLY_FIRST)
         );
         shapes[8] = shapes[7];
         return shapes;
      }
   );

   public ComposterBlock(final BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState(this.stateDefinition.any().setValue(LEVEL, 0));
   }

   public static void handleFill(final Level level, final BlockPos pos, final boolean success) {
      BlockState state = level.getBlockState(pos);
      level.playLocalSound(pos, success ? SoundEvents.COMPOSTER_FILL_SUCCESS : SoundEvents.COMPOSTER_FILL, SoundSource.BLOCKS, 1.0F, 1.0F, false);
      double centerHeight = state.getShape(level, pos).max(Direction.Axis.Y, 0.5, 0.5) + 0.03125;
      double sideOffsetPixels = 2.0;
      double sideOffset = 0.1875;
      double width = 0.625;
      RandomSource random = level.getRandom();

      for (int i = 0; i < 10; i++) {
         double xa = random.nextGaussian() * 0.02;
         double ya = random.nextGaussian() * 0.02;
         double za = random.nextGaussian() * 0.02;
         level.addParticle(
            ParticleTypes.COMPOSTER,
            pos.getX() + 0.1875 + 0.625 * random.nextFloat(),
            pos.getY() + centerHeight + random.nextFloat() * (1.0 - centerHeight),
            pos.getZ() + 0.1875 + 0.625 * random.nextFloat(),
            xa,
            ya,
            za
         );
      }
   }

   @Override
   protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      return SHAPES[state.getValue(LEVEL)];
   }

   @Override
   protected VoxelShape getInteractionShape(final BlockState state, final BlockGetter level, final BlockPos pos) {
      return Shapes.block();
   }

   @Override
   protected VoxelShape getCollisionShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      return SHAPES[0];
   }

   @Override
   protected void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState oldState, final boolean movedByPiston) {
      if (state.getValue(LEVEL) == 7) {
         level.scheduleTick(pos, state.getBlock(), 20);
      }
   }

   @Override
   protected InteractionResult useItemOn(
      final ItemStack itemStack,
      final BlockState state,
      final Level level,
      final BlockPos pos,
      final Player player,
      final InteractionHand hand,
      final BlockHitResult hitResult
   ) {
      int fillLevel = state.getValue(LEVEL);
      Compostable compostable = itemStack.get(DataComponents.COMPOSTABLE);
      if (fillLevel < 8 && compostable != null) {
         if (fillLevel < 7 && level instanceof ServerLevel serverLevel) {
            BlockState newState = addLayer(player, state, serverLevel, pos, compostable);
            level.levelEvent(1500, pos, state != newState ? 1 : 0);
            player.awardStat(Stats.ITEM_USED.get(itemStack.getItem()));
            itemStack.consume(1, player);
         }

         return InteractionResult.SUCCESS;
      } else {
         return super.useItemOn(itemStack, state, level, pos, player, hand, hitResult);
      }
   }

   @Override
   protected InteractionResult useWithoutItem(
      final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult
   ) {
      int fillLevel = state.getValue(LEVEL);
      if (fillLevel == 8) {
         extractProduce(player, state, level, pos);
         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }

   public static BlockState insertItem(
      final Entity sourceEntity, final BlockState state, final ServerLevel level, final ItemStack itemStack, final BlockPos pos
   ) {
      int fillLevel = state.getValue(LEVEL);
      Compostable compostable = itemStack.get(DataComponents.COMPOSTABLE);
      if (fillLevel < 7 && compostable != null) {
         BlockState newState = addLayer(sourceEntity, state, level, pos, compostable);
         itemStack.shrink(1);
         return newState;
      } else {
         return state;
      }
   }

   public static BlockState extractProduce(final Entity sourceEntity, final BlockState state, final Level level, final BlockPos pos) {
      if (!level.isClientSide()) {
         Vec3 itemPos = Vec3.atLowerCornerWithOffset(pos, 0.5, 1.01, 0.5).offsetRandomXZ(level.getRandom(), 0.7F);
         ItemEntity entity = new ItemEntity(level, itemPos.x(), itemPos.y(), itemPos.z(), new ItemStack(Items.BONE_MEAL));
         entity.setDefaultPickUpDelay();
         level.addFreshEntity(entity);
      }

      BlockState emptyState = empty(sourceEntity, state, level, pos);
      level.playSound(null, pos, SoundEvents.COMPOSTER_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
      return emptyState;
   }

   private static BlockState empty(final @Nullable Entity sourceEntity, final BlockState state, final LevelAccessor level, final BlockPos pos) {
      BlockState newState = state.setValue(LEVEL, 0);
      level.setBlockAndUpdate(pos, newState);
      level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(sourceEntity, newState));
      return newState;
   }

   private static BlockState addLayer(
      final @Nullable Entity sourceEntity, final BlockState state, final ServerLevel level, final BlockPos pos, final Compostable compostable
   ) {
      int fillLevel = state.getValue(LEVEL);
      LootContext lootContext = new LootContext.Builder(
            new LootParams.Builder(level)
               .withParameter(LootContextParams.BLOCK_STATE, state)
               .withOptionalParameter(LootContextParams.INTERACTING_ENTITY, sourceEntity)
               .create(LootContextParamSets.BLOCK_INTERACT)
         )
         .create(Optional.empty());
      Optional<NumberProvider> layers = level.getServer()
         .reloadableRegistries()
         .lookup()
         .lookup(Registries.NUMBER_PROVIDER)
         .flatMap(registry -> registry.get(compostable.layers()))
         .map(Holder.Reference::value);
      if (layers.isEmpty()) {
         return state;
      }

      int layersToAdd = layers.get().getInt(lootContext);
      if (layersToAdd > 0) {
         int newLevel = Mth.clamp(fillLevel + layersToAdd, 0, 7);
         BlockState newState = state.setValue(LEVEL, newLevel);
         level.setBlockAndUpdate(pos, newState);
         level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(sourceEntity, newState));
         if (newLevel == 7) {
            level.scheduleTick(pos, state.getBlock(), 20);
         }

         return newState;
      } else {
         return state;
      }
   }

   @Override
   protected void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
      if (state.getValue(LEVEL) == 7) {
         level.setBlockAndUpdate(pos, state.cycle(LEVEL));
         level.playSound(null, pos, SoundEvents.COMPOSTER_READY, SoundSource.BLOCKS, 1.0F, 1.0F);
      }
   }

   @Override
   protected boolean hasAnalogOutputSignal(final BlockState state) {
      return true;
   }

   @Override
   protected int getAnalogOutputSignal(final BlockState state, final Level level, final BlockPos pos, final Direction direction) {
      return state.getValue(LEVEL);
   }

   @Override
   protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(LEVEL);
   }

   @Override
   protected boolean isPathfindable(final BlockState state, final PathComputationType type) {
      return false;
   }

   @Override
   public WorldlyContainer getContainer(final BlockState state, final LevelAccessor level, final BlockPos pos) {
      int contentLevel = state.getValue(LEVEL);
      if (contentLevel == 8) {
         return new ComposterBlock.OutputContainer(state, level, pos, new ItemStack(Items.BONE_MEAL));
      } else {
         return contentLevel < 7 ? new ComposterBlock.InputContainer(state, level, pos) : new ComposterBlock.EmptyContainer();
      }
   }

   private static class EmptyContainer extends SimpleContainer implements WorldlyContainer {
      public EmptyContainer() {
         super(0);
      }

      @Override
      public int[] getSlotsForFace(final Direction direction) {
         return new int[0];
      }

      @Override
      public boolean canPlaceItemThroughFace(final int slot, final ItemStack itemStack, final @Nullable Direction direction) {
         return false;
      }

      @Override
      public boolean canTakeItemThroughFace(final int slot, final ItemStack itemStack, final Direction direction) {
         return false;
      }
   }

   private static class InputContainer extends SimpleContainer implements WorldlyContainer {
      private final BlockState state;
      private final LevelAccessor level;
      private final BlockPos pos;
      private boolean changed;

      public InputContainer(final BlockState state, final LevelAccessor level, final BlockPos pos) {
         super(1);
         this.state = state;
         this.level = level;
         this.pos = pos;
      }

      @Override
      public int getMaxStackSize() {
         return 1;
      }

      @Override
      public int[] getSlotsForFace(final Direction direction) {
         return direction == Direction.UP ? new int[]{0} : new int[0];
      }

      @Override
      public boolean canPlaceItemThroughFace(final int slot, final ItemStack itemStack, final @Nullable Direction direction) {
         return !this.changed && direction == Direction.UP && itemStack.has(DataComponents.COMPOSTABLE);
      }

      @Override
      public boolean canTakeItemThroughFace(final int slot, final ItemStack itemStack, final Direction direction) {
         return false;
      }

      @Override
      public void setChanged() {
         ItemStack contents = this.getItem(0);
         Compostable compostable = contents.get(DataComponents.COMPOSTABLE);
         if (!contents.isEmpty() && compostable != null && this.level instanceof ServerLevel serverLevel) {
            this.changed = true;
            BlockState newState = ComposterBlock.addLayer(null, this.state, serverLevel, this.pos, compostable);
            this.level.levelEvent(1500, this.pos, newState != this.state ? 1 : 0);
            this.removeItemNoUpdate(0);
         }
      }
   }

   private static class OutputContainer extends SimpleContainer implements WorldlyContainer {
      private final BlockState state;
      private final LevelAccessor level;
      private final BlockPos pos;
      private boolean changed;

      public OutputContainer(final BlockState state, final LevelAccessor level, final BlockPos pos, final ItemStack contents) {
         super(contents);
         this.state = state;
         this.level = level;
         this.pos = pos;
      }

      @Override
      public int getMaxStackSize() {
         return 1;
      }

      @Override
      public int[] getSlotsForFace(final Direction direction) {
         return direction == Direction.DOWN ? new int[]{0} : new int[0];
      }

      @Override
      public boolean canPlaceItemThroughFace(final int slot, final ItemStack itemStack, final @Nullable Direction direction) {
         return false;
      }

      @Override
      public boolean canTakeItemThroughFace(final int slot, final ItemStack itemStack, final Direction direction) {
         return !this.changed && direction == Direction.DOWN && itemStack.is(Items.BONE_MEAL);
      }

      @Override
      public void setChanged() {
         ComposterBlock.empty(null, this.state, this.level, this.pos);
         this.changed = true;
      }
   }
}
