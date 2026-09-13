package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.animal.sheep.SheepFurModel;
import net.minecraft.client.model.animal.sheep.SheepModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.SheepRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SheepWoolLayer extends RenderLayer<SheepRenderState, SheepModel> {
   private static final Identifier SHEEP_WOOL_LOCATION = Identifier.withDefaultNamespace("textures/entity/sheep/sheep_wool.png");
   private final EntityModel<SheepRenderState> adultModel;
   private final EntityModel<SheepRenderState> babyModel;

   public SheepWoolLayer(RenderLayerParent<SheepRenderState, SheepModel> p_367510_, EntityModelSet p_367850_) {
      super(p_367510_);
      this.adultModel = new SheepFurModel(p_367850_.bakeLayer(ModelLayers.SHEEP_WOOL));
      this.babyModel = new SheepFurModel(p_367850_.bakeLayer(ModelLayers.SHEEP_BABY_WOOL));
   }

   public void submit(PoseStack p_422890_, SubmitNodeCollector p_429220_, int p_428346_, SheepRenderState p_431415_, float p_425421_, float p_426527_) {
      if (!p_431415_.isSheared) {
         EntityModel<SheepRenderState> entitymodel = p_431415_.isBaby ? this.babyModel : this.adultModel;
         if (p_431415_.isInvisible) {
            if (p_431415_.appearsGlowing()) {
               p_429220_.submitModel(
                  entitymodel,
                  p_431415_,
                  p_422890_,
                  RenderTypes.outline(SHEEP_WOOL_LOCATION),
                  p_428346_,
                  LivingEntityRenderer.getOverlayCoords(p_431415_, 0.0F),
                  -16777216,
                  null,
                  p_431415_.outlineColor,
                  null
               );
            }
         } else {
            coloredCutoutModelCopyLayerRender(entitymodel, SHEEP_WOOL_LOCATION, p_422890_, p_429220_, p_428346_, p_431415_, p_431415_.getWoolColor(), 0);
         }
      }
   }
}
