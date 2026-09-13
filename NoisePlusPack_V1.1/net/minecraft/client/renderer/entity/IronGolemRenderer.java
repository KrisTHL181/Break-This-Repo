package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.animal.golem.IronGolemModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.IronGolemCrackinessLayer;
import net.minecraft.client.renderer.entity.layers.IronGolemFlowerLayer;
import net.minecraft.client.renderer.entity.state.IronGolemRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class IronGolemRenderer extends MobRenderer<IronGolem, IronGolemRenderState, IronGolemModel> {
   private static final Identifier GOLEM_LOCATION = Identifier.withDefaultNamespace("textures/entity/iron_golem/iron_golem.png");

   public IronGolemRenderer(EntityRendererProvider.Context p_174188_) {
      super(p_174188_, new IronGolemModel(p_174188_.bakeLayer(ModelLayers.IRON_GOLEM)), 0.7F);
      this.addLayer(new IronGolemCrackinessLayer(this));
      this.addLayer(new IronGolemFlowerLayer(this));
   }

   public Identifier getTextureLocation(IronGolemRenderState p_459654_) {
      return GOLEM_LOCATION;
   }

   public IronGolemRenderState createRenderState() {
      return new IronGolemRenderState();
   }

   public void extractRenderState(IronGolem p_458283_, IronGolemRenderState p_363683_, float p_363302_) {
      super.extractRenderState(p_458283_, p_363683_, p_363302_);
      p_363683_.attackTicksRemaining = p_458283_.getAttackAnimationTick() > 0.0F ? p_458283_.getAttackAnimationTick() - p_363302_ : 0.0F;
      p_363683_.offerFlowerTick = p_458283_.getOfferFlowerTick();
      p_363683_.crackiness = p_458283_.getCrackiness();
   }

   protected void setupRotations(IronGolemRenderState p_366774_, PoseStack p_115015_, float p_115016_, float p_115017_) {
      super.setupRotations(p_366774_, p_115015_, p_115016_, p_115017_);
      if (!(p_366774_.walkAnimationSpeed < 0.01)) {
         float f = 13.0F;
         float f1 = p_366774_.walkAnimationPos + 6.0F;
         float f2 = (Math.abs(f1 % 13.0F - 6.5F) - 3.25F) / 3.25F;
         p_115015_.mulPose(Axis.ZP.rotationDegrees(6.5F * f2));
      }
   }
}
