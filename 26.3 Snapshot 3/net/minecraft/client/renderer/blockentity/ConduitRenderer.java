package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Transformation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SpriteMapper;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.ConduitRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public class ConduitRenderer implements BlockEntityRenderer<ConduitBlockEntity, ConduitRenderState> {
   public static final Transformation DEFAULT_TRANSFORMATION = new Transformation(new Vector3f(0.5F, 0.5F, 0.5F), null, null, null);
   public static final SpriteMapper MAPPER = new SpriteMapper(TextureAtlas.LOCATION_BLOCKS, "entity/conduit");
   public static final SpriteId SHELL_TEXTURE = MAPPER.defaultNamespaceApply("base");
   public static final SpriteId ACTIVE_SHELL_TEXTURE = MAPPER.defaultNamespaceApply("cage");
   public static final SpriteId WIND_TEXTURE = MAPPER.defaultNamespaceApply("wind");
   public static final SpriteId VERTICAL_WIND_TEXTURE = MAPPER.defaultNamespaceApply("wind_vertical");
   public static final SpriteId OPEN_EYE_TEXTURE = MAPPER.defaultNamespaceApply("open_eye");
   public static final SpriteId CLOSED_EYE_TEXTURE = MAPPER.defaultNamespaceApply("closed_eye");
   private final SpriteGetter sprites;
   private final ModelPart eye;
   private final ModelPart wind;
   private final ModelPart shell;
   private final ModelPart cage;

   public ConduitRenderer(final BlockEntityRendererProvider.Context context) {
      this.sprites = context.sprites();
      this.eye = context.bakeLayer(ModelLayers.CONDUIT_EYE);
      this.wind = context.bakeLayer(ModelLayers.CONDUIT_WIND);
      this.shell = context.bakeLayer(ModelLayers.CONDUIT_SHELL);
      this.cage = context.bakeLayer(ModelLayers.CONDUIT_CAGE);
   }

   public static LayerDefinition createEyeLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      root.addOrReplaceChild(
         "eye", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 0.0F, new CubeDeformation(0.01F)), PartPose.ZERO
      );
      return LayerDefinition.create(mesh, 16, 16);
   }

   public static LayerDefinition createWindLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      root.addOrReplaceChild("wind", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -8.0F, -8.0F, 16.0F, 16.0F, 16.0F), PartPose.ZERO);
      return LayerDefinition.create(mesh, 64, 32);
   }

   public static LayerDefinition createShellLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      root.addOrReplaceChild("shell", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F), PartPose.ZERO);
      return LayerDefinition.create(mesh, 32, 16);
   }

   public static LayerDefinition createCageLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      root.addOrReplaceChild("shell", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO);
      return LayerDefinition.create(mesh, 32, 16);
   }

   public ConduitRenderState createRenderState() {
      return new ConduitRenderState();
   }

   public void extractRenderState(
      final ConduitBlockEntity blockEntity,
      final ConduitRenderState state,
      final float partialTicks,
      final Vec3 cameraPosition,
      final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
   ) {
      BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
      state.isActive = blockEntity.isActive();
      state.activeRotation = blockEntity.getActiveRotation(blockEntity.isActive() ? partialTicks : 0.0F);
      state.animTime = blockEntity.tickCount + partialTicks;
      state.animationPhase = blockEntity.tickCount / 66 % 3;
      state.isHunting = blockEntity.isHunting();
   }

   public void submit(final ConduitRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera) {
      if (!state.isActive) {
         poseStack.pushPose();
         poseStack.translate(0.5F, 0.5F, 0.5F);
         poseStack.mulPose(new Quaternionf().rotationY(state.activeRotation * (float) (Math.PI / 180.0)));
         RenderType shellRenderType = SHELL_TEXTURE.renderType(RenderTypes::entitySolid);
         submitNodeCollector.submitModelPart(
            this.shell, poseStack, shellRenderType, state.lightCoords, OverlayTexture.NO_OVERLAY, this.sprites.get(SHELL_TEXTURE), -1
         );
         if (state.breakProgress != null) {
            submitNodeCollector.order(1)
               .submitCrumblingOverlay(this.shell, poseStack, shellRenderType, state.lightCoords, OverlayTexture.NO_OVERLAY, -1, state.breakProgress);
         }

         poseStack.popPose();
      } else {
         float rotation = state.activeRotation * (180.0F / (float)Math.PI);
         float hh = Mth.sin(state.animTime * 0.1F) / 2.0F + 0.5F;
         hh = hh * hh + hh;
         poseStack.pushPose();
         poseStack.translate(0.5F, 0.3F + hh * 0.2F, 0.5F);
         Vector3f axis = new Vector3f(0.5F, 1.0F, 0.5F).normalize();
         poseStack.mulPose(new Quaternionf().rotationAxis(rotation * (float) (Math.PI / 180.0), axis));
         RenderType cageRenderType = ACTIVE_SHELL_TEXTURE.renderType(RenderTypes::entityCutout);
         submitNodeCollector.submitModelPart(
            this.cage, poseStack, cageRenderType, state.lightCoords, OverlayTexture.NO_OVERLAY, this.sprites.get(ACTIVE_SHELL_TEXTURE), -1
         );
         if (state.breakProgress != null) {
            submitNodeCollector.order(1)
               .submitCrumblingOverlay(this.cage, poseStack, cageRenderType, state.lightCoords, OverlayTexture.NO_OVERLAY, -1, state.breakProgress);
         }

         poseStack.popPose();
         poseStack.pushPose();
         poseStack.translate(0.5F, 0.5F, 0.5F);
         if (state.animationPhase == 1) {
            poseStack.mulPose(new Quaternionf().rotationX((float) (Math.PI / 2)));
         } else if (state.animationPhase == 2) {
            poseStack.mulPose(new Quaternionf().rotationZ((float) (Math.PI / 2)));
         }

         SpriteId windSpriteId = state.animationPhase == 1 ? VERTICAL_WIND_TEXTURE : WIND_TEXTURE;
         RenderType windRenderType = windSpriteId.renderType(RenderTypes::entityCutout);
         TextureAtlasSprite windSprite = this.sprites.get(windSpriteId);
         submitNodeCollector.submitModelPart(this.wind, poseStack, windRenderType, state.lightCoords, OverlayTexture.NO_OVERLAY, windSprite);
         poseStack.popPose();
         poseStack.pushPose();
         poseStack.translate(0.5F, 0.5F, 0.5F);
         poseStack.scale(0.875F, 0.875F, 0.875F);
         poseStack.mulPose(new Quaternionf().rotationXYZ((float) Math.PI, 0.0F, (float) Math.PI));
         submitNodeCollector.submitModelPart(this.wind, poseStack, windRenderType, state.lightCoords, OverlayTexture.NO_OVERLAY, windSprite);
         poseStack.popPose();
         poseStack.pushPose();
         poseStack.translate(0.5F, 0.3F + hh * 0.2F, 0.5F);
         poseStack.scale(0.5F, 0.5F, 0.5F);
         poseStack.mulPose(camera.orientation);
         poseStack.mulPose(new Quaternionf().rotationZ((float) Math.PI).rotateY((float) Math.PI));
         float scale = 1.3333334F;
         poseStack.scale(1.3333334F, 1.3333334F, 1.3333334F);
         SpriteId eyeSprite = state.isHunting ? OPEN_EYE_TEXTURE : CLOSED_EYE_TEXTURE;
         submitNodeCollector.submitModelPart(
            this.eye, poseStack, eyeSprite.renderType(RenderTypes::entityCutout), state.lightCoords, OverlayTexture.NO_OVERLAY, this.sprites.get(eyeSprite)
         );
         poseStack.popPose();
      }
   }
}
