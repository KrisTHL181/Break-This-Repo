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

   public static MutableComponent days(long p_239423_) {
      return Component.translatable("gui.days", p_239423_);
   }

   public static MutableComponent hours(long p_240042_) {
      return Component.translatable("gui.hours", p_240042_);
   }

   public static MutableComponent minutes(long p_239878_) {
      return Component.translatable("gui.minutes", p_239878_);
   }

   public static Component optionStatus(boolean p_130667_) {
      return p_130667_ ? OPTION_ON : OPTION_OFF;
   }

   public static Component disconnectButtonLabel(boolean p_406543_) {
      return p_406543_ ? GUI_RETURN_TO_MENU : GUI_DISCONNECT;
   }

   public static MutableComponent optionStatus(Component p_130664_, boolean p_130665_) {
      return Component.translatable(p_130665_ ? "options.on.composed" : "options.off.composed", p_130664_);
   }

   public static MutableComponent optionNameValue(Component p_178394_, Component p_178395_) {
      return Component.translatable("options.generic_value", p_178394_, p_178395_);
   }

   public static MutableComponent joinForNarration(Component... p_267948_) {
      MutableComponent mutablecomponent = Component.empty();

      for (int i = 0; i < p_267948_.length; i++) {
         mutablecomponent.append(p_267948_[i]);
         if (i != p_267948_.length - 1) {
            mutablecomponent.append(NARRATION_SEPARATOR);
         }
      }

      return mutablecomponent;
   }

   public static Component joinLines(Component... p_178397_) {
      return joinLines(Arrays.asList(p_178397_));
   }

   public static Component joinLines(Collection<? extends Component> p_178392_) {
      return ComponentUtils.formatList(p_178392_, NEW_LINE);
   }
}
