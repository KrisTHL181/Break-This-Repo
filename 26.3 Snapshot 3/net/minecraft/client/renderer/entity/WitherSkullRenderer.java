package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.object.skull.SkullModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.WitherSkullRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;

public class WitherSkullRenderer extends EntityRenderer<WitherSkull, WitherSkullRenderState> {
   private static final Identifier WITHER_INVULNERABLE_LOCATION = Identifier.withDefaultNamespace("textures/entity/wither/wither_invulnerable.png");
   private static final Identifier WITHER_LOCATION = Identifier.withDefaultNamespace("textures/entity/wither/wither.png");
   private final SkullModel model;

   public WitherSkullRenderer(final EntityRendererProvider.Context context) {
      super(context);
      this.model = new SkullModel(context.bakeLayer(ModelLayers.WITHER_SKULL));
   }

   public static LayerDefinition createSkullLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 35).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO);
      return LayerDefinition.create(mesh, 64, 64);
   }

   protected int getBlockLightLevel(final WitherSkull entity, final BlockPos blockPos) {
      return 15;
   }

   public void submit(
      final WitherSkullRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera
   ) {
      poseStack.pushPose();
      poseStack.scale(-1.0F, -1.0F, 1.0F);
      submitNodeCollector.submitModel(
         this.model, state.modelState, poseStack, this.getTextureLocation(state), state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor
      );
      poseStack.popPose();
      super.submit(state, poseStack, submitNodeCollector, camera);
   }

   private Identifier getTextureLocation(final WitherSkullRenderState state) {
      return state.isDangerous ? WITHER_INVULNERABLE_LOCATION : WITHER_LOCATION;
   }

   public WitherSkullRenderState createRenderState() {
      return new WitherSkullRenderState();
   }

   public void extractRenderState(final WitherSkull entity, final WitherSkullRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.isDangerous = entity.isDangerous();
      state.modelState.animationPos = 0.0F;
      state.modelState.yRot = entity.getYRot(partialTicks);
      state.modelState.xRot = entity.getXRot(partialTicks);
   }
}
