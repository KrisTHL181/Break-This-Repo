package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SweetBerryBushBlock extends VegetationBlock implements BonemealableBlock {
   private static final float HURT_SPEED_THRESHOLD = 0.003F;
   public static final int MAX_AGE = 3;
   public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
   private static final VoxelShape SHAPE_SAPLING = Block.column(10.0, 0.0, 8.0);
   private static final VoxelShape SHAPE_GROWING = Block.column(14.0, 0.0, 16.0);

   public SweetBerryBushBlock(final BlockBehaviour.Properties properties) {
      super(properties);
      this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
   }

   @Override
   protected ItemStack getCloneItemStack(final LevelReader level, final BlockPos pos, final BlockState state, final boolean includeData) {
      return new ItemStack(Items.SWEET_BERRIES);
   }

   @Override
   protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
      return switch (state.getValue(AGE)) {
         case 0 -> SHAPE_SAPLING;
         case 3 -> Shapes.block();
         default -> SHAPE_GROWING;
      };
   }

   @Override
   protected boolean isRandomlyTicking(final BlockState state) {
      return state.getValue(AGE) < 3;
   }

   @Override
   protected void randomTick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
      int age = state.getValue(AGE);
      if (age < 3 && random.nextInt(5) == 0 && level.getRawBrightness(pos.above(), 0) >= 9) {
         BlockState newState = state.setValue(AGE, age + 1);
         level.setBlock(pos, newState, 2);
         level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(newState));
      }
   }

   @Override
   protected void entityInside(
      final BlockState state, final Level level, final BlockPos pos, final Entity entity, final InsideBlockEffectApplier effectApplier, final boolean isPrecise
   ) {
      if (entity instanceof LivingEntity && !entity.is(EntityTypes.FOX) && !entity.is(EntityTypes.BEE)) {
         entity.makeStuckInBlock(state, new Vec3(0.8F, 0.75, 0.8F));
         if (level instanceof ServerLevel serverLevel && state.getValue(AGE) != 0) {
            Vec3 movement = entity.isClientAuthoritative() ? entity.getKnownMovement() : entity.oldPosition().subtract(entity.position());
            if (movement.horizontalDistanceSqr() > 0.0) {
               double xs = Math.abs(movement.x());
               double zs = Math.abs(movement.z());
               if (xs >= 0.003F || zs >= 0.003F) {
                  entity.hurtServer(serverLevel, level.damageSources().sweetBerryBush(), 1.0F);
               }
            }
         }
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
      int age = state.getValue(AGE);
      boolean isMaxAge = age == 3;
      return !isMaxAge && itemStack.is(Items.BONE_MEAL) ? InteractionResult.PASS : super.useItemOn(itemStack, state, level, pos, player, hand, hitResult);
   }

   @Override
   protected InteractionResult useWithoutItem(
      final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult
   ) {
      if (state.getValue(AGE) > 1) {
         if (level instanceof ServerLevel serverLevel) {
            Block.dropFromBlockInteractLootTable(
               serverLevel,
               BuiltInLootTables.HARVEST_SWEET_BERRY_BUSH,
               state,
               level.getBlockEntity(pos),
               null,
               player,
               (serverlvl, itemStack) -> Block.popResource(serverlvl, pos, itemStack)
            );
            serverLevel.playSound(
               null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.8F + serverLevel.getRandom().nextFloat() * 0.4F
            );
            BlockState newState = state.setValue(AGE, 1);
            serverLevel.setBlock(pos, newState, 2);
            serverLevel.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, newState));
         }

         return InteractionResult.SUCCESS;
      } else {
         return super.useWithoutItem(state, level, pos, player, hitResult);
      }
   }

   @Override
   protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(AGE);
   }

   @Override
   public boolean isValidBonemealTarget(final LevelReader level, final BlockPos pos, final BlockState state) {
      return state.getValue(AGE) < 3;
   }

   @Override
   public boolean isBonemealSuccess(final Level level, final RandomSource random, final BlockPos pos, final BlockState state) {
      return true;
   }

   @Override
   public void performBonemeal(final ServerLevel level, final RandomSource random, final BlockPos pos, final BlockState state) {
      int newAge = Math.min(3, state.getValue(AGE) + 1);
      level.setBlock(pos, state.setValue(AGE, newAge), 2);
   }
}
