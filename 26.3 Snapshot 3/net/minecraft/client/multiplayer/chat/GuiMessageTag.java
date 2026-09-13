package net.minecraft.client.multiplayer.chat;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public record GuiMessageTag(int indicatorColor, GuiMessageTag.@Nullable Icon icon, @Nullable Component text, @Nullable String logTag) {
   private static final Component SYSTEM_TEXT = Component.translatable("chat.tag.system");
   private static final Component SYSTEM_TEXT_SINGLE_PLAYER = Component.translatable("chat.tag.system_single_player");
   private static final Component CHAT_NOT_SECURE_TEXT = Component.translatable("chat.tag.not_secure");
   private static final Component CHAT_MODIFIED_TEXT = Component.translatable("chat.tag.modified");
   private static final Component CHAT_ERROR_TEXT = Component.translatable("chat.tag.error");
   private static final int CHAT_NOT_SECURE_INDICATOR_COLOR = 13684944;
   private static final int CHAT_MODIFIED_INDICATOR_COLOR = 6316128;
   private static final GuiMessageTag SYSTEM = new GuiMessageTag(13684944, null, SYSTEM_TEXT, "System");
   private static final GuiMessageTag SYSTEM_SINGLE_PLAYER = new GuiMessageTag(13684944, null, SYSTEM_TEXT_SINGLE_PLAYER, "System");
   private static final GuiMessageTag CHAT_NOT_SECURE = new GuiMessageTag(13684944, null, CHAT_NOT_SECURE_TEXT, "Not Secure");
   private static final GuiMessageTag CHAT_ERROR = new GuiMessageTag(16733525, null, CHAT_ERROR_TEXT, "Chat Error");

   public static GuiMessageTag system() {
      return SYSTEM;
   }

   public static GuiMessageTag systemSinglePlayer() {
      return SYSTEM_SINGLE_PLAYER;
   }

   public static GuiMessageTag chatNotSecure() {
      return CHAT_NOT_SECURE;
   }

   public static GuiMessageTag chatModified(final String originalContent) {
      Component decoratedOriginal = Component.literal(originalContent).withStyle(ChatFormatting.GRAY);
      Component text = Component.empty().append(CHAT_MODIFIED_TEXT).append(CommonComponents.NEW_LINE).append(decoratedOriginal);
      return new GuiMessageTag(6316128, GuiMessageTag.Icon.CHAT_MODIFIED, text, "Modified");
   }

   public static GuiMessageTag chatError() {
      return CHAT_ERROR;
   }

   public enum Icon {
      CHAT_MODIFIED(Identifier.withDefaultNamespace("icon/chat_modified"), 9, 9);

      public final Identifier sprite;
      public final int width;
      public final int height;

      Icon(final Identifier sprite, final int width, final int height) {
         this.sprite = sprite;
         this.width = width;
         this.height = height;
      }

      public void extractRenderState(final GuiGraphicsExtractor graphics, final int x, final int y) {
         graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.sprite, x, y, this.width, this.height);
      }
   }
}
