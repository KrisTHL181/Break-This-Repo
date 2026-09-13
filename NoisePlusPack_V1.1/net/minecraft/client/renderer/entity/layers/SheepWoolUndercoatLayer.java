package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.animal.sheep.SheepFurModel;
import net.minecraft.client.model.animal.sheep.SheepModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.SheepRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SheepWoolUndercoatLayer extends RenderLayer<SheepRenderState, SheepModel> {
   private static final Identifier SHEEP_WOOL_UNDERCOAT_LOCATION = Identifier.withDefaultNamespace("textures/entity/sheep/sheep_wool_undercoat.png");
   private final EntityModel<SheepRenderState> adultModel;
   private final EntityModel<SheepRenderState> babyModel;

   public SheepWoolUndercoatLayer(RenderLayerParent<SheepRenderState, SheepModel> p_393062_, EntityModelSet p_395881_) {
      super(p_393062_);
      this.adultModel = new SheepFurModel(p_395881_.bakeLayer(ModelLayers.SHEEP_WOOL_UNDERCOAT));
      this.babyModel = new SheepFurModel(p_395881_.bakeLayer(ModelLayers.SHEEP_BABY_WOOL_UNDERCOAT));
   }

   public void submit(PoseStack p_430869_, SubmitNodeCollector p_431199_, int p_427214_, SheepRenderState p_428673_, float p_426694_, float p_424248_) {
      if (!p_428673_.isInvisible && (p_428673_.isJebSheep || p_428673_.woolColor != DyeColor.WHITE)) {
         EntityModel<SheepRenderState> entitymodel = p_428673_.isBaby ? this.babyModel : this.adultModel;
         coloredCutoutModelCopyLayerRender(entitymodel, SHEEP_WOOL_UNDERCOAT_LOCATION, p_430869_, p_431199_, p_427214_, p_428673_, p_428673_.getWoolColor(), 1);
      }
   }
}
