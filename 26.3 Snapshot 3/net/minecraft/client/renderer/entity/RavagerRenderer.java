package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.ravager.RavagerModel;
import net.minecraft.client.renderer.entity.state.RavagerRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.Ravager;

public class RavagerRenderer extends MobRenderer<Ravager, RavagerRenderState, RavagerModel> {
   private static final Identifier TEXTURE_LOCATION = Identifier.withDefaultNamespace("textures/entity/illager/ravager.png");

   public RavagerRenderer(final EntityRendererProvider.Context context) {
      super(context, new RavagerModel(context.bakeLayer(ModelLayers.RAVAGER)), 1.1F);
   }

   public Identifier getTextureLocation(final RavagerRenderState state) {
      return TEXTURE_LOCATION;
   }

   public RavagerRenderState createRenderState() {
      return new RavagerRenderState();
   }

   public void extractRenderState(final Ravager entity, final RavagerRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.stunnedTicksRemaining = entity.getStunnedTick() > 0.0F ? entity.getStunnedTick() - partialTicks : 0.0F;
      state.attackTicksRemaining = entity.getAttackTick() > 0.0F ? entity.getAttackTick() - partialTicks : 0.0F;
      if (entity.getRoarTick() > 0) {
         state.roarAnimation = (20 - entity.getRoarTick() + partialTicks) / 20.0F;
      } else {
         state.roarAnimation = 0.0F;
      }
   }
}
