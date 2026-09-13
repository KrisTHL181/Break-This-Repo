package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.strider.AdultStriderModel;
import net.minecraft.client.model.monster.strider.BabyStriderModel;
import net.minecraft.client.model.monster.strider.StriderModel;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.StriderRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Strider;

public class StriderRenderer extends AgeableMobRenderer<Strider, StriderRenderState, StriderModel> {
   private static final Identifier STRIDER_LOCATION = Identifier.withDefaultNamespace("textures/entity/strider/strider.png");
   private static final Identifier STRIDER_BABY_LOCATION = Identifier.withDefaultNamespace("textures/entity/strider/strider_baby.png");
   private static final Identifier COLD_LOCATION = Identifier.withDefaultNamespace("textures/entity/strider/strider_cold.png");
   private static final Identifier COLD_BABY_LOCATION = Identifier.withDefaultNamespace("textures/entity/strider/strider_cold_baby.png");
   private static final float SHADOW_RADIUS = 0.5F;

   public StriderRenderer(final EntityRendererProvider.Context context) {
      super(context, new AdultStriderModel(context.bakeLayer(ModelLayers.STRIDER)), new BabyStriderModel(context.bakeLayer(ModelLayers.STRIDER_BABY)), 0.5F);
      this.addLayer(
         new SimpleEquipmentLayer<>(
            this,
            context.getEquipmentRenderer(),
            EquipmentClientInfo.LayerType.STRIDER_SADDLE,
            state -> state.saddle,
            new AdultStriderModel(context.bakeLayer(ModelLayers.STRIDER_SADDLE)),
            null
         )
      );
   }

   public Identifier getTextureLocation(final StriderRenderState state) {
      if (state.isSuffocating) {
         return state.isBaby ? COLD_BABY_LOCATION : COLD_LOCATION;
      } else {
         return state.isBaby ? STRIDER_BABY_LOCATION : STRIDER_LOCATION;
      }
   }

   protected float getShadowRadius(final StriderRenderState state) {
      float radius = super.getShadowRadius(state);
      return state.isBaby ? radius * 0.5F : radius;
   }

   public StriderRenderState createRenderState() {
      return new StriderRenderState();
   }

   public void extractRenderState(final Strider entity, final StriderRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.saddle = entity.getItemBySlot(EquipmentSlot.SADDLE).copy();
      state.isSuffocating = entity.isSuffocating();
      state.isRidden = entity.isVehicle();
   }

   protected boolean isShaking(final StriderRenderState state) {
      return super.isShaking(state) || state.isSuffocating;
   }
}
