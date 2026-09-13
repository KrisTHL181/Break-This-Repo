package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ZombieRenderer extends AbstractZombieRenderer<Zombie, ZombieRenderState, ZombieModel<ZombieRenderState>> {
   public ZombieRenderer(EntityRendererProvider.Context p_174456_) {
      this(p_174456_, ModelLayers.ZOMBIE, ModelLayers.ZOMBIE_BABY, ModelLayers.ZOMBIE_ARMOR, ModelLayers.ZOMBIE_BABY_ARMOR);
   }

   public ZombieRenderState createRenderState() {
      return new ZombieRenderState();
   }

   public ZombieRenderer(
      EntityRendererProvider.Context p_174458_,
      ModelLayerLocation p_174459_,
      ModelLayerLocation p_174460_,
      ArmorModelSet<ModelLayerLocation> p_424473_,
      ArmorModelSet<ModelLayerLocation> p_426191_
   ) {
      super(
         p_174458_,
         new ZombieModel<>(p_174458_.bakeLayer(p_174459_)),
         new ZombieModel<>(p_174458_.bakeLayer(p_174460_)),
         ArmorModelSet.bake(p_424473_, p_174458_.getModelSet(), ZombieModel::new),
         ArmorModelSet.bake(p_426191_, p_174458_.getModelSet(), ZombieModel::new)
      );
   }
}
