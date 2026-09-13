package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Function;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LivingEntityEmissiveLayer<S extends LivingEntityRenderState, M extends EntityModel<S>> extends RenderLayer<S, M> {
   private final Function<S, Identifier> textureProvider;
   private final LivingEntityEmissiveLayer.AlphaFunction<S> alphaFunction;
   private final M model;
   private final Function<Identifier, RenderType> bufferProvider;
   private final boolean alwaysVisible;

   public LivingEntityEmissiveLayer(
      RenderLayerParent<S, M> p_366651_,
      Function<S, Identifier> p_360840_,
      LivingEntityEmissiveLayer.AlphaFunction<S> p_362758_,
      M p_427480_,
      Function<Identifier, RenderType> p_426132_,
      boolean p_375805_
   ) {
      super(p_366651_);
      this.textureProvider = p_360840_;
      this.alphaFunction = p_362758_;
      this.model = p_427480_;
      this.bufferProvider = p_426132_;
      this.alwaysVisible = p_375805_;
   }

   public void submit(PoseStack p_430251_, SubmitNodeCollector p_422889_, int p_428856_, S p_426995_, float p_431132_, float p_431006_) {
      if (!p_426995_.isInvisible || this.alwaysVisible) {
         float f = this.alphaFunction.apply(p_426995_, p_426995_.ageInTicks);
         if (!(f <= 1.0E-5F)) {
            int i = ARGB.white(f);
            RenderType rendertype = this.bufferProvider.apply(this.textureProvider.apply(p_426995_));
            p_422889_.order(1)
               .submitModel(
                  this.model,
                  p_426995_,
                  p_430251_,
                  rendertype,
                  p_428856_,
                  LivingEntityRenderer.getOverlayCoords(p_426995_, 0.0F),
                  i,
                  null,
                  p_426995_.outlineColor,
                  null
               );
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public interface AlphaFunction<S extends LivingEntityRenderState> {
      float apply(S var1, float var2);
   }
}
