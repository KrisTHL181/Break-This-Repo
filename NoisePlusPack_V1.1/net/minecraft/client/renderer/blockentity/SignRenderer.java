package net.minecraft.client.renderer.blockentity;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Map;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.util.Unit;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SignRenderer extends AbstractSignRenderer {
   public static final float RENDER_SCALE = 0.6666667F;
   private static final Vec3 TEXT_OFFSET = new Vec3(0.0, 0.33333334F, 0.046666667F);
   private final Map<WoodType, SignRenderer.Models> signModels;

   public SignRenderer(BlockEntityRendererProvider.Context p_173636_) {
      super(p_173636_);
      this.signModels = WoodType.values()
         .collect(
            ImmutableMap.toImmutableMap(
               p_173645_ -> p_173645_,
               p_420925_ -> new SignRenderer.Models(
                  createSignModel(p_173636_.entityModelSet(), p_420925_, true), createSignModel(p_173636_.entityModelSet(), p_420925_, false)
               )
            )
         );
   }

   @Override
   protected Model.Simple getSignModel(BlockState p_378677_, WoodType p_376798_) {
      SignRenderer.Models signrenderer$models = this.signModels.get(p_376798_);
      return p_378677_.getBlock() instanceof StandingSignBlock ? signrenderer$models.standing() : signrenderer$models.wall();
   }

   @Override
   protected Material getSignMaterial(WoodType p_251961_) {
      return Sheets.getSignMaterial(p_251961_);
   }

   @Override
   protected float getSignModelRenderScale() {
      return 0.6666667F;
   }

   @Override
   protected float getSignTextRenderScale() {
      return 0.6666667F;
   }

   private static void translateBase(PoseStack p_377935_, float p_376614_) {
      p_377935_.translate(0.5F, 0.5F, 0.5F);
      p_377935_.mulPose(Axis.YP.rotationDegrees(p_376614_));
   }

   @Override
   protected void translateSign(PoseStack p_278074_, float p_277875_, BlockState p_277559_) {
      translateBase(p_278074_, p_277875_);
      if (!(p_277559_.getBlock() instanceof StandingSignBlock)) {
         p_278074_.translate(0.0F, -0.3125F, -0.4375F);
      }
   }

   @Override
   protected Vec3 getTextOffset() {
      return TEXT_OFFSET;
   }

   public static void submitSpecial(
      MaterialSet p_424414_, PoseStack p_426493_, SubmitNodeCollector p_427728_, int p_427158_, int p_425383_, Model.Simple p_428118_, Material p_425954_
   ) {
      p_426493_.pushPose();
      applyInHandTransforms(p_426493_);
      p_427728_.submitModel(
         p_428118_, Unit.INSTANCE, p_426493_, p_425954_.renderType(p_428118_::renderType), p_427158_, p_425383_, -1, p_424414_.get(p_425954_), 0, null
      );
      p_426493_.popPose();
   }

   public static void applyInHandTransforms(PoseStack p_408914_) {
      translateBase(p_408914_, 0.0F);
      p_408914_.scale(0.6666667F, -0.6666667F, -0.6666667F);
   }

   public static Model.Simple createSignModel(EntityModelSet p_173647_, WoodType p_173648_, boolean p_364684_) {
      ModelLayerLocation modellayerlocation = p_364684_ ? ModelLayers.createStandingSignModelName(p_173648_) : ModelLayers.createWallSignModelName(p_173648_);
      return new Model.Simple(p_173647_.bakeLayer(modellayerlocation), RenderTypes::entityCutoutNoCull);
   }

   public static LayerDefinition createSignLayer(boolean p_368797_) {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      partdefinition.addOrReplaceChild("sign", CubeListBuilder.create().texOffs(0, 0).addBox(-12.0F, -14.0F, -1.0F, 24.0F, 12.0F, 2.0F), PartPose.ZERO);
      if (p_368797_) {
         partdefinition.addOrReplaceChild("stick", CubeListBuilder.create().texOffs(0, 14).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 14.0F, 2.0F), PartPose.ZERO);
      }

      return LayerDefinition.create(meshdefinition, 64, 32);
   }

   @OnlyIn(Dist.CLIENT)
   record Models(Model.Simple standing, Model.Simple wall) {
   }
}
