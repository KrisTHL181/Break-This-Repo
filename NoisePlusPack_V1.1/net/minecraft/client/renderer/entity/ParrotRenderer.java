package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.parrot.ParrotModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.ParrotRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ParrotRenderer extends MobRenderer<Parrot, ParrotRenderState, ParrotModel> {
   private static final Identifier RED_BLUE = Identifier.withDefaultNamespace("textures/entity/parrot/parrot_red_blue.png");
   private static final Identifier BLUE = Identifier.withDefaultNamespace("textures/entity/parrot/parrot_blue.png");
   private static final Identifier GREEN = Identifier.withDefaultNamespace("textures/entity/parrot/parrot_green.png");
   private static final Identifier YELLOW_BLUE = Identifier.withDefaultNamespace("textures/entity/parrot/parrot_yellow_blue.png");
   private static final Identifier GREY = Identifier.withDefaultNamespace("textures/entity/parrot/parrot_grey.png");

   public ParrotRenderer(EntityRendererProvider.Context p_174336_) {
      super(p_174336_, new ParrotModel(p_174336_.bakeLayer(ModelLayers.PARROT)), 0.3F);
   }

   public Identifier getTextureLocation(ParrotRenderState p_450480_) {
      return getVariantTexture(p_450480_.variant);
   }

   public ParrotRenderState createRenderState() {
      return new ParrotRenderState();
   }

   public void extractRenderState(Parrot p_456442_, ParrotRenderState p_366002_, float p_362989_) {
      super.extractRenderState(p_456442_, p_366002_, p_362989_);
      p_366002_.variant = p_456442_.getVariant();
      float f = Mth.lerp(p_362989_, p_456442_.oFlap, p_456442_.flap);
      float f1 = Mth.lerp(p_362989_, p_456442_.oFlapSpeed, p_456442_.flapSpeed);
      p_366002_.flapAngle = (Mth.sin(f) + 1.0F) * f1;
      p_366002_.pose = ParrotModel.getPose(p_456442_);
   }

   public static Identifier getVariantTexture(Parrot.Variant p_454200_) {
      return switch (p_454200_) {
         case RED_BLUE -> RED_BLUE;
         case BLUE -> BLUE;
         case GREEN -> GREEN;
         case YELLOW_BLUE -> YELLOW_BLUE;
         case GRAY -> GREY;
      };
   }
}
