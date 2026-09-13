package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.equine.DonkeyModel;
import net.minecraft.client.model.animal.equine.EquineSaddleModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.DonkeyRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DonkeyRenderer<T extends AbstractChestedHorse> extends AbstractHorseRenderer<T, DonkeyRenderState, DonkeyModel> {
   private final Identifier texture;

   public DonkeyRenderer(EntityRendererProvider.Context p_362293_, DonkeyRenderer.Type p_397487_) {
      super(p_362293_, new DonkeyModel(p_362293_.bakeLayer(p_397487_.model)), new DonkeyModel(p_362293_.bakeLayer(p_397487_.babyModel)));
      this.texture = p_397487_.texture;
      this.addLayer(
         new SimpleEquipmentLayer<>(
            this,
            p_362293_.getEquipmentRenderer(),
            p_397487_.saddleLayer,
            p_397593_ -> p_397593_.saddle,
            new EquineSaddleModel(p_362293_.bakeLayer(p_397487_.saddleModel)),
            new EquineSaddleModel(p_362293_.bakeLayer(p_397487_.babySaddleModel))
         )
      );
   }

   public Identifier getTextureLocation(DonkeyRenderState p_367902_) {
      return this.texture;
   }

   public DonkeyRenderState createRenderState() {
      return new DonkeyRenderState();
   }

   public void extractRenderState(T p_453396_, DonkeyRenderState p_451699_, float p_360740_) {
      super.extractRenderState(p_453396_, p_451699_, p_360740_);
      p_451699_.hasChest = p_453396_.hasChest();
   }

   @OnlyIn(Dist.CLIENT)
   public enum Type {
      DONKEY(
         Identifier.withDefaultNamespace("textures/entity/horse/donkey.png"),
         ModelLayers.DONKEY,
         ModelLayers.DONKEY_BABY,
         EquipmentClientInfo.LayerType.DONKEY_SADDLE,
         ModelLayers.DONKEY_SADDLE,
         ModelLayers.DONKEY_BABY_SADDLE
      ),
      MULE(
         Identifier.withDefaultNamespace("textures/entity/horse/mule.png"),
         ModelLayers.MULE,
         ModelLayers.MULE_BABY,
         EquipmentClientInfo.LayerType.MULE_SADDLE,
         ModelLayers.MULE_SADDLE,
         ModelLayers.MULE_BABY_SADDLE
      );

      final Identifier texture;
      final ModelLayerLocation model;
      final ModelLayerLocation babyModel;
      final EquipmentClientInfo.LayerType saddleLayer;
      final ModelLayerLocation saddleModel;
      final ModelLayerLocation babySaddleModel;

      Type(
         final Identifier p_453708_,
         final ModelLayerLocation p_396383_,
         final ModelLayerLocation p_392561_,
         final EquipmentClientInfo.LayerType p_394331_,
         final ModelLayerLocation p_392966_,
         final ModelLayerLocation p_394783_
      ) {
         this.texture = p_453708_;
         this.model = p_396383_;
         this.babyModel = p_392561_;
         this.saddleLayer = p_394331_;
         this.saddleModel = p_392966_;
         this.babySaddleModel = p_394783_;
      }
   }
}
