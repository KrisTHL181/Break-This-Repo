package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RespawnAnchorBlock extends Block {
   public static final MapCodec<RespawnAnchorBlock> CODEC = simpleCodec(RespawnAnchorBlock::new);
   public static final int MIN_CHARGES = 0;
   public static final int MAX_CHARGES = 4;
   public static final IntegerProperty CHARGE = BlockStateProperties.RESPAWN_ANCHOR_CHARGES;
   private static final ImmutableList<Vec3i> RESPAWN_HORIZONTAL_OFFSETS = ImmutableList.of(
      new Vec3i(0, 0, -1),
      new Vec3i(-1, 0, 0),
      new Vec3i(0, 0, 1),
      new Vec3i(1, 0, 0),
      new Vec3i(-1, 0, -1),
      new Vec3i(1, 0, -1),
      new Vec3i(-1, 0, 1),
      new Vec3i(1, 0, 1)
   );
   private static final ImmutableList<Vec3i> RESPAWN_OFFSETS = new Builder()
      .addAll(RESPAWN_HORIZONTAL_OFFSETS)
      .addAll(RESPAWN_HORIZONTAL_OFFSETS.stream().map(Vec3i::below).iterator())
      .addAll(RESPAWN_HORIZONTAL_OFFSETS.stream().map(Vec3i::above).iterator())
      .add(new Vec3i(0, 1, 0))
      .build();

   @Override
   public MapCodec<RespawnAnchorBlock> codec() {
      return CODEC;
   }

   public RespawnAnchorBlock(BlockBehaviour.Properties p_55838_) {
      super(p_55838_);
      this.registerDefaultState(this.stateDefinition.any().setValue(CHARGE, 0));
   }

   @Override
   protected InteractionResult useItemOn(
      ItemStack p_334655_, BlockState p_333892_, Level p_327960_, BlockPos p_330278_, Player p_331169_, InteractionHand p_328336_, BlockHitResult p_333705_
   ) {
      if (isRespawnFuel(p_334655_) && canBeCharged(p_333892_)) {
         charge(p_331169_, p_327960_, p_330278_, p_333892_);
         p_334655_.consume(1, p_331169_);
         return InteractionResult.SUCCESS;
      } else {
         return (InteractionResult)(
            p_328336_ == InteractionHand.MAIN_HAND && isRespawnFuel(p_331169_.getItemInHand(InteractionHand.OFF_HAND)) && canBeCharged(p_333892_)
               ? InteractionResult.PASS
               : InteractionResult.TRY_WITH_EMPTY_HAND
         );
      }
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState p_331701_, Level p_333411_, BlockPos p_329077_, Player p_334041_, BlockHitResult p_328905_) {
      if (p_331701_.getValue(CHARGE) == 0) {
         return InteractionResult.PASS;
      }

      if (p_333411_ instanceof ServerLevel serverlevel) {
         if (!canSetSpawn(serverlevel, p_329077_)) {
            this.explode(p_331701_, serverlevel, p_329077_);
            return InteractionResult.SUCCESS_SERVER;
         }

         if (p_334041_ instanceof ServerPlayer serverplayer) {
            ServerPlayer.RespawnConfig serverplayer$respawnconfig = serverplayer.getRespawnConfig();
            ServerPlayer.RespawnConfig serverplayer$respawnconfig1 = new ServerPlayer.RespawnConfig(
               LevelData.RespawnData.of(serverlevel.dimension(), p_329077_, 0.0F, 0.0F), false
            );
            if (serverplayer$respawnconfig == null || !serverplayer$respawnconfig.isSamePosition(serverplayer$respawnconfig1)) {
               serverplayer.setRespawnPosition(serverplayer$respawnconfig1, true);
               serverlevel.playSound(
                  null,
                  p_329077_.getX() + 0.5,
                  p_329077_.getY() + 0.5,
                  p_329077_.getZ() + 0.5,
                  SoundEvents.RESPAWN_ANCHOR_SET_SPAWN,
                  SoundSource.BLOCKS,
                  1.0F,
                  1.0F
               );
               return InteractionResult.SUCCESS_SERVER;
            }
         }

         return InteractionResult.CONSUME;
      } else {
         return InteractionResult.CONSUME;
      }
   }

   private static boolean isRespawnFuel(ItemStack p_55849_) {
      return p_55849_.is(Items.GLOWSTONE);
   }

   private static boolean canBeCharged(BlockState p_55895_) {
      return p_55895_.getValue(CHARGE) < 4;
   }

   private static boolean isWaterThatWouldFlow(BlockPos p_55888_, Level p_55889_) {
      FluidState fluidstate = p_55889_.getFluidState(p_55888_);
      if (!fluidstate.is(FluidTags.WATER)) {
         return false;
      }

      if (fluidstate.isSource()) {
         return true;
      }

      float f = fluidstate.getAmount();
      if (f < 2.0F) {
         return false;
      }

      FluidState fluidstate1 = p_55889_.getFluidState(p_55888_.below());
      return !fluidstate1.is(FluidTags.WATER);
   }

   private void explode(BlockState p_55891_, ServerLevel p_458613_, final BlockPos p_55893_) {
      p_458613_.removeBlock(p_55893_, false);
      boolean flag = Direction.Plane.HORIZONTAL.stream().map(p_55893_::relative).anyMatch(p_55854_ -> isWaterThatWouldFlow(p_55854_, p_458613_));
      final boolean flag1 = flag || p_458613_.getFluidState(p_55893_.above()).is(FluidTags.WATER);
      ExplosionDamageCalculator explosiondamagecalculator = new ExplosionDamageCalculator() {
         @Override
         public Optional<Float> getBlockExplosionResistance(
            Explosion p_55904_, BlockGetter p_55905_, BlockPos p_55906_, BlockState p_55907_, FluidState p_55908_
         ) {
            return p_55906_.equals(p_55893_) && flag1
               ? Optional.of(Blocks.WATER.getExplosionResistance())
               : super.getBlockExplosionResistance(p_55904_, p_55905_, p_55906_, p_55907_, p_55908_);
         }
      };
      Vec3 vec3 = p_55893_.getCenter();
      p_458613_.explode(
         null, p_458613_.damageSources().badRespawnPointExplosion(vec3), explosiondamagecalculator, vec3, 5.0F, true, Level.ExplosionInteraction.BLOCK
      );
   }

   public static boolean canSetSpawn(ServerLevel p_454510_, BlockPos p_459085_) {
      return p_454510_.environmentAttributes().getValue(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS, p_459085_);
   }

   public static void charge(@Nullable Entity p_270997_, Level p_270172_, BlockPos p_270534_, BlockState p_270661_) {
      BlockState blockstate = p_270661_.setValue(CHARGE, p_270661_.getValue(CHARGE) + 1);
      p_270172_.setBlock(p_270534_, blockstate, 3);
      p_270172_.gameEvent(GameEvent.BLOCK_CHANGE, p_270534_, GameEvent.Context.of(p_270997_, blockstate));
      p_270172_.playSound(
         null, p_270534_.getX() + 0.5, p_270534_.getY() + 0.5, p_270534_.getZ() + 0.5, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 1.0F, 1.0F
      );
   }

   @Override
   public void animateTick(BlockState p_221969_, Level p_221970_, BlockPos p_221971_, RandomSource p_221972_) {
      if (p_221969_.getValue(CHARGE) != 0) {
         if (p_221972_.nextInt(100) == 0) {
            p_221970_.playLocalSound(p_221971_, SoundEvents.RESPAWN_ANCHOR_AMBIENT, SoundSource.BLOCKS, 1.0F, 1.0F, false);
         }

         double d0 = p_221971_.getX() + 0.5 + (0.5 - p_221972_.nextDouble());
         double d1 = p_221971_.getY() + 1.0;
         double d2 = p_221971_.getZ() + 0.5 + (0.5 - p_221972_.nextDouble());
         double d3 = p_221972_.nextFloat() * 0.04;
         p_221970_.addParticle(ParticleTypes.REVERSE_PORTAL, d0, d1, d2, 0.0, d3, 0.0);
      }
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_55886_) {
      p_55886_.add(CHARGE);
   }

   @Override
   protected boolean hasAnalogOutputSignal(BlockState p_55860_) {
      return true;
   }

   public static int getScaledChargeLevel(BlockState p_55862_, int p_55863_) {
      return Mth.floor((p_55862_.getValue(CHARGE) - 0) / 4.0F * p_55863_);
   }

   @Override
   protected int getAnalogOutputSignal(BlockState p_55870_, Level p_55871_, BlockPos p_55872_, Direction p_423305_) {
      return getScaledChargeLevel(p_55870_, 15);
   }

   public static Optional<Vec3> findStandUpPosition(EntityType<?> p_55840_, CollisionGetter p_55841_, BlockPos p_55842_) {
      Optional<Vec3> optional = findStandUpPosition(p_55840_, p_55841_, p_55842_, true);
      return optional.isPresent() ? optional : findStandUpPosition(p_55840_, p_55841_, p_55842_, false);
   }

   private static Optional<Vec3> findStandUpPosition(EntityType<?> p_55844_, CollisionGetter p_55845_, BlockPos p_55846_, boolean p_55847_) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
      UnmodifiableIterator var5 = RESPAWN_OFFSETS.iterator();

      while (var5.hasNext()) {
         Vec3i vec3i = (Vec3i)var5.next();
         blockpos$mutableblockpos.set(p_55846_).move(vec3i);
         Vec3 vec3 = DismountHelper.findSafeDismountLocation(p_55844_, p_55845_, blockpos$mutableblockpos, p_55847_);
         if (vec3 != null) {
            return Optional.of(vec3);
         }
      }

      return Optional.empty();
   }

   @Override
   protected boolean isPathfindable(BlockState p_55865_, PathComputationType p_55868_) {
      return false;
   }
}
