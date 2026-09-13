package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.zombie.ZombieVillagerModel;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.client.renderer.entity.state.ZombieVillagerRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ZombieVillagerRenderer extends HumanoidMobRenderer<ZombieVillager, ZombieVillagerRenderState, ZombieVillagerModel<ZombieVillagerRenderState>> {
   private static final Identifier ZOMBIE_VILLAGER_LOCATION = Identifier.withDefaultNamespace("textures/entity/zombie_villager/zombie_villager.png");

   public ZombieVillagerRenderer(EntityRendererProvider.Context p_174463_) {
      super(
         p_174463_,
         new ZombieVillagerModel<>(p_174463_.bakeLayer(ModelLayers.ZOMBIE_VILLAGER)),
         new ZombieVillagerModel<>(p_174463_.bakeLayer(ModelLayers.ZOMBIE_VILLAGER_BABY)),
         0.5F,
         VillagerRenderer.CUSTOM_HEAD_TRANSFORMS
      );
      this.addLayer(
         new HumanoidArmorLayer<>(
            this,
            ArmorModelSet.bake(ModelLayers.ZOMBIE_VILLAGER_ARMOR, p_174463_.getModelSet(), ZombieVillagerModel::new),
            ArmorModelSet.bake(ModelLayers.ZOMBIE_VILLAGER_BABY_ARMOR, p_174463_.getModelSet(), ZombieVillagerModel::new),
            p_174463_.getEquipmentRenderer()
         )
      );
      this.addLayer(
         new VillagerProfessionLayer<>(
            this,
            p_174463_.getResourceManager(),
            "zombie_villager",
            new ZombieVillagerModel<>(p_174463_.bakeLayer(ModelLayers.ZOMBIE_VILLAGER_NO_HAT)),
            new ZombieVillagerModel<>(p_174463_.bakeLayer(ModelLayers.ZOMBIE_VILLAGER_BABY_NO_HAT))
         )
      );
   }

   public Identifier getTextureLocation(ZombieVillagerRenderState p_367471_) {
      return ZOMBIE_VILLAGER_LOCATION;
   }

   public ZombieVillagerRenderState createRenderState() {
      return new ZombieVillagerRenderState();
   }

   public void extractRenderState(ZombieVillager p_460615_, ZombieVillagerRenderState p_363256_, float p_364614_) {
      super.extractRenderState(p_460615_, p_363256_, p_364614_);
      p_363256_.isConverting = p_460615_.isConverting();
      p_363256_.villagerData = p_460615_.getVillagerData();
      p_363256_.isAggressive = p_460615_.isAggressive();
   }

   protected boolean isShaking(ZombieVillagerRenderState p_364608_) {
      return super.isShaking(p_364608_) || p_364608_.isConverting;
   }
}
