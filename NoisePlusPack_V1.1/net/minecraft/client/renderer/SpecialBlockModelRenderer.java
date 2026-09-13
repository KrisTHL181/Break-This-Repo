package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SpecialBlockModelRenderer {
   public static final SpecialBlockModelRenderer EMPTY = new SpecialBlockModelRenderer(Map.of());
   private final Map<Block, SpecialModelRenderer<?>> renderers;

   public SpecialBlockModelRenderer(Map<Block, SpecialModelRenderer<?>> p_376544_) {
      this.renderers = p_376544_;
   }

   public static SpecialBlockModelRenderer vanilla(SpecialModelRenderer.BakingContext p_430395_) {
      return new SpecialBlockModelRenderer(SpecialModelRenderers.createBlockRenderers(p_430395_));
   }

   public void renderByBlock(
      Block p_378389_, ItemDisplayContext p_376582_, PoseStack p_377636_, SubmitNodeCollector p_429504_, int p_378039_, int p_375518_, int p_431909_
   ) {
      SpecialModelRenderer<?> specialmodelrenderer = this.renderers.get(p_378389_);
      if (specialmodelrenderer != null) {
         specialmodelrenderer.submit(null, p_376582_, p_377636_, p_429504_, p_378039_, p_375518_, false, p_431909_);
      }
   }
}
