package net.minecraft.network.chat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import org.jspecify.annotations.Nullable;

public final class Style {
   public static final Style EMPTY = new Style(null, null, null, null, null, null, null, null, null, null, null);
   public static final int NO_SHADOW = 0;
   final @Nullable TextColor color;
   final @Nullable Integer shadowColor;
   final @Nullable Boolean bold;
   final @Nullable Boolean italic;
   final @Nullable Boolean underlined;
   final @Nullable Boolean strikethrough;
   final @Nullable Boolean obfuscated;
   final @Nullable ClickEvent clickEvent;
   final @Nullable HoverEvent hoverEvent;
   final @Nullable String insertion;
   final @Nullable FontDescription font;

   private static Style create(
      Optional<TextColor> p_237258_,
      Optional<Integer> p_237259_,
      Optional<Boolean> p_237260_,
      Optional<Boolean> p_237261_,
      Optional<Boolean> p_237262_,
      Optional<Boolean> p_237263_,
      Optional<Boolean> p_237264_,
      Optional<ClickEvent> p_237265_,
      Optional<HoverEvent> p_311416_,
      Optional<String> p_312643_,
      Optional<FontDescription> p_377285_
   ) {
      Style style = new Style(
         p_237258_.orElse(null),
         p_237259_.orElse(null),
         p_237260_.orElse(null),
         p_237261_.orElse(null),
         p_237262_.orElse(null),
         p_237263_.orElse(null),
         p_237264_.orElse(null),
         p_237265_.orElse(null),
         p_311416_.orElse(null),
         p_312643_.orElse(null),
         p_377285_.orElse(null)
      );
      return style.equals(EMPTY) ? EMPTY : style;
   }

   private Style(
      @Nullable TextColor p_131113_,
      @Nullable Integer p_377243_,
      @Nullable Boolean p_131114_,
      @Nullable Boolean p_131115_,
      @Nullable Boolean p_131116_,
      @Nullable Boolean p_131117_,
      @Nullable Boolean p_131118_,
      @Nullable ClickEvent p_131119_,
      @Nullable HoverEvent p_131120_,
      @Nullable String p_131121_,
      @Nullable FontDescription p_425653_
   ) {
      this.color = p_131113_;
      this.shadowColor = p_377243_;
      this.bold = p_131114_;
      this.italic = p_131115_;
      this.underlined = p_131116_;
      this.strikethrough = p_131117_;
      this.obfuscated = p_131118_;
      this.clickEvent = p_131119_;
      this.hoverEvent = p_131120_;
      this.insertion = p_131121_;
      this.font = p_425653_;
   }

   public @Nullable TextColor getColor() {
      return this.color;
   }

   public @Nullable Integer getShadowColor() {
      return this.shadowColor;
   }

   public boolean isBold() {
      return this.bold == Boolean.TRUE;
   }

   public boolean isItalic() {
      return this.italic == Boolean.TRUE;
   }

   public boolean isStrikethrough() {
      return this.strikethrough == Boolean.TRUE;
   }

   public boolean isUnderlined() {
      return this.underlined == Boolean.TRUE;
   }

   public boolean isObfuscated() {
      return this.obfuscated == Boolean.TRUE;
   }

   public boolean isEmpty() {
      return this == EMPTY;
   }

   public @Nullable ClickEvent getClickEvent() {
      return this.clickEvent;
   }

   public @Nullable HoverEvent getHoverEvent() {
      return this.hoverEvent;
   }

   public @Nullable String getInsertion() {
      return this.insertion;
   }

   public FontDescription getFont() {
      return this.font != null ? this.font : FontDescription.DEFAULT;
   }

   private static <T> Style checkEmptyAfterChange(Style p_310345_, @Nullable T p_309931_, @Nullable T p_310845_) {
      return p_309931_ != null && p_310845_ == null && p_310345_.equals(EMPTY) ? EMPTY : p_310345_;
   }

   public Style withColor(@Nullable TextColor p_131149_) {
      return Objects.equals(this.color, p_131149_)
         ? this
         : checkEmptyAfterChange(
            new Style(
               p_131149_,
               this.shadowColor,
               this.bold,
               this.italic,
               this.underlined,
               this.strikethrough,
               this.obfuscated,
               this.clickEvent,
               this.hoverEvent,
               this.insertion,
               this.font
            ),
            this.color,
            p_131149_
         );
   }

   public Style withColor(@Nullable ChatFormatting p_131141_) {
      return this.withColor(p_131141_ != null ? TextColor.fromLegacyFormat(p_131141_) : null);
   }

   public Style withColor(int p_178521_) {
      return this.withColor(TextColor.fromRgb(p_178521_));
   }

   public Style withShadowColor(int p_378316_) {
      return Objects.equals(this.shadowColor, p_378316_)
         ? this
         : checkEmptyAfterChange(
            new Style(
               this.color,
               p_378316_,
               this.bold,
               this.italic,
               this.underlined,
               this.strikethrough,
               this.obfuscated,
               this.clickEvent,
               this.hoverEvent,
               this.insertion,
               this.font
            ),
            this.shadowColor,
            p_378316_
         );
   }

   public Style withoutShadow() {
      return this.withShadowColor(0);
   }

   public Style withBold(@Nullable Boolean p_131137_) {
      return Objects.equals(this.bold, p_131137_)
         ? this
         : checkEmptyAfterChange(
            new Style(
               this.color,
               this.shadowColor,
               p_131137_,
               this.italic,
               this.underlined,
               this.strikethrough,
               this.obfuscated,
               this.clickEvent,
               this.hoverEvent,
               this.insertion,
               this.font
            ),
            this.bold,
            p_131137_
         );
   }

   public Style withItalic(@Nullable Boolean p_131156_) {
      return Objects.equals(this.italic, p_131156_)
         ? this
         : checkEmptyAfterChange(
            new Style(
               this.color,
               this.shadowColor,
               this.bold,
               p_131156_,
               this.underlined,
               this.strikethrough,
               this.obfuscated,
               this.clickEvent,
               this.hoverEvent,
               this.insertion,
               this.font
            ),
            this.italic,
            p_131156_
         );
   }

   public Style withUnderlined(@Nullable Boolean p_131163_) {
      return Objects.equals(this.underlined, p_131163_)
         ? this
         : checkEmptyAfterChange(
            new Style(
               this.color,
               this.shadowColor,
               this.bold,
               this.italic,
               p_131163_,
               this.strikethrough,
               this.obfuscated,
               this.clickEvent,
               this.hoverEvent,
               this.insertion,
               this.font
            ),
            this.underlined,
            p_131163_
         );
   }

   public Style withStrikethrough(@Nullable Boolean p_178523_) {
      return Objects.equals(this.strikethrough, p_178523_)
         ? this
         : checkEmptyAfterChange(
            new Style(
               this.color,
               this.shadowColor,
               this.bold,
               this.italic,
               this.underlined,
               p_178523_,
               this.obfuscated,
               this.clickEvent,
               this.hoverEvent,
               this.insertion,
               this.font
            ),
            this.strikethrough,
            p_178523_
         );
   }

   public Style withObfuscated(@Nullable Boolean p_178525_) {
      return Objects.equals(this.obfuscated, p_178525_)
         ? this
         : checkEmptyAfterChange(
            new Style(
               this.color,
               this.shadowColor,
               this.bold,
               this.italic,
               this.underlined,
               this.strikethrough,
               p_178525_,
               this.clickEvent,
               this.hoverEvent,
               this.insertion,
               this.font
            ),
            this.obfuscated,
            p_178525_
         );
   }

   public Style withClickEvent(@Nullable ClickEvent p_131143_) {
      return Objects.equals(this.clickEvent, p_131143_)
         ? this
         : checkEmptyAfterChange(
            new Style(
               this.color,
               this.shadowColor,
               this.bold,
               this.italic,
               this.underlined,
               this.strikethrough,
               this.obfuscated,
               p_131143_,
               this.hoverEvent,
               this.insertion,
               this.font
            ),
            this.clickEvent,
            p_131143_
         );
   }

   public Style withHoverEvent(@Nullable HoverEvent p_131145_) {
      return Objects.equals(this.hoverEvent, p_131145_)
         ? this
         : checkEmptyAfterChange(
            new Style(
               this.color,
               this.shadowColor,
               this.bold,
               this.italic,
               this.underlined,
               this.strikethrough,
               this.obfuscated,
               this.clickEvent,
               p_131145_,
               this.insertion,
               this.font
            ),
            this.hoverEvent,
            p_131145_
         );
   }

   public Style withInsertion(@Nullable String p_131139_) {
      return Objects.equals(this.insertion, p_131139_)
         ? this
         : checkEmptyAfterChange(
            new Style(
               this.color,
               this.shadowColor,
               this.bold,
               this.italic,
               this.underlined,
               this.strikethrough,
               this.obfuscated,
               this.clickEvent,
               this.hoverEvent,
               p_131139_,
               this.font
            ),
            this.insertion,
            p_131139_
         );
   }

   public Style withFont(@Nullable FontDescription p_428403_) {
      return Objects.equals(this.font, p_428403_)
         ? this
         : checkEmptyAfterChange(
            new Style(
               this.color,
               this.shadowColor,
               this.bold,
               this.italic,
               this.underlined,
               this.strikethrough,
               this.obfuscated,
               this.clickEvent,
               this.hoverEvent,
               this.insertion,
               p_428403_
            ),
            this.font,
            p_428403_
         );
   }

   public Style applyFormat(ChatFormatting p_131158_) {
      TextColor textcolor = this.color;
      Boolean obool = this.bold;
      Boolean obool1 = this.italic;
      Boolean obool2 = this.strikethrough;
      Boolean obool3 = this.underlined;
      Boolean obool4 = this.obfuscated;
      switch (p_131158_) {
         case OBFUSCATED:
            obool4 = true;
            break;
         case BOLD:
            obool = true;
            break;
         case STRIKETHROUGH:
            obool2 = true;
            break;
         case UNDERLINE:
            obool3 = true;
            break;
         case ITALIC:
            obool1 = true;
            break;
         case RESET:
            return EMPTY;
         default:
            textcolor = TextColor.fromLegacyFormat(p_131158_);
      }

      return new Style(textcolor, this.shadowColor, obool, obool1, obool3, obool2, obool4, this.clickEvent, this.hoverEvent, this.insertion, this.font);
   }

   public Style applyLegacyFormat(ChatFormatting p_131165_) {
      TextColor textcolor = this.color;
      Boolean obool = this.bold;
      Boolean obool1 = this.italic;
      Boolean obool2 = this.strikethrough;
      Boolean obool3 = this.underlined;
      Boolean obool4 = this.obfuscated;
      switch (p_131165_) {
         case OBFUSCATED:
            obool4 = true;
            break;
         case BOLD:
            obool = true;
            break;
         case STRIKETHROUGH:
            obool2 = true;
            break;
         case UNDERLINE:
            obool3 = true;
            break;
         case ITALIC:
            obool1 = true;
            break;
         case RESET:
            return EMPTY;
         default:
            obool4 = false;
            obool = false;
            obool2 = false;
            obool3 = false;
            obool1 = false;
            textcolor = TextColor.fromLegacyFormat(p_131165_);
      }

      return new Style(textcolor, this.shadowColor, obool, obool1, obool3, obool2, obool4, this.clickEvent, this.hoverEvent, this.insertion, this.font);
   }

   public Style applyFormats(ChatFormatting... p_131153_) {
      TextColor textcolor = this.color;
      Boolean obool = this.bold;
      Boolean obool1 = this.italic;
      Boolean obool2 = this.strikethrough;
      Boolean obool3 = this.underlined;
      Boolean obool4 = this.obfuscated;

      for (ChatFormatting chatformatting : p_131153_) {
         switch (chatformatting) {
            case OBFUSCATED:
               obool4 = true;
               break;
            case BOLD:
               obool = true;
               break;
            case STRIKETHROUGH:
               obool2 = true;
               break;
            case UNDERLINE:
               obool3 = true;
               break;
            case ITALIC:
               obool1 = true;
               break;
            case RESET:
               return EMPTY;
            default:
               textcolor = TextColor.fromLegacyFormat(chatformatting);
         }
      }

      return new Style(textcolor, this.shadowColor, obool, obool1, obool3, obool2, obool4, this.clickEvent, this.hoverEvent, this.insertion, this.font);
   }

   public Style applyTo(Style p_131147_) {
      if (this == EMPTY) {
         return p_131147_;
      } else {
         return p_131147_ == EMPTY
            ? this
            : new Style(
               this.color != null ? this.color : p_131147_.color,
               this.shadowColor != null ? this.shadowColor : p_131147_.shadowColor,
               this.bold != null ? this.bold : p_131147_.bold,
               this.italic != null ? this.italic : p_131147_.italic,
               this.underlined != null ? this.underlined : p_131147_.underlined,
               this.strikethrough != null ? this.strikethrough : p_131147_.strikethrough,
               this.obfuscated != null ? this.obfuscated : p_131147_.obfuscated,
               this.clickEvent != null ? this.clickEvent : p_131147_.clickEvent,
               this.hoverEvent != null ? this.hoverEvent : p_131147_.hoverEvent,
               this.insertion != null ? this.insertion : p_131147_.insertion,
               this.font != null ? this.font : p_131147_.font
            );
      }
   }

   @Override
   public String toString() {
      final StringBuilder stringbuilder = new StringBuilder("{");

      class Collector {
         private boolean isNotFirst;

         private void prependSeparator() {
            if (this.isNotFirst) {
               stringbuilder.append(',');
            }

            this.isNotFirst = true;
         }

         void addFlagString(String p_237290_, @Nullable Boolean p_237291_) {
            if (p_237291_ != null) {
               this.prependSeparator();
               if (!p_237291_) {
                  stringbuilder.append('!');
               }

               stringbuilder.append(p_237290_);
            }
         }

         void addValueString(String p_237293_, @Nullable Object p_237294_) {
            if (p_237294_ != null) {
               this.prependSeparator();
               stringbuilder.append(p_237293_);
               stringbuilder.append('=');
               stringbuilder.append(p_237294_);
            }
         }
      }

      Collector style$1collector = new Collector();
      style$1collector.addValueString("color", this.color);
      style$1collector.addValueString("shadowColor", this.shadowColor);
      style$1collector.addFlagString("bold", this.bold);
      style$1collector.addFlagString("italic", this.italic);
      style$1collector.addFlagString("underlined", this.underlined);
      style$1collector.addFlagString("strikethrough", this.strikethrough);
      style$1collector.addFlagString("obfuscated", this.obfuscated);
      style$1collector.addValueString("clickEvent", this.clickEvent);
      style$1collector.addValueString("hoverEvent", this.hoverEvent);
      style$1collector.addValueString("insertion", this.insertion);
      style$1collector.addValueString("font", this.font);
      stringbuilder.append("}");
      return stringbuilder.toString();
   }

   @Override
   public boolean equals(Object p_131175_) {
      if (this == p_131175_) {
         return true;
      } else {
         return p_131175_ instanceof Style style
            ? this.bold == style.bold
               && Objects.equals(this.getColor(), style.getColor())
               && Objects.equals(this.getShadowColor(), style.getShadowColor())
               && this.italic == style.italic
               && this.obfuscated == style.obfuscated
               && this.strikethrough == style.strikethrough
               && this.underlined == style.underlined
               && Objects.equals(this.clickEvent, style.clickEvent)
               && Objects.equals(this.hoverEvent, style.hoverEvent)
               && Objects.equals(this.insertion, style.insertion)
               && Objects.equals(this.font, style.font)
            : false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(
         this.color,
         this.shadowColor,
         this.bold,
         this.italic,
         this.underlined,
         this.strikethrough,
         this.obfuscated,
         this.clickEvent,
         this.hoverEvent,
         this.insertion
      );
   }

   public static class Serializer {
      public static final MapCodec<Style> MAP_CODEC = RecordCodecBuilder.mapCodec(
         p_374846_ -> p_374846_.group(
               TextColor.CODEC.optionalFieldOf("color").forGetter(p_311313_ -> Optional.ofNullable(p_311313_.color)),
               ExtraCodecs.ARGB_COLOR_CODEC.optionalFieldOf("shadow_color").forGetter(p_374845_ -> Optional.ofNullable(p_374845_.shadowColor)),
               Codec.BOOL.optionalFieldOf("bold").forGetter(p_310279_ -> Optional.ofNullable(p_310279_.bold)),
               Codec.BOOL.optionalFieldOf("italic").forGetter(p_310016_ -> Optional.ofNullable(p_310016_.italic)),
               Codec.BOOL.optionalFieldOf("underlined").forGetter(p_312012_ -> Optional.ofNullable(p_312012_.underlined)),
               Codec.BOOL.optionalFieldOf("strikethrough").forGetter(p_310101_ -> Optional.ofNullable(p_310101_.strikethrough)),
               Codec.BOOL.optionalFieldOf("obfuscated").forGetter(p_310873_ -> Optional.ofNullable(p_310873_.obfuscated)),
               ClickEvent.CODEC.optionalFieldOf("click_event").forGetter(p_312594_ -> Optional.ofNullable(p_312594_.clickEvent)),
               HoverEvent.CODEC.optionalFieldOf("hover_event").forGetter(p_311111_ -> Optional.ofNullable(p_311111_.hoverEvent)),
               Codec.STRING.optionalFieldOf("insertion").forGetter(p_310639_ -> Optional.ofNullable(p_310639_.insertion)),
               FontDescription.CODEC.optionalFieldOf("font").forGetter(p_421247_ -> Optional.ofNullable(p_421247_.font))
            )
            .apply(p_374846_, Style::create)
      );
      public static final Codec<Style> CODEC = MAP_CODEC.codec();
      public static final StreamCodec<RegistryFriendlyByteBuf, Style> TRUSTED_STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistriesTrusted(CODEC);
   }
}
