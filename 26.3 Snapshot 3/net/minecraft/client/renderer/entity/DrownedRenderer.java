package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.zombie.BabyDrownedModel;
import net.minecraft.client.model.monster.zombie.DrownedModel;
import net.minecraft.client.renderer.entity.layers.DrownedOuterLayer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class DrownedRenderer extends AbstractZombieRenderer<Drowned, ZombieRenderState, DrownedModel> {
   private static final Identifier DROWNED_LOCATION = Identifier.withDefaultNamespace("textures/entity/zombie/drowned.png");
   private static final Identifier BABY_DROWNED_LOCATION = Identifier.withDefaultNamespace("textures/entity/zombie/drowned_baby.png");

   public DrownedRenderer(final EntityRendererProvider.Context context) {
      super(
         context,
         new DrownedModel(context.bakeLayer(ModelLayers.DROWNED)),
         new BabyDrownedModel(context.bakeLayer(ModelLayers.DROWNED_BABY)),
         ArmorModelSet.bake(ModelLayers.DROWNED_ARMOR, context.getModelSet(), DrownedModel::new),
         ArmorModelSet.bake(ModelLayers.DROWNED_BABY_ARMOR, context.getModelSet(), BabyDrownedModel::new)
      );
      this.addLayer(new DrownedOuterLayer(this, context.getModelSet()));
   }

   public ZombieRenderState createRenderState() {
      return new ZombieRenderState();
   }

   @Override
   public Identifier getTextureLocation(final ZombieRenderState state) {
      return state.isBaby ? BABY_DROWNED_LOCATION : DROWNED_LOCATION;
   }

   protected void setupRotations(final ZombieRenderState state, final PoseStack poseStack, final float bodyRot, final float entityScale) {
      super.setupRotations(state, poseStack, bodyRot, entityScale);
      float swimAmount = state.swimAmount;
      if (swimAmount > 0.0F) {
         float targetRotationX = -10.0F - state.xRot;
         float rotationX = Mth.lerp(swimAmount, 0.0F, targetRotationX);
         poseStack.rotateAround(Axis.XP.rotationDegrees(rotationX), 0.0F, state.boundingBoxHeight / 2.0F / entityScale, 0.0F);
      }
   }

   protected HumanoidModel.ArmPose getArmPose(final Drowned mob, final HumanoidArm arm) {
      ItemStack item = mob.getItemHeldByArm(arm);
      return mob.getMainArm() == arm && mob.isAggressive() && item.is(Items.TRIDENT) ? HumanoidModel.ArmPose.THROW_TRIDENT : super.getArmPose(mob, arm);
   }
}
