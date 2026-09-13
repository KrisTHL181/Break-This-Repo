package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class CampfireBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
   public static final BooleanProperty LIT = BlockStateProperties.LIT;
   public static final BooleanProperty SIGNAL_FIRE = BlockStateProperties.SIGNAL_FIRE;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
   private static final VoxelShape SHAPE = Block.column(16.0, 0.0, 7.0);
   private static final VoxelShape SHAPE_VIRTUAL_POST = Block.column(4.0, 0.0, 16.0);
   private static final int SMOKE_DISTANCE = 5;
   private final boolean spawnParticles;
   private final int fireDamage;

   public CampfireBlock(final boolean spawnParticles, final int fireDamage, final BlockBehaviour.Properties properties) {
      super(properties);
      this.spawnParticles = spawnParticles;
      this.fireDamage = fireDamage;
      this.registerDefaultState(
         this.stateDefinition.any().setValue(LIT, true).setValue(SIGNAL_FIRE, false).setValue(WATERLOGGED, false).setValue(FACING, Direction.NORTH)
      );
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
      if (level.getBlockEntity(pos) instanceof CampfireBlockEntity campfire) {
         ItemStack itemInHand = player.getItemInHand(hand);
         if (level.recipeAccess().propertySet(RecipePropertySet.CAMPFIRE_INPUT).test(itemInHand)) {
            if (level instanceof ServerLevel serverLevel && campfire.placeFood(serverLevel, player, itemInHand)) {
               player.awardStat(Stats.INTERACT_WITH_CAMPFIRE);
               return InteractionResult.SUCCESS_SERVER;
            }

            return InteractionResult.CONSUME;
         }
      }

      if (itemStack.is(ItemTags.DOUSES_CAMPFIRES) && state.getValue(LIT)) {
         if (!level.isClientSide()) {
            level.levelEvent(null, 1009, pos, 0);
         }

         douse(player, level, pos, state);
         level.setBlockAndUpdate(pos, state.setValue(LIT, false));
         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.TRY_WITH_EMPTY_HAND;
      }
   }

   @Override
   protected void entityInside(
      final BlockState state, final Level level, final BlockPos pos, final Entity entity, final InsideBlockEffectApplier effectApplier, final boolean isPrecise
   ) {
      if (state.getValue(LIT) && entity instanceof LivingEntity) {
         entity.hurt(level.damageSources().campfire(), this.fireDamage);
      }

      super.entityInside(state, level, pos, entity, effectApplier, isPrecise);
   }

   @Override
   public @Nullable BlockState getStateForPlacement(final BlockPlaceContext context) {
      LevelAccessor level = context.getLevel();
      BlockPos pos = context.getClickedPos();
      boolean replacedWater = level.getFluidState(pos).is(Fluids.WATER);
      return this.defaultBlockState()
         .setValue(WATERLOGGED, replacedWater)
         .setValue(SIGNAL_FIRE, this.isSmokeSource(level.getBlockState(pos.below())))
         .setValue(LIT, !replacedWater)
         .setValue(FACING, context.getHorizontalDirection());
   }

   @Override
   protected BlockState updateShape(
      final BlockState state,
      final LevelReader level,
      final ScheduledTickAccess ticks,
      final BlockPos pos,
      final Direction directionToNeighbour,
      final BlockPos neighbourPos,
      final BlockState neighbourState,
      final RandomSource random
   ) {
      if (state.getValue(WATERLOGGED)) {
         ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
      }

      return directionToNeighbour == Direction.DOWN
         ? state.setValue(SIGNAL_FIRE, this.isSmokeSource(neighbourState))
         : super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
   }

   private boolean isSmokeSource(final BlockState blockState) {
      return blockState.is(Blocks.HAY_BLOCK);
   }

   @Override
   protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      return SHAPE;
   }

   @Override
   public void animateTick(final BlockState state, final Level level, final BlockPos pos, final RandomSource random) {
      if (state.getValue(LIT)) {
         if (random.nextInt(10) == 0) {
            level.playLocalSound(
               pos.getX() + 0.5,
               pos.getY() + 0.5,
               pos.getZ() + 0.5,
               SoundEvents.CAMPFIRE_CRACKLE,
               SoundSource.BLOCKS,
               0.5F + random.nextFloat(),
               random.nextFloat() * 0.7F + 0.6F,
               false
            );
         }

         if (this.spawnParticles && random.nextInt(5) == 0) {
            for (int i = 0; i < random.nextInt(1) + 1; i++) {
               level.addParticle(
                  ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, random.nextFloat() / 2.0F, 5.0E-5, random.nextFloat() / 2.0F
               );
            }
         }
      }
   }

   public static void douse(final @Nullable Entity source, final LevelAccessor level, final BlockPos pos, final BlockState state) {
      if (level.isClientSide()) {
         for (int j = 0; j < 20; j++) {
            makeParticles((Level)level, pos, state.getValue(SIGNAL_FIRE), true);
         }
      }

      level.gameEvent(source, GameEvent.BLOCK_CHANGE, pos);
   }

   @Override
   public boolean placeLiquid(final LevelAccessor level, final BlockPos pos, final BlockState state, final FluidState fluidState) {
      if (!state.getValue(BlockStateProperties.WATERLOGGED) && fluidState.is(Fluids.WATER)) {
         boolean isLit = state.getValue(LIT);
         if (isLit) {
            if (!level.isClientSide()) {
               level.playSound(null, pos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 1.0F, 1.0F);
            }

            douse(null, level, pos, state);
         }

         level.setBlockAndUpdate(pos, state.setValue(WATERLOGGED, true).setValue(LIT, false));
         level.scheduleTick(pos, fluidState.getType(), fluidState.getType().getTickDelay(level));
         return true;
      } else {
         return false;
      }
   }

   @Override
   protected void onProjectileHit(final Level level, final BlockState state, final BlockHitResult blockHit, final Projectile projectile) {
      BlockPos pos = blockHit.getBlockPos();
      if (level instanceof ServerLevel serverLevel
         && projectile.isOnFire()
         && projectile.mayInteract(serverLevel, pos)
         && !state.getValue(LIT)
         && !state.getValue(WATERLOGGED)) {
         level.setBlock(pos, state.setValue(BlockStateProperties.LIT, true), 11);
      }
   }

   public static void makeParticles(final Level level, final BlockPos pos, final boolean isSignalFire, final boolean smoking) {
      RandomSource random = level.getRandom();
      SimpleParticleType smokeParticle = isSignalFire ? ParticleTypes.CAMPFIRE_SIGNAL_SMOKE : ParticleTypes.CAMPFIRE_COSY_SMOKE;
      level.addAlwaysVisibleParticle(
         smokeParticle,
         true,
         pos.getX() + 0.5 + random.nextDouble() / 3.0 * (random.nextBoolean() ? 1 : -1),
         pos.getY() + random.nextDouble() + random.nextDouble(),
         pos.getZ() + 0.5 + random.nextDouble() / 3.0 * (random.nextBoolean() ? 1 : -1),
         0.0,
         0.07,
         0.0
      );
      if (smoking) {
         level.addParticle(
            ParticleTypes.SMOKE,
            pos.getX() + 0.5 + random.nextDouble() / 4.0 * (random.nextBoolean() ? 1 : -1),
            pos.getY() + 0.4,
            pos.getZ() + 0.5 + random.nextDouble() / 4.0 * (random.nextBoolean() ? 1 : -1),
            0.0,
            0.005,
            0.0
         );
      }
   }

   public static boolean isSmokeyPos(final Level level, final BlockPos pos) {
      for (int i = 1; i <= 5; i++) {
         BlockPos posToCheck = pos.below(i);
         BlockState blockState = level.getBlockState(posToCheck);
         if (isLitCampfire(blockState)) {
            return true;
         }

         boolean smokeBlocked = Shapes.joinIsNotEmpty(SHAPE_VIRTUAL_POST, blockState.getCollisionShape(level, pos, CollisionContext.empty()), BooleanOp.AND);
         if (smokeBlocked) {
            BlockState belowState = level.getBlockState(posToCheck.below());
            return isLitCampfire(belowState);
         }
      }

      return false;
   }

   public static boolean isLitCampfire(final BlockState blockState) {
      return blockState.hasProperty(LIT) && blockState.is(BlockTags.CAMPFIRES) && blockState.getValue(LIT);
   }

   @Override
   protected FluidState getFluidState(final BlockState state) {
      return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
   }

   @Override
   protected BlockState rotate(final BlockState state, final Rotation rotation) {
      return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
   }

   @Override
   protected BlockState mirror(final BlockState state, final Mirror mirror) {
      return state.rotate(mirror.getRotation(state.getValue(FACING)));
   }

   @Override
   protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(LIT, SIGNAL_FIRE, WATERLOGGED, FACING);
   }

   @Override
   public BlockEntity newBlockEntity(final BlockPos worldPosition, final BlockState blockState) {
      return new CampfireBlockEntity(worldPosition, blockState);
   }

   @Override
   public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(final Level level, final BlockState blockState, final BlockEntityType<T> type) {
      if (level instanceof ServerLevel serverLevel) {
         if (blockState.getValue(LIT)) {
            RecipeManager.CachedCheck<SingleRecipeInput, CampfireCookingRecipe> quickCheck = RecipeManager.createCheck(RecipeType.CAMPFIRE_COOKING);
            return createTickerHelper(
               type, BlockEntityTypes.CAMPFIRE, (innerLevel, pos, state, entity) -> CampfireBlockEntity.cookTick(serverLevel, pos, state, entity, quickCheck)
            );
         } else {
            return createTickerHelper(type, BlockEntityTypes.CAMPFIRE, CampfireBlockEntity::cooldownTick);
         }
      } else {
         return blockState.getValue(LIT) ? createTickerHelper(type, BlockEntityTypes.CAMPFIRE, CampfireBlockEntity::particleTick) : null;
      }
   }

   @Override
   protected boolean isPathfindable(final BlockState state, final PathComputationType type) {
      return false;
   }

   public static boolean canLight(final BlockState state) {
      return state.is(BlockTags.CAMPFIRES, s -> s.hasProperty(WATERLOGGED) && s.hasProperty(LIT)) && !state.getValue(WATERLOGGED) && !state.getValue(LIT);
   }
}
