package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.witch.WitchModel;
import net.minecraft.client.renderer.entity.layers.WitchItemLayer;
import net.minecraft.client.renderer.entity.state.HoldingEntityRenderState;
import net.minecraft.client.renderer.entity.state.WitchRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WitchRenderer extends MobRenderer<Witch, WitchRenderState, WitchModel> {
   private static final Identifier WITCH_LOCATION = Identifier.withDefaultNamespace("textures/entity/witch.png");

   public WitchRenderer(EntityRendererProvider.Context p_174443_) {
      super(p_174443_, new WitchModel(p_174443_.bakeLayer(ModelLayers.WITCH)), 0.5F);
      this.addLayer(new WitchItemLayer(this));
   }

   public Identifier getTextureLocation(WitchRenderState p_455203_) {
      return WITCH_LOCATION;
   }

   public WitchRenderState createRenderState() {
      return new WitchRenderState();
   }

   public void extractRenderState(Witch p_363206_, WitchRenderState p_362711_, float p_363215_) {
      super.extractRenderState(p_363206_, p_362711_, p_363215_);
      HoldingEntityRenderState.extractHoldingEntityRenderState(p_363206_, p_362711_, this.itemModelResolver);
      p_362711_.entityId = p_363206_.getId();
      ItemStack itemstack = p_363206_.getMainHandItem();
      p_362711_.isHoldingItem = !itemstack.isEmpty();
      p_362711_.isHoldingPotion = itemstack.is(Items.POTION);
   }
}
