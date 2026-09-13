package net.minecraft.world.level.block.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.Style;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import org.jspecify.annotations.Nullable;

public class SignText {
   private static final Codec<Component[]> LINES_CODEC = ComponentSerialization.CODEC
      .listOf()
      .comapFlatMap(
         input -> Util.fixedSize(input, 4)
            .map(
               components -> new Component[]{
                  (Component)components.get(0), (Component)components.get(1), (Component)components.get(2), (Component)components.get(3)
               }
            ),
         components -> List.of(components[0], components[1], components[2], components[3])
      );
   public static final Codec<SignText> DIRECT_CODEC = RecordCodecBuilder.create(
      i -> i.group(
            LINES_CODEC.fieldOf("messages").forGetter(o -> o.messages),
            LINES_CODEC.lenientOptionalFieldOf("filtered_messages").forGetter(SignText::filteredMessages),
            ExtraCodecs.optionalAlwaysPresentFieldOf(DyeColor.CODEC, "color", DyeColor.BLACK).forGetter(o -> o.color),
            ExtraCodecs.optionalAlwaysPresentFieldOf(Codec.BOOL, "has_glowing_text", false).forGetter(o -> o.hasGlowingText)
         )
         .apply(i, SignText::load)
   );
   public static final int LINES = 4;
   private final Component[] messages;
   private final Component[] filteredMessages;
   private final DyeColor color;
   private final boolean hasGlowingText;
   private FormattedCharSequence @Nullable [] renderMessages;
   private boolean renderMessagedFiltered;

   public SignText() {
      this(emptyMessages(), emptyMessages(), DyeColor.BLACK, false);
   }

   public SignText(final Component[] messages, final Component[] filteredMessages, final DyeColor color, final boolean hasGlowingText) {
      this.messages = messages;
      this.filteredMessages = filteredMessages;
      this.color = color;
      this.hasGlowingText = hasGlowingText;
   }

   private static Component[] emptyMessages() {
      return new Component[]{CommonComponents.EMPTY, CommonComponents.EMPTY, CommonComponents.EMPTY, CommonComponents.EMPTY};
   }

   private static SignText load(final Component[] messages, final Optional<Component[]> filteredMessages, final DyeColor color, final boolean hasGlowingText) {
      return new SignText(messages, filteredMessages.orElse(Arrays.copyOf(messages, messages.length)), color, hasGlowingText);
   }

   public boolean hasGlowingText() {
      return this.hasGlowingText;
   }

   public SignText setHasGlowingText(final boolean hasGlowingText) {
      return hasGlowingText == this.hasGlowingText ? this : new SignText(this.messages, this.filteredMessages, this.color, hasGlowingText);
   }

   public DyeColor getColor() {
      return this.color;
   }

   public SignText setColor(final DyeColor color) {
      return color == this.getColor() ? this : new SignText(this.messages, this.filteredMessages, color, this.hasGlowingText);
   }

   public Component getMessage(final int index, final boolean shouldFilter) {
      return this.getMessages(shouldFilter)[index];
   }

   public SignText setMessage(final int index, final Component message) {
      return this.setMessage(index, message, message);
   }

   public SignText setMessage(final int index, final Component rawMessage, final Component filteredMessage) {
      Component[] messages = Arrays.copyOf(this.messages, this.messages.length);
      Component[] filteredMessages = Arrays.copyOf(this.filteredMessages, this.filteredMessages.length);
      messages[index] = rawMessage;
      filteredMessages[index] = filteredMessage;
      return new SignText(messages, filteredMessages, this.color, this.hasGlowingText);
   }

   public boolean hasMessage(final Player player) {
      return Arrays.stream(this.getMessages(player.isTextFilteringEnabled())).anyMatch(component -> !component.getString().isEmpty());
   }

   public Component[] getMessages(final boolean shouldFilter) {
      return shouldFilter ? this.filteredMessages : this.messages;
   }

   public FormattedCharSequence[] getRenderMessages(final boolean shouldFilter, final Function<Component, FormattedCharSequence> prepare) {
      if (this.renderMessages == null || this.renderMessagedFiltered != shouldFilter) {
         this.renderMessagedFiltered = shouldFilter;
         this.renderMessages = new FormattedCharSequence[4];

         for (int i = 0; i < 4; i++) {
            this.renderMessages[i] = prepare.apply(this.getMessage(i, shouldFilter));
         }
      }

      return this.renderMessages;
   }

   private Optional<Component[]> filteredMessages() {
      for (int i = 0; i < 4; i++) {
         if (!this.filteredMessages[i].equals(this.messages[i])) {
            return Optional.of(this.filteredMessages);
         }
      }

      return Optional.empty();
   }

   public boolean hasAnyClickCommands(final Player player) {
      for (Component message : this.getMessages(player.isTextFilteringEnabled())) {
         Style style = message.getStyle();
         ClickEvent event = style.getClickEvent();
         if (event != null && event.action() == ClickEvent.Action.RUN_COMMAND) {
            return true;
         }
      }

      return false;
   }
}
