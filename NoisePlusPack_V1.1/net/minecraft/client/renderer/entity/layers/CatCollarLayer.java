package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.animal.feline.CatModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.CatRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CatCollarLayer extends RenderLayer<CatRenderState, CatModel> {
   private static final Identifier CAT_COLLAR_LOCATION = Identifier.withDefaultNamespace("textures/entity/cat/cat_collar.png");
   private final CatModel adultModel;
   private final CatModel babyModel;

   public CatCollarLayer(RenderLayerParent<CatRenderState, CatModel> p_174468_, EntityModelSet p_174469_) {
      super(p_174468_);
      this.adultModel = new CatModel(p_174469_.bakeLayer(ModelLayers.CAT_COLLAR));
      this.babyModel = new CatModel(p_174469_.bakeLayer(ModelLayers.CAT_BABY_COLLAR));
   }

   public void submit(PoseStack p_422849_, SubmitNodeCollector p_428242_, int p_428660_, CatRenderState p_430712_, float p_428332_, float p_426592_) {
      DyeColor dyecolor = p_430712_.collarColor;
      if (dyecolor != null) {
         int i = dyecolor.getTextureDiffuseColor();
         CatModel catmodel = p_430712_.isBaby ? this.babyModel : this.adultModel;
         coloredCutoutModelCopyLayerRender(catmodel, CAT_COLLAR_LOCATION, p_422849_, p_428242_, p_428660_, p_430712_, i, 1);
      }
   }
}
