package net.minecraft.world.level;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ClipContext {
   private final Vec3 from;
   private final Vec3 to;
   private final ClipContext.Block block;
   private final ClipContext.Fluid fluid;
   private final CollisionContext collisionContext;

   public ClipContext(Vec3 p_45688_, Vec3 p_45689_, ClipContext.Block p_45690_, ClipContext.Fluid p_45691_, Entity p_45692_) {
      this(p_45688_, p_45689_, p_45690_, p_45691_, CollisionContext.of(p_45692_));
   }

   public ClipContext(Vec3 p_312751_, Vec3 p_311517_, ClipContext.Block p_311464_, ClipContext.Fluid p_311910_, CollisionContext p_310522_) {
      this.from = p_312751_;
      this.to = p_311517_;
      this.block = p_311464_;
      this.fluid = p_311910_;
      this.collisionContext = p_310522_;
   }

   public Vec3 getTo() {
      return this.to;
   }

   public Vec3 getFrom() {
      return this.from;
   }

   public VoxelShape getBlockShape(BlockState p_45695_, BlockGetter p_45696_, BlockPos p_45697_) {
      return this.block.get(p_45695_, p_45696_, p_45697_, this.collisionContext);
   }

   public VoxelShape getFluidShape(FluidState p_45699_, BlockGetter p_45700_, BlockPos p_45701_) {
      return this.fluid.canPick(p_45699_) ? p_45699_.getShape(p_45700_, p_45701_) : Shapes.empty();
   }

   public enum Block implements ClipContext.ShapeGetter {
      COLLIDER(BlockBehaviour.BlockStateBase::getCollisionShape),
      OUTLINE(BlockBehaviour.BlockStateBase::getShape),
      VISUAL(BlockBehaviour.BlockStateBase::getVisualShape),
      FALLDAMAGE_RESETTING(
         (p_422021_, p_422022_, p_422023_, p_422024_) -> {
            if (p_422021_.is(BlockTags.FALL_DAMAGE_RESETTING)) {
               return Shapes.block();
            }

            if (p_422024_ instanceof EntityCollisionContext entitycollisioncontext
               && entitycollisioncontext.getEntity() != null
               && entitycollisioncontext.getEntity().getType() == EntityType.PLAYER) {
               if (p_422021_.is(Blocks.END_GATEWAY) || p_422021_.is(Blocks.END_PORTAL)) {
                  return Shapes.block();
               }

               if (p_422022_ instanceof ServerLevel serverlevel
                  && p_422021_.is(Blocks.NETHER_PORTAL)
                  && serverlevel.getGameRules().get(GameRules.PLAYERS_NETHER_PORTAL_DEFAULT_DELAY) == 0) {
                  return Shapes.block();
               }
            }

            return Shapes.empty();
         }
      );

      private final ClipContext.ShapeGetter shapeGetter;

      Block(final ClipContext.ShapeGetter p_45712_) {
         this.shapeGetter = p_45712_;
      }

      @Override
      public VoxelShape get(BlockState p_45714_, BlockGetter p_45715_, BlockPos p_45716_, CollisionContext p_45717_) {
         return this.shapeGetter.get(p_45714_, p_45715_, p_45716_, p_45717_);
      }
   }

   public enum Fluid {
      NONE(p_45736_ -> false),
      SOURCE_ONLY(FluidState::isSource),
      ANY(p_45734_ -> !p_45734_.isEmpty()),
      WATER(p_201988_ -> p_201988_.is(FluidTags.WATER));

      private final Predicate<FluidState> canPick;

      Fluid(final Predicate<FluidState> p_45730_) {
         this.canPick = p_45730_;
      }

      public boolean canPick(FluidState p_45732_) {
         return this.canPick.test(p_45732_);
      }
   }

   public interface ShapeGetter {
      VoxelShape get(BlockState var1, BlockGetter var2, BlockPos var3, CollisionContext var4);
   }
}
