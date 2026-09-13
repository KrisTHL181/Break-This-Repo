package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.equine.BabyDonkeyModel;
import net.minecraft.client.model.animal.equine.DonkeyModel;
import net.minecraft.client.model.animal.equine.EquineSaddleModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.DonkeyRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;

public class DonkeyRenderer<T extends AbstractChestedHorse> extends AbstractHorseRenderer<T, DonkeyRenderState, DonkeyModel> {
   private final Identifier adultTexture;
   private final Identifier babyTexture;

   public DonkeyRenderer(
      final EntityRendererProvider.Context context,
      final EquipmentClientInfo.LayerType saddleLayer,
      final ModelLayerLocation saddleModel,
      final DonkeyRenderer.Type adult,
      final DonkeyRenderer.Type baby
   ) {
      super(context, new DonkeyModel(context.bakeLayer(adult.model)), new BabyDonkeyModel(context.bakeLayer(baby.model)));
      this.adultTexture = adult.texture;
      this.babyTexture = baby.texture;
      this.addLayer(
         new SimpleEquipmentLayer<>(
            this, context.getEquipmentRenderer(), saddleLayer, state -> state.saddle, new EquineSaddleModel(context.bakeLayer(saddleModel)), null
         )
      );
   }

   public Identifier getTextureLocation(final DonkeyRenderState state) {
      return state.isBaby ? this.babyTexture : this.adultTexture;
   }

   public DonkeyRenderState createRenderState() {
      return new DonkeyRenderState();
   }

   public void extractRenderState(final T entity, final DonkeyRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.hasChest = entity.hasChest();
   }

   public enum Type {
      DONKEY(Identifier.withDefaultNamespace("textures/entity/horse/donkey.png"), ModelLayers.DONKEY),
      DONKEY_BABY(Identifier.withDefaultNamespace("textures/entity/horse/donkey_baby.png"), ModelLayers.DONKEY_BABY),
      MULE(Identifier.withDefaultNamespace("textures/entity/horse/mule.png"), ModelLayers.MULE),
      MULE_BABY(Identifier.withDefaultNamespace("textures/entity/horse/mule_baby.png"), ModelLayers.MULE_BABY);

      private final Identifier texture;
      private final ModelLayerLocation model;

      Type(final Identifier texture, final ModelLayerLocation model) {
         this.texture = texture;
         this.model = model;
      }
   }
}
