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

   public TagParseRule(DynamicOps<T> p_396246_) {
      this.parser = TagParser.create(p_396246_);
   }

   public @Nullable Dynamic<T> parse(ParseState<StringReader> p_334310_) {
      p_334310_.input().skipWhitespace();
      int i = p_334310_.mark();

      try {
         return new Dynamic(this.parser.getOps(), this.parser.parseAsArgument(p_334310_.input()));
      } catch (Exception exception) {
         p_334310_.errorCollector().store(i, exception);
         return null;
      }
   }
}
