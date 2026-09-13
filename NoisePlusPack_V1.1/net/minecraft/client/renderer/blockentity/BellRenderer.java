package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.bell.BellModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BellRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class BellRenderer implements BlockEntityRenderer<BellBlockEntity, BellRenderState> {
   public static final Material BELL_TEXTURE = Sheets.BLOCK_ENTITIES_MAPPER.defaultNamespaceApply("bell/bell_body");
   private final MaterialSet materials;
   private final BellModel model;

   public BellRenderer(BlockEntityRendererProvider.Context p_173554_) {
      this.materials = p_173554_.materials();
      this.model = new BellModel(p_173554_.bakeLayer(ModelLayers.BELL));
   }

   public BellRenderState createRenderState() {
      return new BellRenderState();
   }

   public void extractRenderState(
      BellBlockEntity p_428241_, BellRenderState p_429775_, float p_425010_, Vec3 p_431645_, ModelFeatureRenderer.@Nullable CrumblingOverlay p_423572_
   ) {
      BlockEntityRenderer.super.extractRenderState(p_428241_, p_429775_, p_425010_, p_431645_, p_423572_);
      p_429775_.ticks = p_428241_.ticks + p_425010_;
      p_429775_.shakeDirection = p_428241_.shaking ? p_428241_.clickDirection : null;
   }

   public void submit(BellRenderState p_423832_, PoseStack p_427227_, SubmitNodeCollector p_429109_, CameraRenderState p_424338_) {
      BellModel.State bellmodel$state = new BellModel.State(p_423832_.ticks, p_423832_.shakeDirection);
      this.model.setupAnim(bellmodel$state);
      RenderType rendertype = BELL_TEXTURE.renderType(RenderTypes::entitySolid);
      p_429109_.submitModel(
         this.model,
         bellmodel$state,
         p_427227_,
         rendertype,
         p_423832_.lightCoords,
         OverlayTexture.NO_OVERLAY,
         -1,
         this.materials.get(BELL_TEXTURE),
         0,
         p_423832_.breakProgress
      );
   }
}
