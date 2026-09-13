package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class EndGatewayBlock extends BaseEntityBlock implements Portal {
   public static final MapCodec<EndGatewayBlock> CODEC = simpleCodec(EndGatewayBlock::new);

   @Override
   public MapCodec<EndGatewayBlock> codec() {
      return CODEC;
   }

   protected EndGatewayBlock(BlockBehaviour.Properties p_52999_) {
      super(p_52999_);
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos p_153193_, BlockState p_153194_) {
      return new TheEndGatewayBlockEntity(p_153193_, p_153194_);
   }

   @Override
   public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level p_153189_, BlockState p_153190_, BlockEntityType<T> p_153191_) {
      return createTickerHelper(
         p_153191_, BlockEntityType.END_GATEWAY, p_153189_.isClientSide() ? TheEndGatewayBlockEntity::beamAnimationTick : TheEndGatewayBlockEntity::portalTick
      );
   }

   @Override
   public void animateTick(BlockState p_221097_, Level p_221098_, BlockPos p_221099_, RandomSource p_221100_) {
      BlockEntity blockentity = p_221098_.getBlockEntity(p_221099_);
      if (blockentity instanceof TheEndGatewayBlockEntity) {
         int i = ((TheEndGatewayBlockEntity)blockentity).getParticleAmount();

         for (int j = 0; j < i; j++) {
            double d0 = p_221099_.getX() + p_221100_.nextDouble();
            double d1 = p_221099_.getY() + p_221100_.nextDouble();
            double d2 = p_221099_.getZ() + p_221100_.nextDouble();
            double d3 = (p_221100_.nextDouble() - 0.5) * 0.5;
            double d4 = (p_221100_.nextDouble() - 0.5) * 0.5;
            double d5 = (p_221100_.nextDouble() - 0.5) * 0.5;
            int k = p_221100_.nextInt(2) * 2 - 1;
            if (p_221100_.nextBoolean()) {
               d2 = p_221099_.getZ() + 0.5 + 0.25 * k;
               d5 = p_221100_.nextFloat() * 2.0F * k;
            } else {
               d0 = p_221099_.getX() + 0.5 + 0.25 * k;
               d3 = p_221100_.nextFloat() * 2.0F * k;
            }

            p_221098_.addParticle(ParticleTypes.PORTAL, d0, d1, d2, d3, d4, d5);
         }
      }
   }

   @Override
   protected ItemStack getCloneItemStack(LevelReader p_309482_, BlockPos p_53004_, BlockState p_53005_, boolean p_376210_) {
      return ItemStack.EMPTY;
   }

   @Override
   protected boolean canBeReplaced(BlockState p_53012_, Fluid p_53013_) {
      return false;
   }

   @Override
   protected void entityInside(
      BlockState p_344925_, Level p_342495_, BlockPos p_344985_, Entity p_342191_, InsideBlockEffectApplier p_392538_, boolean p_432028_
   ) {
      if (p_342191_.canUsePortal(false)
         && !p_342495_.isClientSide()
         && p_342495_.getBlockEntity(p_344985_) instanceof TheEndGatewayBlockEntity theendgatewayblockentity
         && !theendgatewayblockentity.isCoolingDown()) {
         p_342191_.setAsInsidePortal(this, p_344985_);
         TheEndGatewayBlockEntity.triggerCooldown(p_342495_, p_344985_, p_344925_, theendgatewayblockentity);
      }
   }

   @Override
   public @Nullable TeleportTransition getPortalDestination(ServerLevel p_343085_, Entity p_345224_, BlockPos p_342863_) {
      if (p_343085_.getBlockEntity(p_342863_) instanceof TheEndGatewayBlockEntity theendgatewayblockentity) {
         Vec3 vec3 = theendgatewayblockentity.getPortalPosition(p_343085_, p_342863_);
         if (vec3 == null) {
            return null;
         } else {
            return p_345224_ instanceof ThrownEnderpearl
               ? new TeleportTransition(p_343085_, vec3, Vec3.ZERO, 0.0F, 0.0F, Set.of(), TeleportTransition.PLACE_PORTAL_TICKET)
               : new TeleportTransition(
                  p_343085_, vec3, Vec3.ZERO, 0.0F, 0.0F, Relative.union(Relative.DELTA, Relative.ROTATION), TeleportTransition.PLACE_PORTAL_TICKET
               );
         }
      } else {
         return null;
      }
   }

   @Override
   protected RenderShape getRenderShape(BlockState p_375689_) {
      return RenderShape.INVISIBLE;
   }
}
