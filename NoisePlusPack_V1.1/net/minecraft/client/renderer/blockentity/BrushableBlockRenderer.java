package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BrushableBlockRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class BrushableBlockRenderer implements BlockEntityRenderer<BrushableBlockEntity, BrushableBlockRenderState> {
   private final ItemModelResolver itemModelResolver;

   public BrushableBlockRenderer(BlockEntityRendererProvider.Context p_277899_) {
      this.itemModelResolver = p_277899_.itemModelResolver();
   }

   public BrushableBlockRenderState createRenderState() {
      return new BrushableBlockRenderState();
   }

   public void extractRenderState(
      BrushableBlockEntity p_428685_,
      BrushableBlockRenderState p_424043_,
      float p_424275_,
      Vec3 p_426205_,
      ModelFeatureRenderer.@Nullable CrumblingOverlay p_424438_
   ) {
      BlockEntityRenderer.super.extractRenderState(p_428685_, p_424043_, p_424275_, p_426205_, p_424438_);
      p_424043_.hitDirection = p_428685_.getHitDirection();
      p_424043_.dustProgress = p_428685_.getBlockState().getValue(BlockStateProperties.DUSTED);
      if (p_428685_.getLevel() != null && p_428685_.getHitDirection() != null) {
         p_424043_.lightCoords = LevelRenderer.getLightColor(
            LevelRenderer.BrightnessGetter.DEFAULT,
            p_428685_.getLevel(),
            p_428685_.getBlockState(),
            p_428685_.getBlockPos().relative(p_428685_.getHitDirection())
         );
      }

      this.itemModelResolver.updateForTopItem(p_424043_.itemState, p_428685_.getItem(), ItemDisplayContext.FIXED, p_428685_.getLevel(), null, 0);
   }

   public void submit(BrushableBlockRenderState p_422561_, PoseStack p_427617_, SubmitNodeCollector p_426677_, CameraRenderState p_423249_) {
      if (p_422561_.dustProgress > 0 && p_422561_.hitDirection != null && !p_422561_.itemState.isEmpty()) {
         p_427617_.pushPose();
         p_427617_.translate(0.0F, 0.5F, 0.0F);
         float[] afloat = this.translations(p_422561_.hitDirection, p_422561_.dustProgress);
         p_427617_.translate(afloat[0], afloat[1], afloat[2]);
         p_427617_.mulPose(Axis.YP.rotationDegrees(75.0F));
         boolean flag = p_422561_.hitDirection == Direction.EAST || p_422561_.hitDirection == Direction.WEST;
         p_427617_.mulPose(Axis.YP.rotationDegrees((flag ? 90 : 0) + 11));
         p_427617_.scale(0.5F, 0.5F, 0.5F);
         p_422561_.itemState.submit(p_427617_, p_426677_, p_422561_.lightCoords, OverlayTexture.NO_OVERLAY, 0);
         p_427617_.popPose();
      }
   }

   private float[] translations(Direction p_278030_, int p_277997_) {
      float[] afloat = new float[]{0.5F, 0.0F, 0.5F};
      float f = p_277997_ / 10.0F * 0.75F;
      switch (p_278030_) {
         case EAST:
            afloat[0] = 0.73F + f;
            break;
         case WEST:
            afloat[0] = 0.25F - f;
            break;
         case UP:
            afloat[1] = 0.25F + f;
            break;
         case DOWN:
            afloat[1] = -0.23F - f;
            break;
         case NORTH:
            afloat[2] = 0.25F - f;
            break;
         case SOUTH:
            afloat[2] = 0.73F + f;
      }

      return afloat;
   }
}
