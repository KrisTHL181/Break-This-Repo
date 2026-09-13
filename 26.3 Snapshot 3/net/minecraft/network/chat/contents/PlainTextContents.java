package net.minecraft.network.chat.contents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

public interface PlainTextContents extends ComponentContents {
   MapCodec<PlainTextContents> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(Codec.STRING.fieldOf("text").forGetter(PlainTextContents::text)).apply(i, PlainTextContents::create)
   );
   PlainTextContents EMPTY = new PlainTextContents() {
      @Override
      public String toString() {
         return "empty";
      }

      @Override
      public String text() {
         return "";
      }
   };

   static PlainTextContents create(final String text) {
      return text.isEmpty() ? EMPTY : new PlainTextContents.LiteralContents(text);
   }

   String text();

   @Override
   default MapCodec<PlainTextContents> codec() {
      return MAP_CODEC;
   }

   record LiteralContents(String text) implements PlainTextContents {
      @Override
      public <T> Optional<T> visit(final FormattedText.ContentConsumer<T> output) {
         return output.accept(this.text);
      }

      @Override
      public <T> Optional<T> visit(final FormattedText.StyledContentConsumer<T> output, final Style currentStyle) {
         return output.accept(currentStyle, this.text);
      }

      @Override
      public String toString() {
         return "literal{" + this.text + "}";
      }
   }
}
