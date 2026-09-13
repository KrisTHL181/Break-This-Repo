package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.statue.CopperGolemStatueModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.CopperGolemStatueRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.animal.golem.CopperGolemOxidationLevels;
import net.minecraft.world.level.block.CopperGolemStatueBlock;
import net.minecraft.world.level.block.entity.CopperGolemStatueBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class CopperGolemStatueBlockRenderer implements BlockEntityRenderer<CopperGolemStatueBlockEntity, CopperGolemStatueRenderState> {
   private final Map<CopperGolemStatueBlock.Pose, CopperGolemStatueModel> models = new HashMap<>();

   public CopperGolemStatueBlockRenderer(BlockEntityRendererProvider.Context p_430681_) {
      EntityModelSet entitymodelset = p_430681_.entityModelSet();
      this.models.put(CopperGolemStatueBlock.Pose.STANDING, new CopperGolemStatueModel(entitymodelset.bakeLayer(ModelLayers.COPPER_GOLEM)));
      this.models.put(CopperGolemStatueBlock.Pose.RUNNING, new CopperGolemStatueModel(entitymodelset.bakeLayer(ModelLayers.COPPER_GOLEM_RUNNING)));
      this.models.put(CopperGolemStatueBlock.Pose.SITTING, new CopperGolemStatueModel(entitymodelset.bakeLayer(ModelLayers.COPPER_GOLEM_SITTING)));
      this.models.put(CopperGolemStatueBlock.Pose.STAR, new CopperGolemStatueModel(entitymodelset.bakeLayer(ModelLayers.COPPER_GOLEM_STAR)));
   }

   public CopperGolemStatueRenderState createRenderState() {
      return new CopperGolemStatueRenderState();
   }

   public void extractRenderState(
      CopperGolemStatueBlockEntity p_427006_,
      CopperGolemStatueRenderState p_424118_,
      float p_428732_,
      Vec3 p_430663_,
      ModelFeatureRenderer.@Nullable CrumblingOverlay p_429014_
   ) {
      BlockEntityRenderer.super.extractRenderState(p_427006_, p_424118_, p_428732_, p_430663_, p_429014_);
      p_424118_.direction = p_427006_.getBlockState().getValue(CopperGolemStatueBlock.FACING);
      p_424118_.pose = p_427006_.getBlockState().getValue(BlockStateProperties.COPPER_GOLEM_POSE);
   }

   public void submit(CopperGolemStatueRenderState p_426466_, PoseStack p_428721_, SubmitNodeCollector p_427211_, CameraRenderState p_425393_) {
      if (p_426466_.blockState.getBlock() instanceof CopperGolemStatueBlock coppergolemstatueblock) {
         p_428721_.pushPose();
         p_428721_.translate(0.5F, 0.0F, 0.5F);
         CopperGolemStatueModel coppergolemstatuemodel = this.models.get(p_426466_.pose);
         Direction direction = p_426466_.direction;
         RenderType rendertype = RenderTypes.entityCutoutNoCull(
            CopperGolemOxidationLevels.getOxidationLevel(coppergolemstatueblock.getWeatheringState()).texture()
         );
         p_427211_.submitModel(
            coppergolemstatuemodel, direction, p_428721_, rendertype, p_426466_.lightCoords, OverlayTexture.NO_OVERLAY, 0, p_426466_.breakProgress
         );
         p_428721_.popPose();
      }
   }
}
