package net.minecraft.client;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.ComponentRenderUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record GuiMessage(int addedTime, Component content, @Nullable MessageSignature signature, @Nullable GuiMessageTag tag) {
   private static final int MESSAGE_TAG_MARGIN_LEFT = 4;

   public List<FormattedCharSequence> splitLines(Font p_450880_, int p_458191_) {
      if (this.tag != null && this.tag.icon() != null) {
         p_458191_ -= this.tag.icon().width + 4 + 2;
      }

      return ComponentRenderUtils.wrapComponents(this.content, p_458191_, p_450880_);
   }

   @OnlyIn(Dist.CLIENT)
   public record Line(int addedTime, FormattedCharSequence content, @Nullable GuiMessageTag tag, boolean endOfEntry) {
      public int getTagIconLeft(Font p_450631_) {
         return p_450631_.width(this.content) + 4;
      }
   }
}
