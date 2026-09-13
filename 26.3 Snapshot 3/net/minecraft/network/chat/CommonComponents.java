package net.minecraft.network.chat;

import java.util.Arrays;
import java.util.Collection;

public class CommonComponents {
   public static final Component EMPTY = Component.empty();
   public static final Component OPTION_ON = Component.translatable("options.on");
   public static final Component OPTION_OFF = Component.translatable("options.off");
   public static final Component GUI_DONE = Component.translatable("gui.done");
   public static final Component GUI_CANCEL = Component.translatable("gui.cancel");
   public static final Component GUI_YES = Component.translatable("gui.yes");
   public static final Component GUI_REMOVE = Component.translatable("gui.remove");
   public static final Component GUI_NO = Component.translatable("gui.no");
   public static final Component GUI_OK = Component.translatable("gui.ok");
   public static final Component GUI_PROCEED = Component.translatable("gui.proceed");
   public static final Component GUI_CONTINUE = Component.translatable("gui.continue");
   public static final Component GUI_BACK = Component.translatable("gui.back");
   public static final Component GUI_TO_TITLE = Component.translatable("gui.toTitle");
   public static final Component GUI_ACKNOWLEDGE = Component.translatable("gui.acknowledge");
   public static final Component GUI_OPEN_IN_BROWSER = Component.translatable("chat.link.open");
   public static final Component GUI_COPY_TO_CLIPBOARD = Component.translatable("chat.copy");
   public static final Component GUI_COPY_LINK_TO_CLIPBOARD = Component.translatable("gui.copy_link_to_clipboard");
   public static final Component GUI_DISCONNECT = Component.translatable("menu.disconnect");
   public static final Component GUI_RETURN_TO_MENU = Component.translatable("menu.returnToMenu");
   public static final Component TRANSFER_CONNECT_FAILED = Component.translatable("connect.failed.transfer");
   public static final Component CONNECT_FAILED = Component.translatable("connect.failed");
   public static final Component NEW_LINE = Component.literal("\n");
   public static final Component NARRATION_SEPARATOR = Component.literal(". ");
   public static final Component ELLIPSIS = Component.literal("...");
   public static final Component SPACE = space();

   public static MutableComponent space() {
      return Component.literal(" ");
   }

   public static MutableComponent days(final long value) {
      return Component.translatable("gui.days", value);
   }

   public static MutableComponent hours(final long value) {
      return Component.translatable("gui.hours", value);
   }

   public static MutableComponent minutes(final long value) {
      return Component.translatable("gui.minutes", value);
   }

   public static Component optionStatus(final boolean value) {
      return value ? OPTION_ON : OPTION_OFF;
   }

   public static Component disconnectButtonLabel(final boolean isLocalServer) {
      return isLocalServer ? GUI_RETURN_TO_MENU : GUI_DISCONNECT;
   }

   public static MutableComponent optionStatus(final Component name, final boolean value) {
      return Component.translatable(value ? "options.on.composed" : "options.off.composed", name);
   }

   public static MutableComponent optionNameValue(final Component name, final Component value) {
      return Component.translatable("options.generic_value", name, value);
   }

   public static MutableComponent joinForNarration(final Component... components) {
      MutableComponent result = Component.empty();

      for (int i = 0; i < components.length; i++) {
         result.append(components[i]);
         if (i != components.length - 1) {
            result.append(NARRATION_SEPARATOR);
         }
      }

      return result;
   }

   public static Component joinLines(final Component... lines) {
      return joinLines(Arrays.asList(lines));
   }

   public static Component joinLines(final Collection<? extends Component> lines) {
      return ComponentUtils.formatList(lines, NEW_LINE);
   }
}
