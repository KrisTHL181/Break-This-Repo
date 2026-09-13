package net.minecraft.client.renderer.entity;

import com.google.common.collect.Maps;
import java.util.Map;
import net.minecraft.client.model.animal.equine.EquineSaddleModel;
import net.minecraft.client.model.animal.equine.HorseModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.HorseMarkingLayer;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.HorseRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.equine.Variant;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class HorseRenderer extends AbstractHorseRenderer<Horse, HorseRenderState, HorseModel> {
   private static final Map<Variant, Identifier> LOCATION_BY_VARIANT = Maps.newEnumMap(
      Map.of(
         Variant.WHITE,
         Identifier.withDefaultNamespace("textures/entity/horse/horse_white.png"),
         Variant.CREAMY,
         Identifier.withDefaultNamespace("textures/entity/horse/horse_creamy.png"),
         Variant.CHESTNUT,
         Identifier.withDefaultNamespace("textures/entity/horse/horse_chestnut.png"),
         Variant.BROWN,
         Identifier.withDefaultNamespace("textures/entity/horse/horse_brown.png"),
         Variant.BLACK,
         Identifier.withDefaultNamespace("textures/entity/horse/horse_black.png"),
         Variant.GRAY,
         Identifier.withDefaultNamespace("textures/entity/horse/horse_gray.png"),
         Variant.DARK_BROWN,
         Identifier.withDefaultNamespace("textures/entity/horse/horse_darkbrown.png")
      )
   );

   public HorseRenderer(EntityRendererProvider.Context p_174167_) {
      super(p_174167_, new HorseModel(p_174167_.bakeLayer(ModelLayers.HORSE)), new HorseModel(p_174167_.bakeLayer(ModelLayers.HORSE_BABY)));
      this.addLayer(new HorseMarkingLayer(this));
      this.addLayer(
         new SimpleEquipmentLayer<>(
            this,
            p_174167_.getEquipmentRenderer(),
            EquipmentClientInfo.LayerType.HORSE_BODY,
            p_389515_ -> p_389515_.bodyArmorItem,
            new HorseModel(p_174167_.bakeLayer(ModelLayers.HORSE_ARMOR)),
            new HorseModel(p_174167_.bakeLayer(ModelLayers.HORSE_BABY_ARMOR)),
            2
         )
      );
      this.addLayer(
         new SimpleEquipmentLayer<>(
            this,
            p_174167_.getEquipmentRenderer(),
            EquipmentClientInfo.LayerType.HORSE_SADDLE,
            p_389516_ -> p_389516_.saddle,
            new EquineSaddleModel(p_174167_.bakeLayer(ModelLayers.HORSE_SADDLE)),
            new EquineSaddleModel(p_174167_.bakeLayer(ModelLayers.HORSE_BABY_SADDLE)),
            2
         )
      );
   }

   public Identifier getTextureLocation(HorseRenderState p_456331_) {
      return LOCATION_BY_VARIANT.get(p_456331_.variant);
   }

   public HorseRenderState createRenderState() {
      return new HorseRenderState();
   }

   public void extractRenderState(Horse p_452059_, HorseRenderState p_458870_, float p_367677_) {
      super.extractRenderState(p_452059_, p_458870_, p_367677_);
      p_458870_.variant = p_452059_.getVariant();
      p_458870_.markings = p_452059_.getMarkings();
      p_458870_.bodyArmorItem = p_452059_.getBodyArmorItem().copy();
   }
}
