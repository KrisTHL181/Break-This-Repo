package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.CampfireRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class CampfireRenderer implements BlockEntityRenderer<CampfireBlockEntity, CampfireRenderState> {
   private static final float SIZE = 0.375F;
   private final ItemModelResolver itemModelResolver;

   public CampfireRenderer(BlockEntityRendererProvider.Context p_173602_) {
      this.itemModelResolver = p_173602_.itemModelResolver();
   }

   public CampfireRenderState createRenderState() {
      return new CampfireRenderState();
   }

   public void extractRenderState(
      CampfireBlockEntity p_430919_, CampfireRenderState p_428295_, float p_422950_, Vec3 p_429555_, ModelFeatureRenderer.@Nullable CrumblingOverlay p_431013_
   ) {
      BlockEntityRenderer.super.extractRenderState(p_430919_, p_428295_, p_422950_, p_429555_, p_431013_);
      p_428295_.facing = p_430919_.getBlockState().getValue(CampfireBlock.FACING);
      int i = (int)p_430919_.getBlockPos().asLong();
      p_428295_.items = new ArrayList<>();

      for (int j = 0; j < p_430919_.getItems().size(); j++) {
         ItemStackRenderState itemstackrenderstate = new ItemStackRenderState();
         this.itemModelResolver
            .updateForTopItem(itemstackrenderstate, p_430919_.getItems().get(j), ItemDisplayContext.FIXED, p_430919_.getLevel(), null, i + j);
         p_428295_.items.add(itemstackrenderstate);
      }
   }

   public void submit(CampfireRenderState p_425058_, PoseStack p_431665_, SubmitNodeCollector p_430223_, CameraRenderState p_424327_) {
      Direction direction = p_425058_.facing;
      List<ItemStackRenderState> list = p_425058_.items;

      for (int i = 0; i < list.size(); i++) {
         ItemStackRenderState itemstackrenderstate = list.get(i);
         if (!itemstackrenderstate.isEmpty()) {
            p_431665_.pushPose();
            p_431665_.translate(0.5F, 0.44921875F, 0.5F);
            Direction direction1 = Direction.from2DDataValue((i + direction.get2DDataValue()) % 4);
            float f = -direction1.toYRot();
            p_431665_.mulPose(Axis.YP.rotationDegrees(f));
            p_431665_.mulPose(Axis.XP.rotationDegrees(90.0F));
            p_431665_.translate(-0.3125F, -0.3125F, 0.0F);
            p_431665_.scale(0.375F, 0.375F, 0.375F);
            itemstackrenderstate.submit(p_431665_, p_430223_, p_425058_.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            p_431665_.popPose();
         }
      }
   }
}
