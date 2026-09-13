package net.minecraft.world.level.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
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
   public static final MapCodec<CampfireBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_422082_ -> p_422082_.group(
            Codec.BOOL.fieldOf("spawn_particles").forGetter(p_309275_ -> p_309275_.spawnParticles),
            Codec.intRange(0, 1000).fieldOf("fire_damage").forGetter(p_309277_ -> p_309277_.fireDamage),
            propertiesCodec()
         )
         .apply(p_422082_, CampfireBlock::new)
   );
   public static final BooleanProperty LIT = BlockStateProperties.LIT;
   public static final BooleanProperty SIGNAL_FIRE = BlockStateProperties.SIGNAL_FIRE;
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
   private static final VoxelShape SHAPE = Block.column(16.0, 0.0, 7.0);
   private static final VoxelShape SHAPE_VIRTUAL_POST = Block.column(4.0, 0.0, 16.0);
   private static final int SMOKE_DISTANCE = 5;
   private final boolean spawnParticles;
   private final int fireDamage;

   @Override
   public MapCodec<CampfireBlock> codec() {
      return CODEC;
   }

   public CampfireBlock(boolean p_51236_, int p_51237_, BlockBehaviour.Properties p_51238_) {
      super(p_51238_);
      this.spawnParticles = p_51236_;
      this.fireDamage = p_51237_;
      this.registerDefaultState(
         this.stateDefinition.any().setValue(LIT, true).setValue(SIGNAL_FIRE, false).setValue(WATERLOGGED, false).setValue(FACING, Direction.NORTH)
      );
   }

   @Override
   protected InteractionResult useItemOn(
      ItemStack p_334288_, BlockState p_51274_, Level p_51275_, BlockPos p_51276_, Player p_51277_, InteractionHand p_51278_, BlockHitResult p_51279_
   ) {
      if (p_51275_.getBlockEntity(p_51276_) instanceof CampfireBlockEntity campfireblockentity) {
         ItemStack itemstack = p_51277_.getItemInHand(p_51278_);
         if (p_51275_.recipeAccess().propertySet(RecipePropertySet.CAMPFIRE_INPUT).test(itemstack)) {
            if (p_51275_ instanceof ServerLevel serverlevel && campfireblockentity.placeFood(serverlevel, p_51277_, itemstack)) {
               p_51277_.awardStat(Stats.INTERACT_WITH_CAMPFIRE);
               return InteractionResult.SUCCESS_SERVER;
            }

            return InteractionResult.CONSUME;
         }
      }

      return InteractionResult.TRY_WITH_EMPTY_HAND;
   }

   @Override
   protected void entityInside(BlockState p_51269_, Level p_51270_, BlockPos p_51271_, Entity p_51272_, InsideBlockEffectApplier p_395061_, boolean p_432032_) {
      if (p_51269_.getValue(LIT) && p_51272_ instanceof LivingEntity) {
         p_51272_.hurt(p_51270_.damageSources().campfire(), this.fireDamage);
      }

      super.entityInside(p_51269_, p_51270_, p_51271_, p_51272_, p_395061_, p_432032_);
   }

   @Override
   public @Nullable BlockState getStateForPlacement(BlockPlaceContext p_51240_) {
      LevelAccessor levelaccessor = p_51240_.getLevel();
      BlockPos blockpos = p_51240_.getClickedPos();
      boolean flag = levelaccessor.getFluidState(blockpos).getType() == Fluids.WATER;
      return this.defaultBlockState()
         .setValue(WATERLOGGED, flag)
         .setValue(SIGNAL_FIRE, this.isSmokeSource(levelaccessor.getBlockState(blockpos.below())))
         .setValue(LIT, !flag)
         .setValue(FACING, p_51240_.getHorizontalDirection());
   }

   @Override
   protected BlockState updateShape(
      BlockState p_51298_,
      LevelReader p_368205_,
      ScheduledTickAccess p_365108_,
      BlockPos p_51302_,
      Direction p_51299_,
      BlockPos p_51303_,
      BlockState p_51300_,
      RandomSource p_366447_
   ) {
      if (p_51298_.getValue(WATERLOGGED)) {
         p_365108_.scheduleTick(p_51302_, Fluids.WATER, Fluids.WATER.getTickDelay(p_368205_));
      }

      return p_51299_ == Direction.DOWN
         ? p_51298_.setValue(SIGNAL_FIRE, this.isSmokeSource(p_51300_))
         : super.updateShape(p_51298_, p_368205_, p_365108_, p_51302_, p_51299_, p_51303_, p_51300_, p_366447_);
   }

   private boolean isSmokeSource(BlockState p_51324_) {
      return p_51324_.is(Blocks.HAY_BLOCK);
   }

   @Override
   protected VoxelShape getShape(BlockState p_51309_, BlockGetter p_51310_, BlockPos p_51311_, CollisionContext p_51312_) {
      return SHAPE;
   }

   @Override
   public void animateTick(BlockState p_220918_, Level p_220919_, BlockPos p_220920_, RandomSource p_220921_) {
      if (p_220918_.getValue(LIT)) {
         if (p_220921_.nextInt(10) == 0) {
            p_220919_.playLocalSound(
               p_220920_.getX() + 0.5,
               p_220920_.getY() + 0.5,
               p_220920_.getZ() + 0.5,
               SoundEvents.CAMPFIRE_CRACKLE,
               SoundSource.BLOCKS,
               0.5F + p_220921_.nextFloat(),
               p_220921_.nextFloat() * 0.7F + 0.6F,
               false
            );
         }

         if (this.spawnParticles && p_220921_.nextInt(5) == 0) {
            for (int i = 0; i < p_220921_.nextInt(1) + 1; i++) {
               p_220919_.addParticle(
                  ParticleTypes.LAVA,
                  p_220920_.getX() + 0.5,
                  p_220920_.getY() + 0.5,
                  p_220920_.getZ() + 0.5,
                  p_220921_.nextFloat() / 2.0F,
                  5.0E-5,
                  p_220921_.nextFloat() / 2.0F
               );
            }
         }
      }
   }

   public static void dowse(@Nullable Entity p_152750_, LevelAccessor p_152751_, BlockPos p_152752_, BlockState p_152753_) {
      if (p_152751_.isClientSide()) {
         for (int i = 0; i < 20; i++) {
            makeParticles((Level)p_152751_, p_152752_, p_152753_.getValue(SIGNAL_FIRE), true);
         }
      }

      p_152751_.gameEvent(p_152750_, GameEvent.BLOCK_CHANGE, p_152752_);
   }

   @Override
   public boolean placeLiquid(LevelAccessor p_51257_, BlockPos p_51258_, BlockState p_51259_, FluidState p_51260_) {
      if (!p_51259_.getValue(BlockStateProperties.WATERLOGGED) && p_51260_.getType() == Fluids.WATER) {
         boolean flag = p_51259_.getValue(LIT);
         if (flag) {
            if (!p_51257_.isClientSide()) {
               p_51257_.playSound(null, p_51258_, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 1.0F, 1.0F);
            }

            dowse(null, p_51257_, p_51258_, p_51259_);
         }

         p_51257_.setBlock(p_51258_, p_51259_.setValue(WATERLOGGED, true).setValue(LIT, false), 3);
         p_51257_.scheduleTick(p_51258_, p_51260_.getType(), p_51260_.getType().getTickDelay(p_51257_));
         return true;
      } else {
         return false;
      }
   }

   @Override
   protected void onProjectileHit(Level p_51244_, BlockState p_51245_, BlockHitResult p_51246_, Projectile p_51247_) {
      BlockPos blockpos = p_51246_.getBlockPos();
      if (p_51244_ instanceof ServerLevel serverlevel
         && p_51247_.isOnFire()
         && p_51247_.mayInteract(serverlevel, blockpos)
         && !p_51245_.getValue(LIT)
         && !p_51245_.getValue(WATERLOGGED)) {
         p_51244_.setBlock(blockpos, p_51245_.setValue(BlockStateProperties.LIT, true), 11);
      }
   }

   public static void makeParticles(Level p_51252_, BlockPos p_51253_, boolean p_51254_, boolean p_51255_) {
      RandomSource randomsource = p_51252_.getRandom();
      SimpleParticleType simpleparticletype = p_51254_ ? ParticleTypes.CAMPFIRE_SIGNAL_SMOKE : ParticleTypes.CAMPFIRE_COSY_SMOKE;
      p_51252_.addAlwaysVisibleParticle(
         simpleparticletype,
         true,
         p_51253_.getX() + 0.5 + randomsource.nextDouble() / 3.0 * (randomsource.nextBoolean() ? 1 : -1),
         p_51253_.getY() + randomsource.nextDouble() + randomsource.nextDouble(),
         p_51253_.getZ() + 0.5 + randomsource.nextDouble() / 3.0 * (randomsource.nextBoolean() ? 1 : -1),
         0.0,
         0.07,
         0.0
      );
      if (p_51255_) {
         p_51252_.addParticle(
            ParticleTypes.SMOKE,
            p_51253_.getX() + 0.5 + randomsource.nextDouble() / 4.0 * (randomsource.nextBoolean() ? 1 : -1),
            p_51253_.getY() + 0.4,
            p_51253_.getZ() + 0.5 + randomsource.nextDouble() / 4.0 * (randomsource.nextBoolean() ? 1 : -1),
            0.0,
            0.005,
            0.0
         );
      }
   }

   public static boolean isSmokeyPos(Level p_51249_, BlockPos p_51250_) {
      for (int i = 1; i <= 5; i++) {
         BlockPos blockpos = p_51250_.below(i);
         BlockState blockstate = p_51249_.getBlockState(blockpos);
         if (isLitCampfire(blockstate)) {
            return true;
         }

         boolean flag = Shapes.joinIsNotEmpty(SHAPE_VIRTUAL_POST, blockstate.getCollisionShape(p_51249_, p_51250_, CollisionContext.empty()), BooleanOp.AND);
         if (flag) {
            BlockState blockstate1 = p_51249_.getBlockState(blockpos.below());
            return isLitCampfire(blockstate1);
         }
      }

      return false;
   }

   public static boolean isLitCampfire(BlockState p_51320_) {
      return p_51320_.hasProperty(LIT) && p_51320_.is(BlockTags.CAMPFIRES) && p_51320_.getValue(LIT);
   }

   @Override
   protected FluidState getFluidState(BlockState p_51318_) {
      return p_51318_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_51318_);
   }

   @Override
   protected BlockState rotate(BlockState p_51295_, Rotation p_51296_) {
      return p_51295_.setValue(FACING, p_51296_.rotate(p_51295_.getValue(FACING)));
   }

   @Override
   protected BlockState mirror(BlockState p_51292_, Mirror p_51293_) {
      return p_51292_.rotate(p_51293_.getRotation(p_51292_.getValue(FACING)));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_51305_) {
      p_51305_.add(LIT, SIGNAL_FIRE, WATERLOGGED, FACING);
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos p_152759_, BlockState p_152760_) {
      return new CampfireBlockEntity(p_152759_, p_152760_);
   }

   @Override
   public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level p_152755_, BlockState p_152756_, BlockEntityType<T> p_152757_) {
      if (p_152755_ instanceof ServerLevel serverlevel) {
         if (p_152756_.getValue(LIT)) {
            RecipeManager.CachedCheck<SingleRecipeInput, CampfireCookingRecipe> cachedcheck = RecipeManager.createCheck(RecipeType.CAMPFIRE_COOKING);
            return createTickerHelper(
               p_152757_,
               BlockEntityType.CAMPFIRE,
               (p_360409_, p_360410_, p_360411_, p_360412_) -> CampfireBlockEntity.cookTick(serverlevel, p_360410_, p_360411_, p_360412_, cachedcheck)
            );
         } else {
            return createTickerHelper(p_152757_, BlockEntityType.CAMPFIRE, CampfireBlockEntity::cooldownTick);
         }
      } else {
         return p_152756_.getValue(LIT) ? createTickerHelper(p_152757_, BlockEntityType.CAMPFIRE, CampfireBlockEntity::particleTick) : null;
      }
   }

   @Override
   protected boolean isPathfindable(BlockState p_51264_, PathComputationType p_51267_) {
      return false;
   }

   public static boolean canLight(BlockState p_51322_) {
      return p_51322_.is(BlockTags.CAMPFIRES, p_51262_ -> p_51262_.hasProperty(WATERLOGGED) && p_51262_.hasProperty(LIT))
         && !p_51322_.getValue(WATERLOGGED)
         && !p_51322_.getValue(LIT);
   }
}
