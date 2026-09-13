package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.zombie.DrownedModel;
import net.minecraft.client.renderer.entity.layers.DrownedOuterLayer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DrownedRenderer extends AbstractZombieRenderer<Drowned, ZombieRenderState, DrownedModel> {
   private static final Identifier DROWNED_LOCATION = Identifier.withDefaultNamespace("textures/entity/zombie/drowned.png");

   public DrownedRenderer(EntityRendererProvider.Context p_173964_) {
      super(
         p_173964_,
         new DrownedModel(p_173964_.bakeLayer(ModelLayers.DROWNED)),
         new DrownedModel(p_173964_.bakeLayer(ModelLayers.DROWNED_BABY)),
         ArmorModelSet.bake(ModelLayers.DROWNED_ARMOR, p_173964_.getModelSet(), DrownedModel::new),
         ArmorModelSet.bake(ModelLayers.DROWNED_BABY_ARMOR, p_173964_.getModelSet(), DrownedModel::new)
      );
      this.addLayer(new DrownedOuterLayer(this, p_173964_.getModelSet()));
   }

   public ZombieRenderState createRenderState() {
      return new ZombieRenderState();
   }

   @Override
   public Identifier getTextureLocation(ZombieRenderState p_461061_) {
      return DROWNED_LOCATION;
   }

   protected void setupRotations(ZombieRenderState p_368450_, PoseStack p_114104_, float p_114105_, float p_114106_) {
      super.setupRotations(p_368450_, p_114104_, p_114105_, p_114106_);
      float f = p_368450_.swimAmount;
      if (f > 0.0F) {
         float f1 = -10.0F - p_368450_.xRot;
         float f2 = Mth.lerp(f, 0.0F, f1);
         p_114104_.rotateAround(Axis.XP.rotationDegrees(f2), 0.0F, p_368450_.boundingBoxHeight / 2.0F / p_114106_, 0.0F);
      }
   }

   protected HumanoidModel.ArmPose getArmPose(Drowned p_453300_, HumanoidArm p_459022_) {
      ItemStack itemstack = p_453300_.getItemHeldByArm(p_459022_);
      return p_453300_.getMainArm() == p_459022_ && p_453300_.isAggressive() && itemstack.is(Items.TRIDENT)
         ? HumanoidModel.ArmPose.THROW_TRIDENT
         : super.getArmPose(p_453300_, p_459022_);
   }
}
