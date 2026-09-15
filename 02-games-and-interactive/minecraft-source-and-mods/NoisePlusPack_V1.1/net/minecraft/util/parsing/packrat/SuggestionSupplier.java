package net.minecraft.util.parsing.packrat;

import java.util.stream.Stream;

public interface SuggestionSupplier<S> {
   Stream<String> possibleValues(ParseState<S> var1);

   static <S> SuggestionSupplier<S> empty() {
      return p_331625_ -> Stream.empty();
   }
}
