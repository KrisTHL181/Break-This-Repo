package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.zombie.BabyZombieVillagerModel;
import net.minecraft.client.model.monster.zombie.ZombieVillagerModel;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.client.renderer.entity.state.ZombieVillagerRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;

public class ZombieVillagerRenderer extends HumanoidMobRenderer<ZombieVillager, ZombieVillagerRenderState, ZombieVillagerModel<ZombieVillagerRenderState>> {
   private static final Identifier ZOMBIE_VILLAGER_LOCATION = Identifier.withDefaultNamespace("textures/entity/zombie_villager/zombie_villager.png");
   private static final Identifier BABY_ZOMBIE_VILLAGER_LOCATION = Identifier.withDefaultNamespace("textures/entity/zombie_villager/zombie_villager_baby.png");

   public ZombieVillagerRenderer(final EntityRendererProvider.Context context) {
      super(
         context,
         new ZombieVillagerModel<ZombieVillagerRenderState>(context.bakeLayer(ModelLayers.ZOMBIE_VILLAGER)),
         new BabyZombieVillagerModel<>(context.bakeLayer(ModelLayers.ZOMBIE_VILLAGER_BABY)),
         0.5F,
         VillagerRenderer.CUSTOM_HEAD_TRANSFORMS
      );
      this.addLayer(
         new HumanoidArmorLayer<>(
            this,
            ArmorModelSet.bake(ModelLayers.ZOMBIE_VILLAGER_ARMOR, context.getModelSet(), ZombieVillagerModel::new),
            ArmorModelSet.bake(ModelLayers.ZOMBIE_VILLAGER_BABY_ARMOR, context.getModelSet(), BabyZombieVillagerModel::new),
            context.getEquipmentRenderer()
         )
      );
      this.addLayer(
         new VillagerProfessionLayer<>(
            this,
            context.getResourceManager(),
            "zombie_villager",
            new ZombieVillagerModel<ZombieVillagerRenderState>(context.bakeLayer(ModelLayers.ZOMBIE_VILLAGER_NO_HAT)),
            new BabyZombieVillagerModel<>(context.bakeLayer(ModelLayers.ZOMBIE_VILLAGER_BABY_NO_HAT))
         )
      );
   }

   public Identifier getTextureLocation(final ZombieVillagerRenderState state) {
      return state.isBaby ? BABY_ZOMBIE_VILLAGER_LOCATION : ZOMBIE_VILLAGER_LOCATION;
   }

   public ZombieVillagerRenderState createRenderState() {
      return new ZombieVillagerRenderState();
   }

   public void extractRenderState(final ZombieVillager entity, final ZombieVillagerRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.isConverting = entity.isConverting();
      state.villagerData = entity.getVillagerData();
      state.isAggressive = entity.isAggressive();
   }

   protected boolean isShaking(final ZombieVillagerRenderState state) {
      return super.isShaking(state) || state.isConverting;
   }
}
