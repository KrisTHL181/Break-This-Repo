package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.client.renderer.entity.state.HoldingEntityRenderState;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class VillagerRenderer extends AgeableMobRenderer<Villager, VillagerRenderState, VillagerModel> {
   private static final Identifier VILLAGER_BASE_SKIN = Identifier.withDefaultNamespace("textures/entity/villager/villager.png");
   public static final CustomHeadLayer.Transforms CUSTOM_HEAD_TRANSFORMS = new CustomHeadLayer.Transforms(-0.1171875F, -0.07421875F, 1.0F);

   public VillagerRenderer(EntityRendererProvider.Context p_174437_) {
      super(p_174437_, new VillagerModel(p_174437_.bakeLayer(ModelLayers.VILLAGER)), new VillagerModel(p_174437_.bakeLayer(ModelLayers.VILLAGER_BABY)), 0.5F);
      this.addLayer(new CustomHeadLayer<>(this, p_174437_.getModelSet(), p_174437_.getPlayerSkinRenderCache(), CUSTOM_HEAD_TRANSFORMS));
      this.addLayer(
         new VillagerProfessionLayer<>(
            this,
            p_174437_.getResourceManager(),
            "villager",
            new VillagerModel(p_174437_.bakeLayer(ModelLayers.VILLAGER_NO_HAT)),
            new VillagerModel(p_174437_.bakeLayer(ModelLayers.VILLAGER_BABY_NO_HAT))
         )
      );
      this.addLayer(new CrossedArmsItemLayer<>(this));
   }

   public Identifier getTextureLocation(VillagerRenderState p_453289_) {
      return VILLAGER_BASE_SKIN;
   }

   protected float getShadowRadius(VillagerRenderState p_363752_) {
      float f = super.getShadowRadius(p_363752_);
      return p_363752_.isBaby ? f * 0.5F : f;
   }

   public VillagerRenderState createRenderState() {
      return new VillagerRenderState();
   }

   public void extractRenderState(Villager p_456228_, VillagerRenderState p_364690_, float p_364344_) {
      super.extractRenderState(p_456228_, p_364690_, p_364344_);
      HoldingEntityRenderState.extractHoldingEntityRenderState(p_456228_, p_364690_, this.itemModelResolver);
      p_364690_.isUnhappy = p_456228_.getUnhappyCounter() > 0;
      p_364690_.villagerData = p_456228_.getVillagerData();
   }
}
