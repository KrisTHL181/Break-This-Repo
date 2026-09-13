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

public class HappyGhastRenderer extends AgeableMobRenderer<HappyGhast, HappyGhastRenderState, HappyGhastModel> {
   private static final Identifier GHAST_LOCATION = Identifier.withDefaultNamespace("textures/entity/ghast/happy_ghast.png");
   private static final Identifier GHAST_BABY_LOCATION = Identifier.withDefaultNamespace("textures/entity/ghast/happy_ghast_baby.png");
   private static final Identifier GHAST_ROPES = Identifier.withDefaultNamespace("textures/entity/ghast/happy_ghast_ropes.png");

   public HappyGhastRenderer(final EntityRendererProvider.Context context) {
      super(
         context, new HappyGhastModel(context.bakeLayer(ModelLayers.HAPPY_GHAST)), new HappyGhastModel(context.bakeLayer(ModelLayers.HAPPY_GHAST_BABY)), 2.0F
      );
      this.addLayer(
         new SimpleEquipmentLayer<>(
            this,
            context.getEquipmentRenderer(),
            EquipmentClientInfo.LayerType.HAPPY_GHAST_BODY,
            state -> state.bodyItem,
            new HappyGhastHarnessModel(context.bakeLayer(ModelLayers.HAPPY_GHAST_HARNESS)),
            new HappyGhastHarnessModel(context.bakeLayer(ModelLayers.HAPPY_GHAST_BABY_HARNESS))
         )
      );
      this.addLayer(new RopesLayer<>(this, context.getModelSet(), GHAST_ROPES));
   }

   public Identifier getTextureLocation(final HappyGhastRenderState state) {
      return state.isBaby ? GHAST_BABY_LOCATION : GHAST_LOCATION;
   }

   public HappyGhastRenderState createRenderState() {
      return new HappyGhastRenderState();
   }

   protected AABB getBoundingBoxForCulling(final HappyGhast entity) {
      AABB aabb = super.getBoundingBoxForCulling(entity);
      float height = entity.getBbHeight();
      return aabb.setMinY(aabb.minY - height / 2.0F);
   }

   public void extractRenderState(final HappyGhast entity, final HappyGhastRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.bodyItem = entity.getItemBySlot(EquipmentSlot.BODY).copy();
      state.isRidden = entity.isVehicle();
      state.isLeashHolder = entity.isLeashHolder();
   }
}
