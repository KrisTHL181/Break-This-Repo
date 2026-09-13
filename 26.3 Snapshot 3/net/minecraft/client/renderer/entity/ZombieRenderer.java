package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.zombie.BabyZombieModel;
import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.world.entity.monster.zombie.Zombie;

public class ZombieRenderer extends AbstractZombieRenderer<Zombie, ZombieRenderState, ZombieModel<ZombieRenderState>> {
   public ZombieRenderer(final EntityRendererProvider.Context context) {
      this(context, ModelLayers.ZOMBIE, ModelLayers.ZOMBIE_BABY, ModelLayers.ZOMBIE_ARMOR, ModelLayers.ZOMBIE_BABY_ARMOR);
   }

   public ZombieRenderState createRenderState() {
      return new ZombieRenderState();
   }

   public ZombieRenderer(
      final EntityRendererProvider.Context context,
      final ModelLayerLocation body,
      final ModelLayerLocation babyBody,
      final ArmorModelSet<ModelLayerLocation> armorSet,
      final ArmorModelSet<ModelLayerLocation> babyArmorSet
   ) {
      super(
         context,
         new ZombieModel<ZombieRenderState>(context.bakeLayer(body)),
         new BabyZombieModel<>(context.bakeLayer(babyBody)),
         ArmorModelSet.bake(armorSet, context.getModelSet(), ZombieModel::new),
         ArmorModelSet.bake(babyArmorSet, context.getModelSet(), BabyZombieModel::new)
      );
   }
}
