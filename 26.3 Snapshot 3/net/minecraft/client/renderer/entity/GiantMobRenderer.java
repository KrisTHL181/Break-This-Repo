package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.zombie.GiantZombieModel;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.Giant;

public class GiantMobRenderer extends MobRenderer<Giant, ZombieRenderState, HumanoidModel<ZombieRenderState>> {
   private static final Identifier ZOMBIE_LOCATION = Identifier.withDefaultNamespace("textures/entity/zombie/zombie.png");

   public GiantMobRenderer(final EntityRendererProvider.Context context, final float scale) {
      super(context, new GiantZombieModel(context.bakeLayer(ModelLayers.GIANT)), 0.5F * scale);
      this.addLayer(new ItemInHandLayer<>(this));
      this.addLayer(
         new HumanoidArmorLayer<>(
            this, ArmorModelSet.bake(ModelLayers.GIANT_ARMOR, context.getModelSet(), GiantZombieModel::new), context.getEquipmentRenderer()
         )
      );
   }

   public Identifier getTextureLocation(final ZombieRenderState state) {
      return ZOMBIE_LOCATION;
   }

   public ZombieRenderState createRenderState() {
      return new ZombieRenderState();
   }

   public void extractRenderState(final Giant entity, final ZombieRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      HumanoidMobRenderer.extractHumanoidRenderState(entity, state, partialTicks, this.itemModelResolver);
   }
}
