package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.FishingHookRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FishingHookRenderer extends EntityRenderer<FishingHook, FishingHookRenderState> {
   private static final Identifier TEXTURE_LOCATION = Identifier.withDefaultNamespace("textures/entity/fishing_hook.png");
   private static final RenderType RENDER_TYPE = RenderTypes.entityCutout(TEXTURE_LOCATION);
   private static final double VIEW_BOBBING_SCALE = 960.0;

   public FishingHookRenderer(EntityRendererProvider.Context p_174117_) {
      super(p_174117_);
   }

   public boolean shouldRender(FishingHook p_364485_, Frustum p_366882_, double p_369405_, double p_366566_, double p_370201_) {
      return super.shouldRender(p_364485_, p_366882_, p_369405_, p_366566_, p_370201_) && p_364485_.getPlayerOwner() != null;
   }

   public void submit(FishingHookRenderState p_431626_, PoseStack p_422771_, SubmitNodeCollector p_427476_, CameraRenderState p_424754_) {
      p_422771_.pushPose();
      p_422771_.pushPose();
      p_422771_.scale(0.5F, 0.5F, 0.5F);
      p_422771_.mulPose(p_424754_.orientation);
      p_427476_.submitCustomGeometry(p_422771_, RENDER_TYPE, (p_431327_, p_428679_) -> {
         vertex(p_428679_, p_431327_, p_431626_.lightCoords, 0.0F, 0, 0, 1);
         vertex(p_428679_, p_431327_, p_431626_.lightCoords, 1.0F, 0, 1, 1);
         vertex(p_428679_, p_431327_, p_431626_.lightCoords, 1.0F, 1, 1, 0);
         vertex(p_428679_, p_431327_, p_431626_.lightCoords, 0.0F, 1, 0, 0);
      });
      p_422771_.popPose();
      float f = (float)p_431626_.lineOriginOffset.x;
      float f1 = (float)p_431626_.lineOriginOffset.y;
      float f2 = (float)p_431626_.lineOriginOffset.z;
      float f3 = Minecraft.getInstance().getWindow().getAppropriateLineWidth();
      p_427476_.submitCustomGeometry(p_422771_, RenderTypes.lines(), (p_448326_, p_448327_) -> {
         int i = 16;

         for (int j = 0; j < 16; j++) {
            float f4 = fraction(j, 16);
            float f5 = fraction(j + 1, 16);
            stringVertex(f, f1, f2, p_448327_, p_448326_, f4, f5, f3);
            stringVertex(f, f1, f2, p_448327_, p_448326_, f5, f4, f3);
         }
      });
      p_422771_.popPose();
      super.submit(p_431626_, p_422771_, p_427476_, p_424754_);
   }

   public static HumanoidArm getHoldingArm(Player p_377586_) {
      return p_377586_.getMainHandItem().getItem() instanceof FishingRodItem ? p_377586_.getMainArm() : p_377586_.getMainArm().getOpposite();
   }

   private Vec3 getPlayerHandPos(Player p_328037_, float p_328369_, float p_332926_) {
      int i = getHoldingArm(p_328037_) == HumanoidArm.RIGHT ? 1 : -1;
      if (this.entityRenderDispatcher.options.getCameraType().isFirstPerson() && p_328037_ == Minecraft.getInstance().player) {
         double d4 = 960.0 / this.entityRenderDispatcher.options.fov().get().intValue();
         Vec3 vec3 = this.entityRenderDispatcher
            .camera
            .getNearPlane()
            .getPointOnPlane(i * 0.525F, -0.1F)
            .scale(d4)
            .yRot(p_328369_ * 0.5F)
            .xRot(-p_328369_ * 0.7F);
         return p_328037_.getEyePosition(p_332926_).add(vec3);
      } else {
         float f = Mth.lerp(p_332926_, p_328037_.yBodyRotO, p_328037_.yBodyRot) * (float) (Math.PI / 180.0);
         double d0 = Mth.sin(f);
         double d1 = Mth.cos(f);
         float f1 = p_328037_.getScale();
         double d2 = i * 0.35 * f1;
         double d3 = 0.8 * f1;
         float f2 = p_328037_.isCrouching() ? -0.1875F : 0.0F;
         return p_328037_.getEyePosition(p_332926_).add(-d1 * d2 - d0 * d3, f2 - 0.45 * f1, -d0 * d2 + d1 * d3);
      }
   }

   private static float fraction(int p_114691_, int p_114692_) {
      return (float)p_114691_ / p_114692_;
   }

   private static void vertex(VertexConsumer p_254464_, PoseStack.Pose p_328848_, int p_254296_, float p_253632_, int p_254132_, int p_254171_, int p_254026_) {
      p_254464_.addVertex(p_328848_, p_253632_ - 0.5F, p_254132_ - 0.5F, 0.0F)
         .setColor(-1)
         .setUv(p_254171_, p_254026_)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(p_254296_)
         .setNormal(p_328848_, 0.0F, 1.0F, 0.0F);
   }

   private static void stringVertex(
      float p_174119_, float p_174120_, float p_174121_, VertexConsumer p_174122_, PoseStack.Pose p_174123_, float p_174124_, float p_174125_, float p_460475_
   ) {
      float f = p_174119_ * p_174124_;
      float f1 = p_174120_ * (p_174124_ * p_174124_ + p_174124_) * 0.5F + 0.25F;
      float f2 = p_174121_ * p_174124_;
      float f3 = p_174119_ * p_174125_ - f;
      float f4 = p_174120_ * (p_174125_ * p_174125_ + p_174125_) * 0.5F + 0.25F - f1;
      float f5 = p_174121_ * p_174125_ - f2;
      float f6 = Mth.sqrt(f3 * f3 + f4 * f4 + f5 * f5);
      f3 /= f6;
      f4 /= f6;
      f5 /= f6;
      p_174122_.addVertex(p_174123_, f, f1, f2).setColor(-16777216).setNormal(p_174123_, f3, f4, f5).setLineWidth(p_460475_);
   }

   public FishingHookRenderState createRenderState() {
      return new FishingHookRenderState();
   }

   public void extractRenderState(FishingHook p_363636_, FishingHookRenderState p_369118_, float p_368947_) {
      super.extractRenderState(p_363636_, p_369118_, p_368947_);
      Player player = p_363636_.getPlayerOwner();
      if (player == null) {
         p_369118_.lineOriginOffset = Vec3.ZERO;
      } else {
         float f = player.getAttackAnim(p_368947_);
         float f1 = Mth.sin(Mth.sqrt(f) * (float) Math.PI);
         Vec3 vec3 = this.getPlayerHandPos(player, f1, p_368947_);
         Vec3 vec31 = p_363636_.getPosition(p_368947_).add(0.0, 0.25, 0.0);
         p_369118_.lineOriginOffset = vec3.subtract(vec31);
      }
   }

   protected boolean affectedByCulling(FishingHook p_361671_) {
      return false;
   }
}
