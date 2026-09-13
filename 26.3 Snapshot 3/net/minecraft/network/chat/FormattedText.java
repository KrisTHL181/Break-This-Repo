package net.minecraft.network.chat;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import net.minecraft.util.Unit;

public interface FormattedText {
   Optional<Unit> STOP_ITERATION = Optional.of(Unit.INSTANCE);
   FormattedText EMPTY = new FormattedText() {
      @Override
      public <T> Optional<T> visit(final FormattedText.ContentConsumer<T> output) {
         return Optional.empty();
      }

      @Override
      public <T> Optional<T> visit(final FormattedText.StyledContentConsumer<T> output, final Style parentStyle) {
         return Optional.empty();
      }
   };

   <T> Optional<T> visit(final FormattedText.ContentConsumer<T> output);

   <T> Optional<T> visit(final FormattedText.StyledContentConsumer<T> output, final Style parentStyle);

   static FormattedText of(final String text) {
      return new FormattedText() {
         @Override
         public <T> Optional<T> visit(final FormattedText.ContentConsumer<T> output) {
            return output.accept(text);
         }

         @Override
         public <T> Optional<T> visit(final FormattedText.StyledContentConsumer<T> output, final Style parentStyle) {
            return output.accept(parentStyle, text);
         }
      };
   }

   static FormattedText of(final String text, final Style style) {
      return new FormattedText() {
         @Override
         public <T> Optional<T> visit(final FormattedText.ContentConsumer<T> output) {
            return output.accept(text);
         }

         @Override
         public <T> Optional<T> visit(final FormattedText.StyledContentConsumer<T> output, final Style parentStyle) {
            return output.accept(style.applyTo(parentStyle), text);
         }
      };
   }

   static FormattedText composite(final FormattedText... parts) {
      return composite(ImmutableList.copyOf(parts));
   }

   static FormattedText composite(final List<? extends FormattedText> parts) {
      return new FormattedText() {
         @Override
         public <T> Optional<T> visit(final FormattedText.ContentConsumer<T> output) {
            for (FormattedText part : parts) {
               Optional<T> result = part.visit(output);
               if (result.isPresent()) {
                  return result;
               }
            }

            return Optional.empty();
         }

         @Override
         public <T> Optional<T> visit(final FormattedText.StyledContentConsumer<T> output, final Style parentStyle) {
            for (FormattedText part : parts) {
               Optional<T> result = part.visit(output, parentStyle);
               if (result.isPresent()) {
                  return result;
               }
            }

            return Optional.empty();
         }
      };
   }

   default String getString() {
      StringBuilder builder = new StringBuilder();
      this.visit(contents -> {
         builder.append(contents);
         return Optional.empty();
      });
      return builder.toString();
   }

   interface ContentConsumer<T> {
      Optional<T> accept(final String contents);
   }

   interface StyledContentConsumer<T> {
      Optional<T> accept(final Style style, final String contents);
   }
}
