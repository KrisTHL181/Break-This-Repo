package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.FireworkRocketRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FireworkEntityRenderer extends EntityRenderer<FireworkRocketEntity, FireworkRocketRenderState> {
   private final ItemModelResolver itemModelResolver;

   public FireworkEntityRenderer(EntityRendererProvider.Context p_174114_) {
      super(p_174114_);
      this.itemModelResolver = p_174114_.getItemModelResolver();
   }

   public void submit(FireworkRocketRenderState p_431208_, PoseStack p_424870_, SubmitNodeCollector p_430982_, CameraRenderState p_431027_) {
      p_424870_.pushPose();
      p_424870_.mulPose(p_431027_.orientation);
      if (p_431208_.isShotAtAngle) {
         p_424870_.mulPose(Axis.ZP.rotationDegrees(180.0F));
         p_424870_.mulPose(Axis.YP.rotationDegrees(180.0F));
         p_424870_.mulPose(Axis.XP.rotationDegrees(90.0F));
      }

      p_431208_.item.submit(p_424870_, p_430982_, p_431208_.lightCoords, OverlayTexture.NO_OVERLAY, p_431208_.outlineColor);
      p_424870_.popPose();
      super.submit(p_431208_, p_424870_, p_430982_, p_431027_);
   }

   public FireworkRocketRenderState createRenderState() {
      return new FireworkRocketRenderState();
   }

   public void extractRenderState(FireworkRocketEntity p_362725_, FireworkRocketRenderState p_362243_, float p_362924_) {
      super.extractRenderState(p_362725_, p_362243_, p_362924_);
      p_362243_.isShotAtAngle = p_362725_.isShotAtAngle();
      this.itemModelResolver.updateForNonLiving(p_362243_.item, p_362725_.getItem(), ItemDisplayContext.GROUND, p_362725_);
   }
}
