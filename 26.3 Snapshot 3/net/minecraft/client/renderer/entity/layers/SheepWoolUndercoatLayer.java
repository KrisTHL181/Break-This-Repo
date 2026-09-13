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

public class SheepWoolUndercoatLayer extends RenderLayer<SheepRenderState, SheepModel> {
   private static final Identifier SHEEP_WOOL_UNDERCOAT_LOCATION = Identifier.withDefaultNamespace("textures/entity/sheep/sheep_wool_undercoat.png");
   private final EntityModel<SheepRenderState> model;

   public SheepWoolUndercoatLayer(final RenderLayerParent<SheepRenderState, SheepModel> renderer, final EntityModelSet modelSet) {
      super(renderer);
      this.model = new SheepFurModel(modelSet.bakeLayer(ModelLayers.SHEEP_WOOL_UNDERCOAT));
   }

   public void submit(
      final PoseStack poseStack,
      final SubmitNodeCollector submitNodeCollector,
      final int lightCoords,
      final SheepRenderState state,
      final float yRot,
      final float xRot
   ) {
      if (!state.isInvisible && (state.isJebSheep || state.woolColor != DyeColor.WHITE) && !state.isBaby) {
         coloredCutoutModelCopyLayerRender(
            this.model, SHEEP_WOOL_UNDERCOAT_LOCATION, poseStack, submitNodeCollector, lightCoords, state, state.getWoolColor(), 1
         );
      }
   }
}
