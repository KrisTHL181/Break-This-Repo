package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.equine.AbstractEquineModel;
import net.minecraft.client.model.animal.equine.BabyHorseModel;
import net.minecraft.client.model.animal.equine.EquineSaddleModel;
import net.minecraft.client.model.animal.equine.HorseModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

public class UndeadHorseRenderer extends AbstractHorseRenderer<AbstractHorse, EquineRenderState, AbstractEquineModel<EquineRenderState>> {
   private final Identifier adultTexture;
   private final Identifier babyTexture;

   public UndeadHorseRenderer(
      final EntityRendererProvider.Context context,
      final EquipmentClientInfo.LayerType saddleLayer,
      final ModelLayerLocation saddleModel,
      final UndeadHorseRenderer.Type adult,
      final UndeadHorseRenderer.Type baby
   ) {
      super(context, new HorseModel(context.bakeLayer(adult.model)), new BabyHorseModel(context.bakeLayer(baby.model)));
      this.adultTexture = adult.texture;
      this.babyTexture = baby.texture;
      this.addLayer(
         new SimpleEquipmentLayer<>(
            this,
            context.getEquipmentRenderer(),
            EquipmentClientInfo.LayerType.HORSE_BODY,
            state -> state.bodyArmorItem,
            new HorseModel(context.bakeLayer(ModelLayers.UNDEAD_HORSE_ARMOR)),
            null
         )
      );
      this.addLayer(
         new SimpleEquipmentLayer<>(
            this, context.getEquipmentRenderer(), saddleLayer, state -> state.saddle, new EquineSaddleModel(context.bakeLayer(saddleModel)), null
         )
      );
   }

   public Identifier getTextureLocation(final EquineRenderState state) {
      return state.isBaby ? this.babyTexture : this.adultTexture;
   }

   public EquineRenderState createRenderState() {
      return new EquineRenderState();
   }

   public enum Type {
      SKELETON(Identifier.withDefaultNamespace("textures/entity/horse/horse_skeleton.png"), ModelLayers.SKELETON_HORSE),
      SKELETON_BABY(Identifier.withDefaultNamespace("textures/entity/horse/horse_skeleton_baby.png"), ModelLayers.SKELETON_HORSE_BABY),
      ZOMBIE(Identifier.withDefaultNamespace("textures/entity/horse/horse_zombie.png"), ModelLayers.ZOMBIE_HORSE),
      ZOMBIE_BABY(Identifier.withDefaultNamespace("textures/entity/horse/horse_zombie_baby.png"), ModelLayers.ZOMBIE_HORSE_BABY);

      private final Identifier texture;
      private final ModelLayerLocation model;

      Type(final Identifier texture, final ModelLayerLocation model) {
         this.texture = texture;
         this.model = model;
      }
   }
}
