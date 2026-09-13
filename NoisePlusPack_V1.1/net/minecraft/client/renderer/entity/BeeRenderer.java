package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.bee.BeeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.BeeRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BeeRenderer extends AgeableMobRenderer<Bee, BeeRenderState, BeeModel> {
   private static final Identifier ANGRY_BEE_TEXTURE = Identifier.withDefaultNamespace("textures/entity/bee/bee_angry.png");
   private static final Identifier ANGRY_NECTAR_BEE_TEXTURE = Identifier.withDefaultNamespace("textures/entity/bee/bee_angry_nectar.png");
   private static final Identifier BEE_TEXTURE = Identifier.withDefaultNamespace("textures/entity/bee/bee.png");
   private static final Identifier NECTAR_BEE_TEXTURE = Identifier.withDefaultNamespace("textures/entity/bee/bee_nectar.png");

   public BeeRenderer(EntityRendererProvider.Context p_173931_) {
      super(p_173931_, new BeeModel(p_173931_.bakeLayer(ModelLayers.BEE)), new BeeModel(p_173931_.bakeLayer(ModelLayers.BEE_BABY)), 0.4F);
   }

   public Identifier getTextureLocation(BeeRenderState p_456293_) {
      if (p_456293_.isAngry) {
         return p_456293_.hasNectar ? ANGRY_NECTAR_BEE_TEXTURE : ANGRY_BEE_TEXTURE;
      } else {
         return p_456293_.hasNectar ? NECTAR_BEE_TEXTURE : BEE_TEXTURE;
      }
   }

   public BeeRenderState createRenderState() {
      return new BeeRenderState();
   }

   public void extractRenderState(Bee p_453790_, BeeRenderState p_362934_, float p_366251_) {
      super.extractRenderState(p_453790_, p_362934_, p_366251_);
      p_362934_.rollAmount = p_453790_.getRollAmount(p_366251_);
      p_362934_.hasStinger = !p_453790_.hasStung();
      p_362934_.isOnGround = p_453790_.onGround() && p_453790_.getDeltaMovement().lengthSqr() < 1.0E-7;
      p_362934_.isAngry = p_453790_.isAngry();
      p_362934_.hasNectar = p_453790_.hasNectar();
   }
}
