package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.illager.IllagerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.EvokerRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.illager.SpellcasterIllager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EvokerRenderer<T extends SpellcasterIllager> extends IllagerRenderer<T, EvokerRenderState> {
   private static final Identifier EVOKER_ILLAGER = Identifier.withDefaultNamespace("textures/entity/illager/evoker.png");

   public EvokerRenderer(EntityRendererProvider.Context p_174108_) {
      super(p_174108_, new IllagerModel<>(p_174108_.bakeLayer(ModelLayers.EVOKER)), 0.5F);
      this.addLayer(new ItemInHandLayer<EvokerRenderState, IllagerModel<EvokerRenderState>>(this) {
         public void submit(PoseStack p_427967_, SubmitNodeCollector p_429266_, int p_424836_, EvokerRenderState p_431241_, float p_424884_, float p_425155_) {
            if (p_431241_.isCastingSpell) {
               super.submit(p_427967_, p_429266_, p_424836_, p_431241_, p_424884_, p_425155_);
            }
         }
      });
   }

   public Identifier getTextureLocation(EvokerRenderState p_362508_) {
      return EVOKER_ILLAGER;
   }

   public EvokerRenderState createRenderState() {
      return new EvokerRenderState();
   }

   public void extractRenderState(T p_458930_, EvokerRenderState p_453610_, float p_363195_) {
      super.extractRenderState(p_458930_, p_453610_, p_363195_);
      p_453610_.isCastingSpell = p_458930_.isCastingSpell();
   }
}
