package net.minecraft.client.gui.components;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.ComponentCollector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

public class ComponentRenderUtils {
   private static final FormattedCharSequence INDENT = FormattedCharSequence.codepoint(32, Style.EMPTY);

   private static String stripColor(final String input) {
      return Minecraft.getInstance().options.chatColors().get() ? input : ChatFormatting.stripFormatting(input);
   }

   public static List<FormattedCharSequence> wrapComponents(final FormattedText message, final int maxWidth, final Font font) {
      ComponentCollector collector = new ComponentCollector();
      message.visit((style, contents) -> {
         collector.append(FormattedText.of(stripColor(contents), style));
         return Optional.empty();
      }, Style.EMPTY);
      List<FormattedCharSequence> result = Lists.newArrayList();
      font.getSplitter().splitLines(collector.getResultOrEmpty(), maxWidth, Style.EMPTY, (text, wrapped) -> {
         FormattedCharSequence reorderedText = Language.getInstance().getVisualOrder(text);
         result.add(wrapped ? FormattedCharSequence.composite(INDENT, reorderedText) : reorderedText);
      });
      return result.isEmpty() ? Lists.newArrayList(new FormattedCharSequence[]{FormattedCharSequence.EMPTY}) : result;
   }

   public static FormattedCharSequence clipText(final Component text, final Font font, final int width) {
      FormattedText clippedText = font.substrByWidth(text, width - font.width(CommonComponents.ELLIPSIS));
      return Language.getInstance().getVisualOrder(FormattedText.composite(clippedText, CommonComponents.ELLIPSIS));
   }
}
