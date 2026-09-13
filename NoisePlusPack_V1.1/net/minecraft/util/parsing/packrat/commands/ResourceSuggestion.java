package net.minecraft.util.parsing.packrat.commands;

import com.mojang.brigadier.StringReader;
import java.util.stream.Stream;
import net.minecraft.resources.Identifier;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.SuggestionSupplier;

public interface ResourceSuggestion extends SuggestionSupplier<StringReader> {
   Stream<Identifier> possibleResources();

   @Override
   default Stream<String> possibleValues(ParseState<StringReader> p_334233_) {
      return this.possibleResources().map(Identifier::toString);
   }
}
