package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.parrot.ParrotModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.ParrotRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.parrot.Parrot;

public class ParrotRenderer extends MobRenderer<Parrot, ParrotRenderState, ParrotModel> {
   private static final Identifier RED_BLUE = Identifier.withDefaultNamespace("textures/entity/parrot/parrot_red_blue.png");
   private static final Identifier BLUE = Identifier.withDefaultNamespace("textures/entity/parrot/parrot_blue.png");
   private static final Identifier GREEN = Identifier.withDefaultNamespace("textures/entity/parrot/parrot_green.png");
   private static final Identifier YELLOW_BLUE = Identifier.withDefaultNamespace("textures/entity/parrot/parrot_yellow_blue.png");
   private static final Identifier GREY = Identifier.withDefaultNamespace("textures/entity/parrot/parrot_grey.png");

   public ParrotRenderer(final EntityRendererProvider.Context context) {
      super(context, new ParrotModel(context.bakeLayer(ModelLayers.PARROT)), 0.3F);
   }

   public Identifier getTextureLocation(final ParrotRenderState state) {
      return getVariantTexture(state.variant);
   }

   public ParrotRenderState createRenderState() {
      return new ParrotRenderState();
   }

   public void extractRenderState(final Parrot entity, final ParrotRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.variant = entity.getVariant();
      float flap = Mth.lerp(partialTicks, entity.oFlap, entity.flap);
      float flapSpeed = Mth.lerp(partialTicks, entity.oFlapSpeed, entity.flapSpeed);
      state.flapAngle = (Mth.sin(flap) + 1.0F) * flapSpeed;
      state.pose = ParrotModel.getPose(entity);
   }

   public static Identifier getVariantTexture(final Parrot.Variant variant) {
      return switch (variant) {
         case RED_BLUE -> RED_BLUE;
         case BLUE -> BLUE;
         case GREEN -> GREEN;
         case YELLOW_BLUE -> YELLOW_BLUE;
         case GRAY -> GREY;
      };
   }
}
