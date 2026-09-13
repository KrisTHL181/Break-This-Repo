package net.minecraft.util.parsing.packrat.commands;

import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.resources.Identifier;
import net.minecraft.util.parsing.packrat.DelayedException;
import net.minecraft.util.parsing.packrat.NamedRule;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.Rule;
import org.jspecify.annotations.Nullable;

public abstract class ResourceLookupRule<C, V> implements Rule<StringReader, V>, ResourceSuggestion {
   private final NamedRule<StringReader, Identifier> idParser;
   protected final C context;
   private final DelayedException<CommandSyntaxException> error;

   protected ResourceLookupRule(final NamedRule<StringReader, Identifier> idParser, final C context) {
      this.idParser = idParser;
      this.context = context;
      this.error = DelayedException.create(Identifier.ERROR_INVALID);
   }

   @Override
   public @Nullable V parse(final ParseState<StringReader> state) {
      state.input().skipWhitespace();
      int mark = state.mark();
      Identifier id = state.parse(this.idParser);
      if (id != null) {
         try {
            return this.validateElement((ImmutableStringReader)state.input(), id);
         } catch (Exception e) {
            state.errorCollector().store(mark, this, e);
            return null;
         }
      } else {
         state.errorCollector().store(mark, this, this.error);
         return null;
      }
   }

   protected abstract V validateElement(ImmutableStringReader reader, Identifier id) throws Exception;
}
