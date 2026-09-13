package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MaterialMapper;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.CondiutRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ConduitRenderer implements BlockEntityRenderer<ConduitBlockEntity, CondiutRenderState> {
   public static final MaterialMapper MAPPER = new MaterialMapper(TextureAtlas.LOCATION_BLOCKS, "entity/conduit");
   public static final Material SHELL_TEXTURE = MAPPER.defaultNamespaceApply("base");
   public static final Material ACTIVE_SHELL_TEXTURE = MAPPER.defaultNamespaceApply("cage");
   public static final Material WIND_TEXTURE = MAPPER.defaultNamespaceApply("wind");
   public static final Material VERTICAL_WIND_TEXTURE = MAPPER.defaultNamespaceApply("wind_vertical");
   public static final Material OPEN_EYE_TEXTURE = MAPPER.defaultNamespaceApply("open_eye");
   public static final Material CLOSED_EYE_TEXTURE = MAPPER.defaultNamespaceApply("closed_eye");
   private final MaterialSet materials;
   private final ModelPart eye;
   private final ModelPart wind;
   private final ModelPart shell;
   private final ModelPart cage;

   public ConduitRenderer(BlockEntityRendererProvider.Context p_173613_) {
      this.materials = p_173613_.materials();
      this.eye = p_173613_.bakeLayer(ModelLayers.CONDUIT_EYE);
      this.wind = p_173613_.bakeLayer(ModelLayers.CONDUIT_WIND);
      this.shell = p_173613_.bakeLayer(ModelLayers.CONDUIT_SHELL);
      this.cage = p_173613_.bakeLayer(ModelLayers.CONDUIT_CAGE);
   }

   public static LayerDefinition createEyeLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      partdefinition.addOrReplaceChild(
         "eye", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 0.0F, new CubeDeformation(0.01F)), PartPose.ZERO
      );
      return LayerDefinition.create(meshdefinition, 16, 16);
   }

   public static LayerDefinition createWindLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      partdefinition.addOrReplaceChild("wind", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -8.0F, -8.0F, 16.0F, 16.0F, 16.0F), PartPose.ZERO);
      return LayerDefinition.create(meshdefinition, 64, 32);
   }

   public static LayerDefinition createShellLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      partdefinition.addOrReplaceChild("shell", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F), PartPose.ZERO);
      return LayerDefinition.create(meshdefinition, 32, 16);
   }

   public static LayerDefinition createCageLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      partdefinition.addOrReplaceChild("shell", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO);
      return LayerDefinition.create(meshdefinition, 32, 16);
   }

   public CondiutRenderState createRenderState() {
      return new CondiutRenderState();
   }

   public void extractRenderState(
      ConduitBlockEntity p_431527_, CondiutRenderState p_430149_, float p_423055_, Vec3 p_429121_, ModelFeatureRenderer.@Nullable CrumblingOverlay p_425754_
   ) {
      BlockEntityRenderer.super.extractRenderState(p_431527_, p_430149_, p_423055_, p_429121_, p_425754_);
      p_430149_.isActive = p_431527_.isActive();
      p_430149_.activeRotation = p_431527_.getActiveRotation(p_431527_.isActive() ? p_423055_ : 0.0F);
      p_430149_.animTime = p_431527_.tickCount + p_423055_;
      p_430149_.animationPhase = p_431527_.tickCount / 66 % 3;
      p_430149_.isHunting = p_431527_.isHunting();
   }

   public void submit(CondiutRenderState p_424144_, PoseStack p_422450_, SubmitNodeCollector p_422910_, CameraRenderState p_424749_) {
      if (!p_424144_.isActive) {
         p_422450_.pushPose();
         p_422450_.translate(0.5F, 0.5F, 0.5F);
         p_422450_.mulPose(new Quaternionf().rotationY(p_424144_.activeRotation * (float) (Math.PI / 180.0)));
         p_422910_.submitModelPart(
            this.shell,
            p_422450_,
            SHELL_TEXTURE.renderType(RenderTypes::entitySolid),
            p_424144_.lightCoords,
            OverlayTexture.NO_OVERLAY,
            this.materials.get(SHELL_TEXTURE),
            -1,
            p_424144_.breakProgress
         );
         p_422450_.popPose();
      } else {
         float f = p_424144_.activeRotation * (180.0F / (float)Math.PI);
         float f1 = Mth.sin(p_424144_.animTime * 0.1F) / 2.0F + 0.5F;
         f1 = f1 * f1 + f1;
         p_422450_.pushPose();
         p_422450_.translate(0.5F, 0.3F + f1 * 0.2F, 0.5F);
         Vector3f vector3f = new Vector3f(0.5F, 1.0F, 0.5F).normalize();
         p_422450_.mulPose(new Quaternionf().rotationAxis(f * (float) (Math.PI / 180.0), vector3f));
         p_422910_.submitModelPart(
            this.cage,
            p_422450_,
            ACTIVE_SHELL_TEXTURE.renderType(RenderTypes::entityCutoutNoCull),
            p_424144_.lightCoords,
            OverlayTexture.NO_OVERLAY,
            this.materials.get(ACTIVE_SHELL_TEXTURE),
            -1,
            p_424144_.breakProgress
         );
         p_422450_.popPose();
         p_422450_.pushPose();
         p_422450_.translate(0.5F, 0.5F, 0.5F);
         if (p_424144_.animationPhase == 1) {
            p_422450_.mulPose(new Quaternionf().rotationX((float) (Math.PI / 2)));
         } else if (p_424144_.animationPhase == 2) {
            p_422450_.mulPose(new Quaternionf().rotationZ((float) (Math.PI / 2)));
         }

         Material material = p_424144_.animationPhase == 1 ? VERTICAL_WIND_TEXTURE : WIND_TEXTURE;
         RenderType rendertype = material.renderType(RenderTypes::entityCutoutNoCull);
         TextureAtlasSprite textureatlassprite = this.materials.get(material);
         p_422910_.submitModelPart(this.wind, p_422450_, rendertype, p_424144_.lightCoords, OverlayTexture.NO_OVERLAY, textureatlassprite);
         p_422450_.popPose();
         p_422450_.pushPose();
         p_422450_.translate(0.5F, 0.5F, 0.5F);
         p_422450_.scale(0.875F, 0.875F, 0.875F);
         p_422450_.mulPose(new Quaternionf().rotationXYZ((float) Math.PI, 0.0F, (float) Math.PI));
         p_422910_.submitModelPart(this.wind, p_422450_, rendertype, p_424144_.lightCoords, OverlayTexture.NO_OVERLAY, textureatlassprite);
         p_422450_.popPose();
         p_422450_.pushPose();
         p_422450_.translate(0.5F, 0.3F + f1 * 0.2F, 0.5F);
         p_422450_.scale(0.5F, 0.5F, 0.5F);
         p_422450_.mulPose(p_424749_.orientation);
         p_422450_.mulPose(new Quaternionf().rotationZ((float) Math.PI).rotateY((float) Math.PI));
         float f2 = 1.3333334F;
         p_422450_.scale(1.3333334F, 1.3333334F, 1.3333334F);
         Material material1 = p_424144_.isHunting ? OPEN_EYE_TEXTURE : CLOSED_EYE_TEXTURE;
         p_422910_.submitModelPart(
            this.eye,
            p_422450_,
            material1.renderType(RenderTypes::entityCutoutNoCull),
            p_424144_.lightCoords,
            OverlayTexture.NO_OVERLAY,
            this.materials.get(material1)
         );
         p_422450_.popPose();
      }
   }
}
