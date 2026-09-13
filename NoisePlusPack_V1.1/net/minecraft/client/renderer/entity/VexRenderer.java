package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.vex.VexModel;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.VexRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.Vex;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class VexRenderer extends MobRenderer<Vex, VexRenderState, VexModel> {
   private static final Identifier VEX_LOCATION = Identifier.withDefaultNamespace("textures/entity/illager/vex.png");
   private static final Identifier VEX_CHARGING_LOCATION = Identifier.withDefaultNamespace("textures/entity/illager/vex_charging.png");

   public VexRenderer(EntityRendererProvider.Context p_174435_) {
      super(p_174435_, new VexModel(p_174435_.bakeLayer(ModelLayers.VEX)), 0.3F);
      this.addLayer(new ItemInHandLayer<>(this));
   }

   protected int getBlockLightLevel(Vex p_116298_, BlockPos p_116299_) {
      return 15;
   }

   public Identifier getTextureLocation(VexRenderState p_450388_) {
      return p_450388_.isCharging ? VEX_CHARGING_LOCATION : VEX_LOCATION;
   }

   public VexRenderState createRenderState() {
      return new VexRenderState();
   }

   public void extractRenderState(Vex p_361927_, VexRenderState p_368289_, float p_368523_) {
      super.extractRenderState(p_361927_, p_368289_, p_368523_);
      ArmedEntityRenderState.extractArmedEntityRenderState(p_361927_, p_368289_, this.itemModelResolver, p_368523_);
      p_368289_.isCharging = p_361927_.isCharging();
   }
}
