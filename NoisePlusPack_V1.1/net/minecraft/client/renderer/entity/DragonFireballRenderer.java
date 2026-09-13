package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DragonFireballRenderer extends EntityRenderer<DragonFireball, EntityRenderState> {
   private static final Identifier TEXTURE_LOCATION = Identifier.withDefaultNamespace("textures/entity/enderdragon/dragon_fireball.png");
   private static final RenderType RENDER_TYPE = RenderTypes.entityCutoutNoCull(TEXTURE_LOCATION);

   public DragonFireballRenderer(EntityRendererProvider.Context p_173962_) {
      super(p_173962_);
   }

   protected int getBlockLightLevel(DragonFireball p_456158_, BlockPos p_114088_) {
      return 15;
   }

   @Override
   public void submit(EntityRenderState p_424665_, PoseStack p_431375_, SubmitNodeCollector p_422551_, CameraRenderState p_426749_) {
      p_431375_.pushPose();
      p_431375_.scale(2.0F, 2.0F, 2.0F);
      p_431375_.mulPose(p_426749_.orientation);
      p_422551_.submitCustomGeometry(p_431375_, RENDER_TYPE, (p_424360_, p_425160_) -> {
         vertex(p_425160_, p_424360_, p_424665_.lightCoords, 0.0F, 0, 0, 1);
         vertex(p_425160_, p_424360_, p_424665_.lightCoords, 1.0F, 0, 1, 1);
         vertex(p_425160_, p_424360_, p_424665_.lightCoords, 1.0F, 1, 1, 0);
         vertex(p_425160_, p_424360_, p_424665_.lightCoords, 0.0F, 1, 0, 0);
      });
      p_431375_.popPose();
      super.submit(p_424665_, p_431375_, p_422551_, p_426749_);
   }

   private static void vertex(VertexConsumer p_254095_, PoseStack.Pose p_336223_, int p_253829_, float p_253995_, int p_254031_, int p_253641_, int p_254243_) {
      p_254095_.addVertex(p_336223_, p_253995_ - 0.5F, p_254031_ - 0.25F, 0.0F)
         .setColor(-1)
         .setUv(p_253641_, p_254243_)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(p_253829_)
         .setNormal(p_336223_, 0.0F, 1.0F, 0.0F);
   }

   @Override
   public EntityRenderState createRenderState() {
      return new EntityRenderState();
   }
}
