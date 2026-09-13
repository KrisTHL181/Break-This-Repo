package net.minecraft.client.renderer.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TextFeatureRenderer {
   public void render(SubmitNodeCollection p_424576_, MultiBufferSource.BufferSource p_429835_) {
      Font font = Minecraft.getInstance().font;

      for (SubmitNodeStorage.TextSubmit submitnodestorage$textsubmit : p_424576_.getTextSubmits()) {
         if (submitnodestorage$textsubmit.outlineColor() == 0) {
            font.drawInBatch(
               submitnodestorage$textsubmit.string(),
               submitnodestorage$textsubmit.x(),
               submitnodestorage$textsubmit.y(),
               submitnodestorage$textsubmit.color(),
               submitnodestorage$textsubmit.dropShadow(),
               submitnodestorage$textsubmit.pose(),
               p_429835_,
               submitnodestorage$textsubmit.displayMode(),
               submitnodestorage$textsubmit.backgroundColor(),
               submitnodestorage$textsubmit.lightCoords()
            );
         } else {
            font.drawInBatch8xOutline(
               submitnodestorage$textsubmit.string(),
               submitnodestorage$textsubmit.x(),
               submitnodestorage$textsubmit.y(),
               submitnodestorage$textsubmit.color(),
               submitnodestorage$textsubmit.outlineColor(),
               submitnodestorage$textsubmit.pose(),
               p_429835_,
               submitnodestorage$textsubmit.lightCoords()
            );
         }
      }
   }
}
