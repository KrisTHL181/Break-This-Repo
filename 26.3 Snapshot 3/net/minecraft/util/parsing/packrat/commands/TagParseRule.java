package net.minecraft.util.parsing.packrat.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.Rule;
import org.jspecify.annotations.Nullable;

public class TagParseRule<T> implements Rule<StringReader, Dynamic<?>> {
   private final TagParser<T> parser;

   public TagParseRule(final DynamicOps<T> ops) {
      this.parser = TagParser.create(ops);
   }

   public @Nullable Dynamic<T> parse(final ParseState<StringReader> state) {
      state.input().skipWhitespace();
      int mark = state.mark();

      try {
         return new Dynamic(this.parser.getOps(), this.parser.parseAsArgument(state.input()));
      } catch (Exception e) {
         state.errorCollector().store(mark, e);
         return null;
      }
   }
}
