package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Objects;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.object.cart.MinecartModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.MinecartRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import net.minecraft.world.entity.vehicle.minecart.OldMinecartBehavior;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractMinecartRenderer<T extends AbstractMinecart, S extends MinecartRenderState> extends EntityRenderer<T, S> {
   private static final Identifier MINECART_LOCATION = Identifier.withDefaultNamespace("textures/entity/minecart.png");
   private static final float DISPLAY_BLOCK_SCALE = 0.75F;
   protected final MinecartModel model;

   public AbstractMinecartRenderer(EntityRendererProvider.Context p_369922_, ModelLayerLocation p_364230_) {
      super(p_369922_);
      this.shadowRadius = 0.7F;
      this.model = new MinecartModel(p_369922_.bakeLayer(p_364230_));
   }

   public void submit(S p_427147_, PoseStack p_431709_, SubmitNodeCollector p_425420_, CameraRenderState p_431192_) {
      super.submit(p_427147_, p_431709_, p_425420_, p_431192_);
      p_431709_.pushPose();
      long i = p_427147_.offsetSeed;
      float f = (((float)(i >> 16 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
      float f1 = (((float)(i >> 20 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
      float f2 = (((float)(i >> 24 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
      p_431709_.translate(f, f1, f2);
      if (p_427147_.isNewRender) {
         newRender(p_427147_, p_431709_);
      } else {
         oldRender(p_427147_, p_431709_);
      }

      float f3 = p_427147_.hurtTime;
      if (f3 > 0.0F) {
         p_431709_.mulPose(Axis.XP.rotationDegrees(Mth.sin(f3) * f3 * p_427147_.damageTime / 10.0F * p_427147_.hurtDir));
      }

      BlockState blockstate = p_427147_.displayBlockState;
      if (blockstate.getRenderShape() != RenderShape.INVISIBLE) {
         p_431709_.pushPose();
         p_431709_.scale(0.75F, 0.75F, 0.75F);
         p_431709_.translate(-0.5F, (p_427147_.displayOffset - 8) / 16.0F, 0.5F);
         p_431709_.mulPose(Axis.YP.rotationDegrees(90.0F));
         this.submitMinecartContents(p_427147_, blockstate, p_431709_, p_425420_, p_427147_.lightCoords);
         p_431709_.popPose();
      }

      p_431709_.scale(-1.0F, -1.0F, 1.0F);
      p_425420_.submitModel(
         this.model,
         p_427147_,
         p_431709_,
         this.model.renderType(MINECART_LOCATION),
         p_427147_.lightCoords,
         OverlayTexture.NO_OVERLAY,
         p_427147_.outlineColor,
         null
      );
      p_431709_.popPose();
   }

   private static <S extends MinecartRenderState> void newRender(S p_369039_, PoseStack p_366808_) {
      p_366808_.mulPose(Axis.YP.rotationDegrees(p_369039_.yRot));
      p_366808_.mulPose(Axis.ZP.rotationDegrees(-p_369039_.xRot));
      p_366808_.translate(0.0F, 0.375F, 0.0F);
   }

   private static <S extends MinecartRenderState> void oldRender(S p_364306_, PoseStack p_367729_) {
      double d0 = p_364306_.x;
      double d1 = p_364306_.y;
      double d2 = p_364306_.z;
      float f = p_364306_.xRot;
      float f1 = p_364306_.yRot;
      if (p_364306_.posOnRail != null && p_364306_.frontPos != null && p_364306_.backPos != null) {
         Vec3 vec3 = p_364306_.frontPos;
         Vec3 vec31 = p_364306_.backPos;
         p_367729_.translate(p_364306_.posOnRail.x - d0, (vec3.y + vec31.y) / 2.0 - d1, p_364306_.posOnRail.z - d2);
         Vec3 vec32 = vec31.add(-vec3.x, -vec3.y, -vec3.z);
         if (vec32.length() != 0.0) {
            vec32 = vec32.normalize();
            f1 = (float)(Math.atan2(vec32.z, vec32.x) * 180.0 / Math.PI);
            f = (float)(Math.atan(vec32.y) * 73.0);
         }
      }

      p_367729_.translate(0.0F, 0.375F, 0.0F);
      p_367729_.mulPose(Axis.YP.rotationDegrees(180.0F - f1));
      p_367729_.mulPose(Axis.ZP.rotationDegrees(-f));
   }

   public void extractRenderState(T p_455270_, S p_364445_, float p_364174_) {
      super.extractRenderState(p_455270_, p_364445_, p_364174_);
      if (p_455270_.getBehavior() instanceof NewMinecartBehavior newminecartbehavior) {
         newExtractState(p_455270_, newminecartbehavior, p_364445_, p_364174_);
         p_364445_.isNewRender = true;
      } else if (p_455270_.getBehavior() instanceof OldMinecartBehavior oldminecartbehavior) {
         oldExtractState(p_455270_, oldminecartbehavior, p_364445_, p_364174_);
         p_364445_.isNewRender = false;
      }

      long i = p_455270_.getId() * 493286711L;
      p_364445_.offsetSeed = i * i * 4392167121L + i * 98761L;
      p_364445_.hurtTime = p_455270_.getHurtTime() - p_364174_;
      p_364445_.hurtDir = p_455270_.getHurtDir();
      p_364445_.damageTime = Math.max(p_455270_.getDamage() - p_364174_, 0.0F);
      p_364445_.displayOffset = p_455270_.getDisplayOffset();
      p_364445_.displayBlockState = p_455270_.getDisplayBlockState();
   }

   private static <T extends AbstractMinecart, S extends MinecartRenderState> void newExtractState(
      T p_453465_, NewMinecartBehavior p_456461_, S p_367623_, float p_365529_
   ) {
      if (p_456461_.cartHasPosRotLerp()) {
         p_367623_.renderPos = p_456461_.getCartLerpPosition(p_365529_);
         p_367623_.xRot = p_456461_.getCartLerpXRot(p_365529_);
         p_367623_.yRot = p_456461_.getCartLerpYRot(p_365529_);
      } else {
         p_367623_.renderPos = null;
         p_367623_.xRot = p_453465_.getXRot();
         p_367623_.yRot = p_453465_.getYRot();
      }
   }

   private static <T extends AbstractMinecart, S extends MinecartRenderState> void oldExtractState(
      T p_461054_, OldMinecartBehavior p_457844_, S p_368073_, float p_362159_
   ) {
      float f = 0.3F;
      p_368073_.xRot = p_461054_.getXRot(p_362159_);
      p_368073_.yRot = p_461054_.getYRot(p_362159_);
      double d0 = p_368073_.x;
      double d1 = p_368073_.y;
      double d2 = p_368073_.z;
      Vec3 vec3 = p_457844_.getPos(d0, d1, d2);
      if (vec3 != null) {
         p_368073_.posOnRail = vec3;
         Vec3 vec31 = p_457844_.getPosOffs(d0, d1, d2, 0.3F);
         Vec3 vec32 = p_457844_.getPosOffs(d0, d1, d2, -0.3F);
         p_368073_.frontPos = Objects.requireNonNullElse(vec31, vec3);
         p_368073_.backPos = Objects.requireNonNullElse(vec32, vec3);
      } else {
         p_368073_.posOnRail = null;
         p_368073_.frontPos = null;
         p_368073_.backPos = null;
      }
   }

   protected void submitMinecartContents(S p_424935_, BlockState p_425890_, PoseStack p_423302_, SubmitNodeCollector p_431110_, int p_429487_) {
      p_431110_.submitBlock(p_423302_, p_425890_, p_429487_, OverlayTexture.NO_OVERLAY, p_424935_.outlineColor);
   }

   protected AABB getBoundingBoxForCulling(T p_450738_) {
      AABB aabb = super.getBoundingBoxForCulling(p_450738_);
      return !p_450738_.getDisplayBlockState().isAir() ? aabb.expandTowards(0.0, p_450738_.getDisplayOffset() * 0.75F / 16.0F, 0.0) : aabb;
   }

   public Vec3 getRenderOffset(S p_367749_) {
      Vec3 vec3 = super.getRenderOffset(p_367749_);
      return p_367749_.isNewRender && p_367749_.renderPos != null
         ? vec3.add(p_367749_.renderPos.x - p_367749_.x, p_367749_.renderPos.y - p_367749_.y, p_367749_.renderPos.z - p_367749_.z)
         : vec3;
   }
}
