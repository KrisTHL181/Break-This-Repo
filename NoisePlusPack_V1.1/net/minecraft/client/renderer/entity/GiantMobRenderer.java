package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.zombie.GiantZombieModel;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.Giant;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GiantMobRenderer extends MobRenderer<Giant, ZombieRenderState, HumanoidModel<ZombieRenderState>> {
   private static final Identifier ZOMBIE_LOCATION = Identifier.withDefaultNamespace("textures/entity/zombie/zombie.png");

   public GiantMobRenderer(EntityRendererProvider.Context p_174131_, float p_174132_) {
      super(p_174131_, new GiantZombieModel(p_174131_.bakeLayer(ModelLayers.GIANT)), 0.5F * p_174132_);
      this.addLayer(new ItemInHandLayer<>(this));
      this.addLayer(
         new HumanoidArmorLayer<>(
            this, ArmorModelSet.bake(ModelLayers.GIANT_ARMOR, p_174131_.getModelSet(), GiantZombieModel::new), p_174131_.getEquipmentRenderer()
         )
      );
   }

   public Identifier getTextureLocation(ZombieRenderState p_454509_) {
      return ZOMBIE_LOCATION;
   }

   public ZombieRenderState createRenderState() {
      return new ZombieRenderState();
   }

   public void extractRenderState(Giant p_361435_, ZombieRenderState p_362298_, float p_367781_) {
      super.extractRenderState(p_361435_, p_362298_, p_367781_);
      HumanoidMobRenderer.extractHumanoidRenderState(p_361435_, p_362298_, p_367781_, this.itemModelResolver);
   }
}
