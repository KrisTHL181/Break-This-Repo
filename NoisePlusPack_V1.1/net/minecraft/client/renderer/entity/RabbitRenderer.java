package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.rabbit.RabbitModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.RabbitRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RabbitRenderer extends AgeableMobRenderer<Rabbit, RabbitRenderState, RabbitModel> {
   private static final Identifier RABBIT_BROWN_LOCATION = Identifier.withDefaultNamespace("textures/entity/rabbit/brown.png");
   private static final Identifier RABBIT_WHITE_LOCATION = Identifier.withDefaultNamespace("textures/entity/rabbit/white.png");
   private static final Identifier RABBIT_BLACK_LOCATION = Identifier.withDefaultNamespace("textures/entity/rabbit/black.png");
   private static final Identifier RABBIT_GOLD_LOCATION = Identifier.withDefaultNamespace("textures/entity/rabbit/gold.png");
   private static final Identifier RABBIT_SALT_LOCATION = Identifier.withDefaultNamespace("textures/entity/rabbit/salt.png");
   private static final Identifier RABBIT_WHITE_SPLOTCHED_LOCATION = Identifier.withDefaultNamespace("textures/entity/rabbit/white_splotched.png");
   private static final Identifier RABBIT_TOAST_LOCATION = Identifier.withDefaultNamespace("textures/entity/rabbit/toast.png");
   private static final Identifier RABBIT_EVIL_LOCATION = Identifier.withDefaultNamespace("textures/entity/rabbit/caerbannog.png");

   public RabbitRenderer(EntityRendererProvider.Context p_174360_) {
      super(p_174360_, new RabbitModel(p_174360_.bakeLayer(ModelLayers.RABBIT)), new RabbitModel(p_174360_.bakeLayer(ModelLayers.RABBIT_BABY)), 0.3F);
   }

   public Identifier getTextureLocation(RabbitRenderState p_362493_) {
      if (p_362493_.isToast) {
         return RABBIT_TOAST_LOCATION;
      }

      return switch (p_362493_.variant) {
         case BROWN -> RABBIT_BROWN_LOCATION;
         case WHITE -> RABBIT_WHITE_LOCATION;
         case BLACK -> RABBIT_BLACK_LOCATION;
         case GOLD -> RABBIT_GOLD_LOCATION;
         case SALT -> RABBIT_SALT_LOCATION;
         case WHITE_SPLOTCHED -> RABBIT_WHITE_SPLOTCHED_LOCATION;
         case EVIL -> RABBIT_EVIL_LOCATION;
      };
   }

   public RabbitRenderState createRenderState() {
      return new RabbitRenderState();
   }

   public void extractRenderState(Rabbit p_457237_, RabbitRenderState p_365846_, float p_365965_) {
      super.extractRenderState(p_457237_, p_365846_, p_365965_);
      p_365846_.jumpCompletion = p_457237_.getJumpCompletion(p_365965_);
      p_365846_.isToast = checkMagicName(p_457237_, "Toast");
      p_365846_.variant = p_457237_.getVariant();
   }
}
