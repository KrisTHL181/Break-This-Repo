package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.AbstractChestBlock;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CopperGolemStatueBlock;
import net.minecraft.world.level.block.FlowerBedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlockDecorationLayer<S extends EntityRenderState, M extends EntityModel<S>> extends RenderLayer<S, M> {
   private final Function<S, Optional<BlockState>> blockState;
   private final Consumer<PoseStack> transform;

   public BlockDecorationLayer(RenderLayerParent<S, M> p_430837_, Function<S, Optional<BlockState>> p_431368_, Consumer<PoseStack> p_429298_) {
      super(p_430837_);
      this.blockState = p_431368_;
      this.transform = p_429298_;
   }

   @Override
   public void submit(PoseStack p_426111_, SubmitNodeCollector p_423820_, int p_428781_, S p_430741_, float p_429103_, float p_429468_) {
      Optional<BlockState> optional = this.blockState.apply(p_430741_);
      if (!optional.isEmpty()) {
         BlockState blockstate = optional.get();
         Block block = blockstate.getBlock();
         boolean flag = block instanceof CopperGolemStatueBlock;
         p_426111_.pushPose();
         this.transform.accept(p_426111_);
         if (!flag) {
            p_426111_.mulPose(Axis.ZP.rotationDegrees(180.0F));
         }

         if (flag || block instanceof AbstractSkullBlock || block instanceof AbstractBannerBlock || block instanceof AbstractChestBlock) {
            p_426111_.mulPose(Axis.YP.rotationDegrees(180.0F));
         }

         if (block instanceof FlowerBedBlock) {
            p_426111_.translate(-0.25, -1.5, -0.25);
         } else if (!flag) {
            p_426111_.translate(-0.5, -1.5, -0.5);
         } else {
            p_426111_.translate(-0.5, 0.0, -0.5);
         }

         p_423820_.submitBlock(p_426111_, blockstate, p_428781_, OverlayTexture.NO_OVERLAY, p_430741_.outlineColor);
         p_426111_.popPose();
      }
   }
}
