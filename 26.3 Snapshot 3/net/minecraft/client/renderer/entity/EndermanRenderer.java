package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.enderman.EndermanModel;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.layers.CarriedBlockLayer;
import net.minecraft.client.renderer.entity.layers.EnderEyesLayer;
import net.minecraft.client.renderer.entity.state.EndermanRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class EndermanRenderer extends MobRenderer<EnderMan, EndermanRenderState, EndermanModel<EndermanRenderState>> {
   public static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT = BlockDisplayContext.create();
   private static final Identifier ENDERMAN_LOCATION = Identifier.withDefaultNamespace("textures/entity/enderman/enderman.png");
   private final RandomSource random = RandomSource.create();
   private final BlockModelResolver blockModelResolver;

   public EndermanRenderer(final EntityRendererProvider.Context context) {
      super(context, new EndermanModel<>(context.bakeLayer(ModelLayers.ENDERMAN)), 0.5F);
      this.blockModelResolver = context.getBlockModelResolver();
      this.addLayer(new EnderEyesLayer(this));
      this.addLayer(new CarriedBlockLayer(this));
   }

   public Vec3 getRenderOffset(final EndermanRenderState state) {
      Vec3 offset = super.getRenderOffset(state);
      if (state.isCreepy) {
         double d = 0.02 * state.scale;
         return offset.add(this.random.nextGaussian() * d, 0.0, this.random.nextGaussian() * d);
      } else {
         return offset;
      }
   }

   public Identifier getTextureLocation(final EndermanRenderState state) {
      return ENDERMAN_LOCATION;
   }

   public EndermanRenderState createRenderState() {
      return new EndermanRenderState();
   }

   public void extractRenderState(final EnderMan entity, final EndermanRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      HumanoidMobRenderer.extractHumanoidRenderState(entity, state, partialTicks, this.itemModelResolver);
      state.isCreepy = entity.isCreepy();
      BlockState carriedBlock = entity.getCarriedBlock();
      if (carriedBlock != null) {
         this.blockModelResolver.update(state.carriedBlock, carriedBlock, BLOCK_DISPLAY_CONTEXT);
      } else {
         state.carriedBlock.clear();
      }
   }
}
