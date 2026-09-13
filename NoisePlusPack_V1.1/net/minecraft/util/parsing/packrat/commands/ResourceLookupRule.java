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

   protected ResourceLookupRule(NamedRule<StringReader, Identifier> p_397427_, C p_330414_) {
      this.idParser = p_397427_;
      this.context = p_330414_;
      this.error = DelayedException.create(Identifier.ERROR_INVALID);
   }

   @Override
   public @Nullable V parse(ParseState<StringReader> p_332578_) {
      p_332578_.input().skipWhitespace();
      int i = p_332578_.mark();
      Identifier identifier = p_332578_.parse(this.idParser);
      if (identifier != null) {
         try {
            return this.validateElement((ImmutableStringReader)p_332578_.input(), identifier);
         } catch (Exception exception) {
            p_332578_.errorCollector().store(i, this, exception);
            return null;
         }
      } else {
         p_332578_.errorCollector().store(i, this, this.error);
         return null;
      }
   }

   protected abstract V validateElement(ImmutableStringReader var1, Identifier var2) throws Exception;
}
