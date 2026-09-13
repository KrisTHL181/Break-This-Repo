package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.armorstand.ArmorStandArmorModel;
import net.minecraft.client.model.object.armorstand.ArmorStandModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ArmorStandRenderer extends LivingEntityRenderer<ArmorStand, ArmorStandRenderState, ArmorStandArmorModel> {
   public static final Identifier DEFAULT_SKIN_LOCATION = Identifier.withDefaultNamespace("textures/entity/armorstand/wood.png");
   private final ArmorStandArmorModel bigModel = this.getModel();
   private final ArmorStandArmorModel smallModel;

   public ArmorStandRenderer(EntityRendererProvider.Context p_173915_) {
      super(p_173915_, new ArmorStandModel(p_173915_.bakeLayer(ModelLayers.ARMOR_STAND)), 0.0F);
      this.smallModel = new ArmorStandModel(p_173915_.bakeLayer(ModelLayers.ARMOR_STAND_SMALL));
      this.addLayer(
         new HumanoidArmorLayer<>(
            this,
            ArmorModelSet.bake(ModelLayers.ARMOR_STAND_ARMOR, p_173915_.getModelSet(), ArmorStandArmorModel::new),
            ArmorModelSet.bake(ModelLayers.ARMOR_STAND_SMALL_ARMOR, p_173915_.getModelSet(), ArmorStandArmorModel::new),
            p_173915_.getEquipmentRenderer()
         )
      );
      this.addLayer(new ItemInHandLayer<>(this));
      this.addLayer(new WingsLayer<>(this, p_173915_.getModelSet(), p_173915_.getEquipmentRenderer()));
      this.addLayer(new CustomHeadLayer<>(this, p_173915_.getModelSet(), p_173915_.getPlayerSkinRenderCache()));
   }

   public Identifier getTextureLocation(ArmorStandRenderState p_361116_) {
      return DEFAULT_SKIN_LOCATION;
   }

   public ArmorStandRenderState createRenderState() {
      return new ArmorStandRenderState();
   }

   public void extractRenderState(ArmorStand p_364068_, ArmorStandRenderState p_361680_, float p_369387_) {
      super.extractRenderState(p_364068_, p_361680_, p_369387_);
      HumanoidMobRenderer.extractHumanoidRenderState(p_364068_, p_361680_, p_369387_, this.itemModelResolver);
      p_361680_.yRot = Mth.rotLerp(p_369387_, p_364068_.yRotO, p_364068_.getYRot());
      p_361680_.isMarker = p_364068_.isMarker();
      p_361680_.isSmall = p_364068_.isSmall();
      p_361680_.showArms = p_364068_.showArms();
      p_361680_.showBasePlate = p_364068_.showBasePlate();
      p_361680_.bodyPose = p_364068_.getBodyPose();
      p_361680_.headPose = p_364068_.getHeadPose();
      p_361680_.leftArmPose = p_364068_.getLeftArmPose();
      p_361680_.rightArmPose = p_364068_.getRightArmPose();
      p_361680_.leftLegPose = p_364068_.getLeftLegPose();
      p_361680_.rightLegPose = p_364068_.getRightLegPose();
      p_361680_.wiggle = (float)(p_364068_.level().getGameTime() - p_364068_.lastHit) + p_369387_;
   }

   public void submit(ArmorStandRenderState p_430115_, PoseStack p_430877_, SubmitNodeCollector p_431083_, CameraRenderState p_429593_) {
      this.model = p_430115_.isSmall ? this.smallModel : this.bigModel;
      super.submit(p_430115_, p_430877_, p_431083_, p_429593_);
   }

   protected void setupRotations(ArmorStandRenderState p_365303_, PoseStack p_113788_, float p_113789_, float p_113790_) {
      p_113788_.mulPose(Axis.YP.rotationDegrees(180.0F - p_113789_));
      if (p_365303_.wiggle < 5.0F) {
         p_113788_.mulPose(Axis.YP.rotationDegrees(Mth.sin(p_365303_.wiggle / 1.5F * (float) Math.PI) * 3.0F));
      }
   }

   protected boolean shouldShowName(ArmorStand p_363344_, double p_365520_) {
      return p_363344_.isCustomNameVisible();
   }

   protected @Nullable RenderType getRenderType(ArmorStandRenderState p_451197_, boolean p_113794_, boolean p_113795_, boolean p_113796_) {
      if (!p_451197_.isMarker) {
         return super.getRenderType(p_451197_, p_113794_, p_113795_, p_113796_);
      } else {
         Identifier identifier = this.getTextureLocation(p_451197_);
         if (p_113795_) {
            return RenderTypes.entityTranslucent(identifier, false);
         } else {
            return p_113794_ ? RenderTypes.entityCutoutNoCull(identifier, false) : null;
         }
      }
   }
}
