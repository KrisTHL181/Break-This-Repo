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
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import org.jspecify.annotations.Nullable;

public class SignText {
   private static final Codec<Component[]> LINES_CODEC = ComponentSerialization.CODEC
      .listOf()
      .comapFlatMap(
         p_449927_ -> Util.fixedSize(p_449927_, 4)
            .map(
               p_277881_ -> new Component[]{(Component)p_277881_.get(0), (Component)p_277881_.get(1), (Component)p_277881_.get(2), (Component)p_277881_.get(3)}
            ),
         p_277460_ -> List.of(p_277460_[0], p_277460_[1], p_277460_[2], p_277460_[3])
      );
   public static final Codec<SignText> DIRECT_CODEC = RecordCodecBuilder.create(
      p_327313_ -> p_327313_.group(
            LINES_CODEC.fieldOf("messages").forGetter(p_277822_ -> p_277822_.messages),
            LINES_CODEC.lenientOptionalFieldOf("filtered_messages").forGetter(SignText::filteredMessages),
            DyeColor.CODEC.fieldOf("color").orElse(DyeColor.BLACK).forGetter(p_277343_ -> p_277343_.color),
            Codec.BOOL.fieldOf("has_glowing_text").orElse(false).forGetter(p_277555_ -> p_277555_.hasGlowingText)
         )
         .apply(p_327313_, SignText::load)
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

   public SignText(Component[] p_277506_, Component[] p_277908_, DyeColor p_277883_, boolean p_278091_) {
      this.messages = p_277506_;
      this.filteredMessages = p_277908_;
      this.color = p_277883_;
      this.hasGlowingText = p_278091_;
   }

   private static Component[] emptyMessages() {
      return new Component[]{CommonComponents.EMPTY, CommonComponents.EMPTY, CommonComponents.EMPTY, CommonComponents.EMPTY};
   }

   private static SignText load(Component[] p_277661_, Optional<Component[]> p_277768_, DyeColor p_277345_, boolean p_278008_) {
      return new SignText(p_277661_, p_277768_.orElse(Arrays.copyOf(p_277661_, p_277661_.length)), p_277345_, p_278008_);
   }

   public boolean hasGlowingText() {
      return this.hasGlowingText;
   }

   public SignText setHasGlowingText(boolean p_277953_) {
      return p_277953_ == this.hasGlowingText ? this : new SignText(this.messages, this.filteredMessages, this.color, p_277953_);
   }

   public DyeColor getColor() {
      return this.color;
   }

   public SignText setColor(DyeColor p_277507_) {
      return p_277507_ == this.getColor() ? this : new SignText(this.messages, this.filteredMessages, p_277507_, this.hasGlowingText);
   }

   public Component getMessage(int p_277404_, boolean p_278108_) {
      return this.getMessages(p_278108_)[p_277404_];
   }

   public SignText setMessage(int p_277878_, Component p_277360_) {
      return this.setMessage(p_277878_, p_277360_, p_277360_);
   }

   public SignText setMessage(int p_277690_, Component p_277852_, Component p_277564_) {
      Component[] acomponent = Arrays.copyOf(this.messages, this.messages.length);
      Component[] acomponent1 = Arrays.copyOf(this.filteredMessages, this.filteredMessages.length);
      acomponent[p_277690_] = p_277852_;
      acomponent1[p_277690_] = p_277564_;
      return new SignText(acomponent, acomponent1, this.color, this.hasGlowingText);
   }

   public boolean hasMessage(Player p_277764_) {
      return Arrays.stream(this.getMessages(p_277764_.isTextFilteringEnabled())).anyMatch(p_277499_ -> !p_277499_.getString().isEmpty());
   }

   public Component[] getMessages(boolean p_277992_) {
      return p_277992_ ? this.filteredMessages : this.messages;
   }

   public FormattedCharSequence[] getRenderMessages(boolean p_277336_, Function<Component, FormattedCharSequence> p_277538_) {
      if (this.renderMessages == null || this.renderMessagedFiltered != p_277336_) {
         this.renderMessagedFiltered = p_277336_;
         this.renderMessages = new FormattedCharSequence[4];

         for (int i = 0; i < 4; i++) {
            this.renderMessages[i] = p_277538_.apply(this.getMessage(i, p_277336_));
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

   public boolean hasAnyClickCommands(Player p_277865_) {
      for (Component component : this.getMessages(p_277865_.isTextFilteringEnabled())) {
         Style style = component.getStyle();
         ClickEvent clickevent = style.getClickEvent();
         if (clickevent != null && clickevent.action() == ClickEvent.Action.RUN_COMMAND) {
            return true;
         }
      }

      return false;
   }
}
