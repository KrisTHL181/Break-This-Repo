package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.npc.BabyVillagerModel;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.client.renderer.entity.state.HoldingEntityRenderState;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.Villager;

public class VillagerRenderer extends AgeableMobRenderer<Villager, VillagerRenderState, VillagerModel> {
   private static final Identifier VILLAGER_BASE_LOCATION = Identifier.withDefaultNamespace("textures/entity/villager/villager.png");
   private static final Identifier VILLAGER_BABY_LOCATION = Identifier.withDefaultNamespace("textures/entity/villager/villager_baby.png");
   public static final CustomHeadLayer.Transforms CUSTOM_HEAD_TRANSFORMS = new CustomHeadLayer.Transforms(-0.1171875F, -0.07421875F, 1.0F);

   public VillagerRenderer(final EntityRendererProvider.Context context) {
      super(context, new VillagerModel(context.bakeLayer(ModelLayers.VILLAGER)), new BabyVillagerModel(context.bakeLayer(ModelLayers.VILLAGER_BABY)), 0.5F);
      this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getPlayerSkinRenderCache(), CUSTOM_HEAD_TRANSFORMS));
      this.addLayer(
         new VillagerProfessionLayer<>(
            this,
            context.getResourceManager(),
            "villager",
            new VillagerModel(context.bakeLayer(ModelLayers.VILLAGER_NO_HAT)),
            new BabyVillagerModel(context.bakeLayer(ModelLayers.VILLAGER_BABY_NO_HAT))
         )
      );
      this.addLayer(new CrossedArmsItemLayer<>(this));
   }

   public Identifier getTextureLocation(final VillagerRenderState state) {
      return state.isBaby ? VILLAGER_BABY_LOCATION : VILLAGER_BASE_LOCATION;
   }

   protected float getShadowRadius(final VillagerRenderState state) {
      float radius = super.getShadowRadius(state);
      return state.isBaby ? radius * 0.5F : radius;
   }

   public VillagerRenderState createRenderState() {
      return new VillagerRenderState();
   }

   public void extractRenderState(final Villager entity, final VillagerRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      HoldingEntityRenderState.extractHoldingEntityRenderState(entity, state, this.itemModelResolver);
      state.isUnhappy = entity.getUnhappyCounter() > 0;
      state.villagerData = entity.getVillagerData();
   }
}
