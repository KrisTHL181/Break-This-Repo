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
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.jspecify.annotations.Nullable;

public class ArmorStandRenderer extends LivingEntityRenderer<ArmorStand, ArmorStandRenderState, ArmorStandArmorModel> {
   public static final Identifier DEFAULT_SKIN_LOCATION = Identifier.withDefaultNamespace("textures/entity/armorstand/armorstand.png");
   private final ArmorStandArmorModel bigModel = this.getModel();
   private final ArmorStandArmorModel smallModel;

   public ArmorStandRenderer(final EntityRendererProvider.Context context) {
      super(context, new ArmorStandModel(context.bakeLayer(ModelLayers.ARMOR_STAND)), 0.0F);
      this.smallModel = new ArmorStandModel(context.bakeLayer(ModelLayers.ARMOR_STAND_SMALL));
      this.addLayer(
         new HumanoidArmorLayer<>(
            this,
            ArmorModelSet.bake(ModelLayers.ARMOR_STAND_ARMOR, context.getModelSet(), ArmorStandArmorModel::new),
            ArmorModelSet.bake(ModelLayers.ARMOR_STAND_SMALL_ARMOR, context.getModelSet(), ArmorStandArmorModel::new),
            context.getEquipmentRenderer()
         )
      );
      this.addLayer(new ItemInHandLayer<>(this));
      this.addLayer(new WingsLayer<>(this, context.getModelSet(), context.getEquipmentRenderer()));
      this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getPlayerSkinRenderCache()));
   }

   public Identifier getTextureLocation(final ArmorStandRenderState state) {
      return DEFAULT_SKIN_LOCATION;
   }

   public ArmorStandRenderState createRenderState() {
      return new ArmorStandRenderState();
   }

   public void extractRenderState(final ArmorStand entity, final ArmorStandRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      HumanoidMobRenderer.extractHumanoidRenderState(entity, state, partialTicks, this.itemModelResolver);
      state.yRot = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
      state.isMarker = entity.isMarker();
      state.isSmall = entity.isSmall();
      state.showArms = entity.showArms();
      state.showBasePlate = entity.showBasePlate();
      state.bodyPose = entity.getBodyPose();
      state.headPose = entity.getHeadPose();
      state.leftArmPose = entity.getLeftArmPose();
      state.rightArmPose = entity.getRightArmPose();
      state.leftLegPose = entity.getLeftLegPose();
      state.rightLegPose = entity.getRightLegPose();
      state.wiggle = (float)(entity.level().getGameTime() - entity.lastHit) + partialTicks;
   }

   public void submit(
      final ArmorStandRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera
   ) {
      this.model = state.isSmall ? this.smallModel : this.bigModel;
      super.submit(state, poseStack, submitNodeCollector, camera);
   }

   protected void setupRotations(final ArmorStandRenderState state, final PoseStack poseStack, final float bodyRot, final float entityScale) {
      poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyRot));
      if (state.wiggle < 5.0F) {
         poseStack.mulPose(Axis.YP.rotationDegrees(Mth.sin(state.wiggle / 1.5F * (float) Math.PI) * 3.0F));
      }
   }

   protected boolean shouldShowName(final ArmorStand entity, final double distanceToCameraSq) {
      return entity.isCustomNameVisible();
   }

   protected @Nullable RenderType getRenderType(
      final ArmorStandRenderState state, final boolean isBodyVisible, final boolean forceTransparent, final boolean appearGlowing
   ) {
      if (!state.isMarker) {
         return super.getRenderType(state, isBodyVisible, forceTransparent, appearGlowing);
      } else {
         Identifier texture = this.getTextureLocation(state);
         if (forceTransparent) {
            return RenderTypes.entityTranslucent(texture, false);
         } else {
            return isBodyVisible ? RenderTypes.entityCutout(texture, false) : null;
         }
      }
   }
}
