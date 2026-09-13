package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.ghast.HappyGhastHarnessModel;
import net.minecraft.client.model.animal.ghast.HappyGhastModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.RopesLayer;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.HappyGhastRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HappyGhastRenderer extends AgeableMobRenderer<HappyGhast, HappyGhastRenderState, HappyGhastModel> {
   private static final Identifier GHAST_LOCATION = Identifier.withDefaultNamespace("textures/entity/ghast/happy_ghast.png");
   private static final Identifier GHAST_BABY_LOCATION = Identifier.withDefaultNamespace("textures/entity/ghast/happy_ghast_baby.png");
   private static final Identifier GHAST_ROPES = Identifier.withDefaultNamespace("textures/entity/ghast/happy_ghast_ropes.png");

   public HappyGhastRenderer(EntityRendererProvider.Context p_408214_) {
      super(
         p_408214_,
         new HappyGhastModel(p_408214_.bakeLayer(ModelLayers.HAPPY_GHAST)),
         new HappyGhastModel(p_408214_.bakeLayer(ModelLayers.HAPPY_GHAST_BABY)),
         2.0F
      );
      this.addLayer(
         new SimpleEquipmentLayer<>(
            this,
            p_408214_.getEquipmentRenderer(),
            EquipmentClientInfo.LayerType.HAPPY_GHAST_BODY,
            p_408530_ -> p_408530_.bodyItem,
            new HappyGhastHarnessModel(p_408214_.bakeLayer(ModelLayers.HAPPY_GHAST_HARNESS)),
            new HappyGhastHarnessModel(p_408214_.bakeLayer(ModelLayers.HAPPY_GHAST_BABY_HARNESS))
         )
      );
      this.addLayer(new RopesLayer<>(this, p_408214_.getModelSet(), GHAST_ROPES));
   }

   public Identifier getTextureLocation(HappyGhastRenderState p_407787_) {
      return p_407787_.isBaby ? GHAST_BABY_LOCATION : GHAST_LOCATION;
   }

   public HappyGhastRenderState createRenderState() {
      return new HappyGhastRenderState();
   }

   protected AABB getBoundingBoxForCulling(HappyGhast p_458413_) {
      AABB aabb = super.getBoundingBoxForCulling(p_458413_);
      float f = p_458413_.getBbHeight();
      return aabb.setMinY(aabb.minY - f / 2.0F);
   }

   public void extractRenderState(HappyGhast p_454974_, HappyGhastRenderState p_407988_, float p_409409_) {
      super.extractRenderState(p_454974_, p_407988_, p_409409_);
      p_407988_.bodyItem = p_454974_.getItemBySlot(EquipmentSlot.BODY).copy();
      p_407988_.isRidden = p_454974_.isVehicle();
      p_407988_.isLeashHolder = p_454974_.isLeashHolder();
   }
}
