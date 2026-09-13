package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.EndGatewayRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class TheEndGatewayRenderer extends AbstractEndPortalRenderer<TheEndGatewayBlockEntity, EndGatewayRenderState> {
   private static final Identifier BEAM_LOCATION = Identifier.withDefaultNamespace("textures/entity/end_gateway_beam.png");

   public EndGatewayRenderState createRenderState() {
      return new EndGatewayRenderState();
   }

   public void extractRenderState(
      TheEndGatewayBlockEntity p_426535_,
      EndGatewayRenderState p_423236_,
      float p_423000_,
      Vec3 p_422854_,
      ModelFeatureRenderer.@Nullable CrumblingOverlay p_427129_
   ) {
      super.extractRenderState(p_426535_, p_423236_, p_423000_, p_422854_, p_427129_);
      Level level = p_426535_.getLevel();
      if (p_426535_.isSpawning() || p_426535_.isCoolingDown() && level != null) {
         p_423236_.scale = p_426535_.isSpawning() ? p_426535_.getSpawnPercent(p_423000_) : p_426535_.getCooldownPercent(p_423000_);
         double d0 = p_426535_.isSpawning() ? p_426535_.getLevel().getMaxY() : 50.0;
         p_423236_.scale = Mth.sin(p_423236_.scale * (float) Math.PI);
         p_423236_.height = Mth.floor(p_423236_.scale * d0);
         p_423236_.color = p_426535_.isSpawning() ? DyeColor.MAGENTA.getTextureDiffuseColor() : DyeColor.PURPLE.getTextureDiffuseColor();
         p_423236_.animationTime = p_426535_.getLevel() != null ? Math.floorMod(p_426535_.getLevel().getGameTime(), 40) + p_423000_ : 0.0F;
      } else {
         p_423236_.height = 0;
      }
   }

   public void submit(EndGatewayRenderState p_423878_, PoseStack p_430039_, SubmitNodeCollector p_424193_, CameraRenderState p_427717_) {
      if (p_423878_.height > 0) {
         BeaconRenderer.submitBeaconBeam(
            p_430039_,
            p_424193_,
            BEAM_LOCATION,
            p_423878_.scale,
            p_423878_.animationTime,
            -p_423878_.height,
            p_423878_.height * 2,
            p_423878_.color,
            0.15F,
            0.175F
         );
      }

      super.submit(p_423878_, p_430039_, p_424193_, p_427717_);
   }

   @Override
   protected float getOffsetUp() {
      return 1.0F;
   }

   @Override
   protected float getOffsetDown() {
      return 0.0F;
   }

   @Override
   protected RenderType renderType() {
      return RenderTypes.endGateway();
   }

   @Override
   public int getViewDistance() {
      return 256;
   }
}
