package net.minecraft.world.level.block;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.BlockUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class NetherPortalBlock extends Block implements Portal {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final MapCodec<NetherPortalBlock> CODEC = simpleCodec(NetherPortalBlock::new);
   public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
   private static final Map<Direction.Axis, VoxelShape> SHAPES = Shapes.rotateHorizontalAxis(Block.column(4.0, 16.0, 0.0, 16.0));

   @Override
   public MapCodec<NetherPortalBlock> codec() {
      return CODEC;
   }

   public NetherPortalBlock(BlockBehaviour.Properties p_54909_) {
      super(p_54909_);
      this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X));
   }

   @Override
   protected VoxelShape getShape(BlockState p_54942_, BlockGetter p_54943_, BlockPos p_54944_, CollisionContext p_54945_) {
      return SHAPES.get(p_54942_.getValue(AXIS));
   }

   @Override
   protected void randomTick(BlockState p_221799_, ServerLevel p_221800_, BlockPos p_221801_, RandomSource p_221802_) {
      if (p_221800_.isSpawningMonsters()
         && p_221800_.environmentAttributes().getValue(EnvironmentAttributes.NETHER_PORTAL_SPAWNS_PIGLINS, p_221801_)
         && p_221802_.nextInt(2000) < p_221800_.getDifficulty().getId()
         && p_221800_.anyPlayerCloseEnoughForSpawning(p_221801_)) {
         while (p_221800_.getBlockState(p_221801_).is(this)) {
            p_221801_ = p_221801_.below();
         }

         if (p_221800_.getBlockState(p_221801_).isValidSpawn(p_221800_, p_221801_, EntityType.ZOMBIFIED_PIGLIN)) {
            Entity entity = EntityType.ZOMBIFIED_PIGLIN.spawn(p_221800_, p_221801_.above(), EntitySpawnReason.STRUCTURE);
            if (entity != null) {
               entity.setPortalCooldown();
               Entity entity1 = entity.getVehicle();
               if (entity1 != null) {
                  entity1.setPortalCooldown();
               }
            }
         }
      }
   }

   @Override
   protected BlockState updateShape(
      BlockState p_54928_,
      LevelReader p_367647_,
      ScheduledTickAccess p_363604_,
      BlockPos p_54932_,
      Direction p_54929_,
      BlockPos p_54933_,
      BlockState p_54930_,
      RandomSource p_368816_
   ) {
      Direction.Axis direction$axis = p_54929_.getAxis();
      Direction.Axis direction$axis1 = p_54928_.getValue(AXIS);
      boolean flag = direction$axis1 != direction$axis && direction$axis.isHorizontal();
      return !flag && !p_54930_.is(this) && !PortalShape.findAnyShape(p_367647_, p_54932_, direction$axis1).isComplete()
         ? Blocks.AIR.defaultBlockState()
         : super.updateShape(p_54928_, p_367647_, p_363604_, p_54932_, p_54929_, p_54933_, p_54930_, p_368816_);
   }

   @Override
   protected void entityInside(BlockState p_54915_, Level p_54916_, BlockPos p_54917_, Entity p_54918_, InsideBlockEffectApplier p_392916_, boolean p_432041_) {
      if (p_54918_.canUsePortal(false)) {
         p_54918_.setAsInsidePortal(this, p_54917_);
      }
   }

   @Override
   public int getPortalTransitionTime(ServerLevel p_342064_, Entity p_344634_) {
      return p_344634_ instanceof Player player
         ? Math.max(
            0,
            p_342064_.getGameRules()
               .get(player.getAbilities().invulnerable ? GameRules.PLAYERS_NETHER_PORTAL_CREATIVE_DELAY : GameRules.PLAYERS_NETHER_PORTAL_DEFAULT_DELAY)
         )
         : 0;
   }

   @Override
   public @Nullable TeleportTransition getPortalDestination(ServerLevel p_342106_, Entity p_343065_, BlockPos p_344977_) {
      ResourceKey<Level> resourcekey = p_342106_.dimension() == Level.NETHER ? Level.OVERWORLD : Level.NETHER;
      ServerLevel serverlevel = p_342106_.getServer().getLevel(resourcekey);
      if (serverlevel == null) {
         return null;
      }

      boolean flag = serverlevel.dimension() == Level.NETHER;
      WorldBorder worldborder = serverlevel.getWorldBorder();
      double d0 = DimensionType.getTeleportationScale(p_342106_.dimensionType(), serverlevel.dimensionType());
      BlockPos blockpos = worldborder.clampToBounds(p_343065_.getX() * d0, p_343065_.getY(), p_343065_.getZ() * d0);
      return this.getExitPortal(serverlevel, p_343065_, p_344977_, blockpos, flag, worldborder);
   }

   private @Nullable TeleportTransition getExitPortal(
      ServerLevel p_343269_, Entity p_343673_, BlockPos p_343381_, BlockPos p_343194_, boolean p_343644_, WorldBorder p_343185_
   ) {
      Optional<BlockPos> optional = p_343269_.getPortalForcer().findClosestPortalPosition(p_343194_, p_343644_, p_343185_);
      BlockUtil.FoundRectangle blockutil$foundrectangle;
      TeleportTransition.PostTeleportTransition teleporttransition$postteleporttransition;
      if (optional.isPresent()) {
         BlockPos blockpos = optional.get();
         BlockState blockstate = p_343269_.getBlockState(blockpos);
         blockutil$foundrectangle = BlockUtil.getLargestRectangleAround(
            blockpos,
            blockstate.getValue(BlockStateProperties.HORIZONTAL_AXIS),
            21,
            Direction.Axis.Y,
            21,
            p_343533_ -> p_343269_.getBlockState(p_343533_) == blockstate
         );
         teleporttransition$postteleporttransition = TeleportTransition.PLAY_PORTAL_SOUND.then(p_343530_ -> p_343530_.placePortalTicket(blockpos));
      } else {
         Direction.Axis direction$axis = p_343673_.level().getBlockState(p_343381_).getOptionalValue(AXIS).orElse(Direction.Axis.X);
         Optional<BlockUtil.FoundRectangle> optional1 = p_343269_.getPortalForcer().createPortal(p_343194_, direction$axis);
         if (optional1.isEmpty()) {
            LOGGER.error("Unable to create a portal, likely target out of worldborder");
            return null;
         }

         blockutil$foundrectangle = optional1.get();
         teleporttransition$postteleporttransition = TeleportTransition.PLAY_PORTAL_SOUND.then(TeleportTransition.PLACE_PORTAL_TICKET);
      }

      return getDimensionTransitionFromExit(p_343673_, p_343381_, blockutil$foundrectangle, p_343269_, teleporttransition$postteleporttransition);
   }

   private static TeleportTransition getDimensionTransitionFromExit(
      Entity p_344252_, BlockPos p_343376_, BlockUtil.FoundRectangle p_456892_, ServerLevel p_343963_, TeleportTransition.PostTeleportTransition p_368919_
   ) {
      BlockState blockstate = p_344252_.level().getBlockState(p_343376_);
      Direction.Axis direction$axis;
      Vec3 vec3;
      if (blockstate.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)) {
         direction$axis = blockstate.getValue(BlockStateProperties.HORIZONTAL_AXIS);
         BlockUtil.FoundRectangle blockutil$foundrectangle = BlockUtil.getLargestRectangleAround(
            p_343376_, direction$axis, 21, Direction.Axis.Y, 21, p_342174_ -> p_344252_.level().getBlockState(p_342174_) == blockstate
         );
         vec3 = p_344252_.getRelativePortalPosition(direction$axis, blockutil$foundrectangle);
      } else {
         direction$axis = Direction.Axis.X;
         vec3 = new Vec3(0.5, 0.0, 0.0);
      }

      return createDimensionTransition(p_343963_, p_456892_, direction$axis, vec3, p_344252_, p_368919_);
   }

   private static TeleportTransition createDimensionTransition(
      ServerLevel p_344368_,
      BlockUtil.FoundRectangle p_453085_,
      Direction.Axis p_345454_,
      Vec3 p_344397_,
      Entity p_344167_,
      TeleportTransition.PostTeleportTransition p_361307_
   ) {
      BlockPos blockpos = p_453085_.minCorner;
      BlockState blockstate = p_344368_.getBlockState(blockpos);
      Direction.Axis direction$axis = blockstate.getOptionalValue(BlockStateProperties.HORIZONTAL_AXIS).orElse(Direction.Axis.X);
      double d0 = p_453085_.axis1Size;
      double d1 = p_453085_.axis2Size;
      EntityDimensions entitydimensions = p_344167_.getDimensions(p_344167_.getPose());
      int i = p_345454_ == direction$axis ? 0 : 90;
      double d2 = entitydimensions.width() / 2.0 + (d0 - entitydimensions.width()) * p_344397_.x();
      double d3 = (d1 - entitydimensions.height()) * p_344397_.y();
      double d4 = 0.5 + p_344397_.z();
      boolean flag = direction$axis == Direction.Axis.X;
      Vec3 vec3 = new Vec3(blockpos.getX() + (flag ? d2 : d4), blockpos.getY() + d3, blockpos.getZ() + (flag ? d4 : d2));
      Vec3 vec31 = PortalShape.findCollisionFreePosition(vec3, p_344368_, p_344167_, entitydimensions);
      return new TeleportTransition(p_344368_, vec31, Vec3.ZERO, i, 0.0F, Relative.union(Relative.DELTA, Relative.ROTATION), p_361307_);
   }

   @Override
   public Portal.Transition getLocalTransition() {
      return Portal.Transition.CONFUSION;
   }

   @Override
   public void animateTick(BlockState p_221794_, Level p_221795_, BlockPos p_221796_, RandomSource p_221797_) {
      if (p_221797_.nextInt(100) == 0) {
         p_221795_.playLocalSound(
            p_221796_.getX() + 0.5,
            p_221796_.getY() + 0.5,
            p_221796_.getZ() + 0.5,
            SoundEvents.PORTAL_AMBIENT,
            SoundSource.BLOCKS,
            0.5F,
            p_221797_.nextFloat() * 0.4F + 0.8F,
            false
         );
      }

      for (int i = 0; i < 4; i++) {
         double d0 = p_221796_.getX() + p_221797_.nextDouble();
         double d1 = p_221796_.getY() + p_221797_.nextDouble();
         double d2 = p_221796_.getZ() + p_221797_.nextDouble();
         double d3 = (p_221797_.nextFloat() - 0.5) * 0.5;
         double d4 = (p_221797_.nextFloat() - 0.5) * 0.5;
         double d5 = (p_221797_.nextFloat() - 0.5) * 0.5;
         int j = p_221797_.nextInt(2) * 2 - 1;
         if (!p_221795_.getBlockState(p_221796_.west()).is(this) && !p_221795_.getBlockState(p_221796_.east()).is(this)) {
            d0 = p_221796_.getX() + 0.5 + 0.25 * j;
            d3 = p_221797_.nextFloat() * 2.0F * j;
         } else {
            d2 = p_221796_.getZ() + 0.5 + 0.25 * j;
            d5 = p_221797_.nextFloat() * 2.0F * j;
         }

         p_221795_.addParticle(ParticleTypes.PORTAL, d0, d1, d2, d3, d4, d5);
      }
   }

   @Override
   protected ItemStack getCloneItemStack(LevelReader p_310044_, BlockPos p_54912_, BlockState p_54913_, boolean p_376456_) {
      return ItemStack.EMPTY;
   }

   @Override
   protected BlockState rotate(BlockState p_54925_, Rotation p_54926_) {
      switch (p_54926_) {
         case COUNTERCLOCKWISE_90:
         case CLOCKWISE_90:
            switch ((Direction.Axis)p_54925_.getValue(AXIS)) {
               case X:
                  return p_54925_.setValue(AXIS, Direction.Axis.Z);
               case Z:
                  return p_54925_.setValue(AXIS, Direction.Axis.X);
               default:
                  return p_54925_;
            }
         default:
            return p_54925_;
      }
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_54935_) {
      p_54935_.add(AXIS);
   }
}
