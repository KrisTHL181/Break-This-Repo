package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.ExperienceOrbRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ExperienceOrbRenderer extends EntityRenderer<ExperienceOrb, ExperienceOrbRenderState> {
   private static final Identifier EXPERIENCE_ORB_LOCATION = Identifier.withDefaultNamespace("textures/entity/experience_orb.png");
   private static final RenderType RENDER_TYPE = RenderTypes.itemEntityTranslucentCull(EXPERIENCE_ORB_LOCATION);

   public ExperienceOrbRenderer(EntityRendererProvider.Context p_174110_) {
      super(p_174110_);
      this.shadowRadius = 0.15F;
      this.shadowStrength = 0.75F;
   }

   protected int getBlockLightLevel(ExperienceOrb p_114606_, BlockPos p_114607_) {
      return Mth.clamp(super.getBlockLightLevel(p_114606_, p_114607_) + 7, 0, 15);
   }

   public void submit(ExperienceOrbRenderState p_430820_, PoseStack p_427445_, SubmitNodeCollector p_429592_, CameraRenderState p_429130_) {
      p_427445_.pushPose();
      int i = p_430820_.icon;
      float f = (i % 4 * 16 + 0) / 64.0F;
      float f1 = (i % 4 * 16 + 16) / 64.0F;
      float f2 = (i / 4 * 16 + 0) / 64.0F;
      float f3 = (i / 4 * 16 + 16) / 64.0F;
      float f4 = 1.0F;
      float f5 = 0.5F;
      float f6 = 0.25F;
      float f7 = 255.0F;
      float f8 = p_430820_.ageInTicks / 2.0F;
      int j = (int)((Mth.sin(f8 + 0.0F) + 1.0F) * 0.5F * 255.0F);
      int k = 255;
      int l = (int)((Mth.sin(f8 + (float) (Math.PI * 4.0 / 3.0)) + 1.0F) * 0.1F * 255.0F);
      p_427445_.translate(0.0F, 0.1F, 0.0F);
      p_427445_.mulPose(p_429130_.orientation);
      float f9 = 0.3F;
      p_427445_.scale(0.3F, 0.3F, 0.3F);
      p_429592_.submitCustomGeometry(p_427445_, RENDER_TYPE, (p_428876_, p_427493_) -> {
         vertex(p_427493_, p_428876_, -0.5F, -0.25F, j, 255, l, f, f3, p_430820_.lightCoords);
         vertex(p_427493_, p_428876_, 0.5F, -0.25F, j, 255, l, f1, f3, p_430820_.lightCoords);
         vertex(p_427493_, p_428876_, 0.5F, 0.75F, j, 255, l, f1, f2, p_430820_.lightCoords);
         vertex(p_427493_, p_428876_, -0.5F, 0.75F, j, 255, l, f, f2, p_430820_.lightCoords);
      });
      p_427445_.popPose();
      super.submit(p_430820_, p_427445_, p_429592_, p_429130_);
   }

   private static void vertex(
      VertexConsumer p_254515_,
      PoseStack.Pose p_333175_,
      float p_253952_,
      float p_254066_,
      int p_254283_,
      int p_254566_,
      int p_253882_,
      float p_254434_,
      float p_254223_,
      int p_254372_
   ) {
      p_254515_.addVertex(p_333175_, p_253952_, p_254066_, 0.0F)
         .setColor(p_254283_, p_254566_, p_253882_, 128)
         .setUv(p_254434_, p_254223_)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(p_254372_)
         .setNormal(p_333175_, 0.0F, 1.0F, 0.0F);
   }

   public ExperienceOrbRenderState createRenderState() {
      return new ExperienceOrbRenderState();
   }

   public void extractRenderState(ExperienceOrb p_363328_, ExperienceOrbRenderState p_365917_, float p_368286_) {
      super.extractRenderState(p_363328_, p_365917_, p_368286_);
      p_365917_.icon = p_363328_.getIcon();
   }
}
