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

public class WitchRenderer extends MobRenderer<Witch, WitchRenderState, WitchModel> {
   private static final Identifier WITCH_LOCATION = Identifier.withDefaultNamespace("textures/entity/witch/witch.png");

   public WitchRenderer(final EntityRendererProvider.Context context) {
      super(context, new WitchModel(context.bakeLayer(ModelLayers.WITCH)), 0.5F);
      this.addLayer(new WitchItemLayer(this));
   }

   public Identifier getTextureLocation(final WitchRenderState state) {
      return WITCH_LOCATION;
   }

   public WitchRenderState createRenderState() {
      return new WitchRenderState();
   }

   public void extractRenderState(final Witch entity, final WitchRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      HoldingEntityRenderState.extractHoldingEntityRenderState(entity, state, this.itemModelResolver);
      state.entityId = entity.getId();
      ItemStack mainHandItem = entity.getMainHandItem();
      state.isHoldingItem = !mainHandItem.isEmpty();
      state.isHoldingPotion = mainHandItem.is(Items.POTION);
   }
}
