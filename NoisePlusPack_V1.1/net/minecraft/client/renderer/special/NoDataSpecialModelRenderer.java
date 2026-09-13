package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface NoDataSpecialModelRenderer extends SpecialModelRenderer<Void> {
   default @Nullable Void extractArgument(ItemStack p_376871_) {
      return null;
   }

   default void submit(
      @Nullable Void p_428540_,
      ItemDisplayContext p_431397_,
      PoseStack p_428811_,
      SubmitNodeCollector p_426965_,
      int p_427481_,
      int p_429969_,
      boolean p_425861_,
      int p_431885_
   ) {
      this.submit(p_431397_, p_428811_, p_426965_, p_427481_, p_429969_, p_425861_, p_431885_);
   }

   void submit(ItemDisplayContext var1, PoseStack var2, SubmitNodeCollector var3, int var4, int var5, boolean var6, int var7);
}
