package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Consumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.ShulkerBoxRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ShulkerBoxRenderer implements BlockEntityRenderer<ShulkerBoxBlockEntity, ShulkerBoxRenderState> {
   private final MaterialSet materials;
   private final ShulkerBoxRenderer.ShulkerBoxModel model;

   public ShulkerBoxRenderer(BlockEntityRendererProvider.Context p_173626_) {
      this(p_173626_.entityModelSet(), p_173626_.materials());
   }

   public ShulkerBoxRenderer(SpecialModelRenderer.BakingContext p_431042_) {
      this(p_431042_.entityModelSet(), p_431042_.materials());
   }

   public ShulkerBoxRenderer(EntityModelSet p_376600_, MaterialSet p_423394_) {
      this.materials = p_423394_;
      this.model = new ShulkerBoxRenderer.ShulkerBoxModel(p_376600_.bakeLayer(ModelLayers.SHULKER_BOX));
   }

   public ShulkerBoxRenderState createRenderState() {
      return new ShulkerBoxRenderState();
   }

   public void extractRenderState(
      ShulkerBoxBlockEntity p_431300_,
      ShulkerBoxRenderState p_430156_,
      float p_428608_,
      Vec3 p_428519_,
      ModelFeatureRenderer.@Nullable CrumblingOverlay p_427339_
   ) {
      BlockEntityRenderer.super.extractRenderState(p_431300_, p_430156_, p_428608_, p_428519_, p_427339_);
      p_430156_.direction = p_431300_.getBlockState().getValueOrElse(ShulkerBoxBlock.FACING, Direction.UP);
      p_430156_.color = p_431300_.getColor();
      p_430156_.progress = p_431300_.getProgress(p_428608_);
   }

   public void submit(ShulkerBoxRenderState p_431388_, PoseStack p_424095_, SubmitNodeCollector p_426300_, CameraRenderState p_431212_) {
      DyeColor dyecolor = p_431388_.color;
      Material material;
      if (dyecolor == null) {
         material = Sheets.DEFAULT_SHULKER_TEXTURE_LOCATION;
      } else {
         material = Sheets.getShulkerBoxMaterial(dyecolor);
      }

      this.submit(
         p_424095_, p_426300_, p_431388_.lightCoords, OverlayTexture.NO_OVERLAY, p_431388_.direction, p_431388_.progress, p_431388_.breakProgress, material, 0
      );
   }

   public void submit(
      PoseStack p_431720_,
      SubmitNodeCollector p_425728_,
      int p_426517_,
      int p_423154_,
      Direction p_426689_,
      float p_422352_,
      ModelFeatureRenderer.@Nullable CrumblingOverlay p_428745_,
      Material p_429415_,
      int p_431891_
   ) {
      p_431720_.pushPose();
      this.prepareModel(p_431720_, p_426689_, p_422352_);
      p_425728_.submitModel(
         this.model,
         p_422352_,
         p_431720_,
         p_429415_.renderType(this.model::renderType),
         p_426517_,
         p_423154_,
         -1,
         this.materials.get(p_429415_),
         p_431891_,
         p_428745_
      );
      p_431720_.popPose();
   }

   private void prepareModel(PoseStack p_406885_, Direction p_410653_, float p_409643_) {
      p_406885_.translate(0.5F, 0.5F, 0.5F);
      float f = 0.9995F;
      p_406885_.scale(0.9995F, 0.9995F, 0.9995F);
      p_406885_.mulPose(p_410653_.getRotation());
      p_406885_.scale(1.0F, -1.0F, -1.0F);
      p_406885_.translate(0.0F, -1.0F, 0.0F);
      this.model.setupAnim(p_409643_);
   }

   public void getExtents(Direction p_407911_, float p_410036_, Consumer<Vector3fc> p_460516_) {
      PoseStack posestack = new PoseStack();
      this.prepareModel(posestack, p_407911_, p_410036_);
      this.model.root().getExtentsForGui(posestack, p_460516_);
   }

   @OnlyIn(Dist.CLIENT)
   static class ShulkerBoxModel extends Model<Float> {
      private final ModelPart lid;

      public ShulkerBoxModel(ModelPart p_366433_) {
         super(p_366433_, RenderTypes::entityCutoutNoCull);
         this.lid = p_366433_.getChild("lid");
      }

      public void setupAnim(Float p_429906_) {
         super.setupAnim(p_429906_);
         this.lid.setPos(0.0F, 24.0F - p_429906_ * 0.5F * 16.0F, 0.0F);
         this.lid.yRot = 270.0F * p_429906_ * (float) (Math.PI / 180.0);
      }
   }
}
