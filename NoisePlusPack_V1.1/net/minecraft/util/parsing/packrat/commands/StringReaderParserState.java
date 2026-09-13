package net.minecraft.util.parsing.packrat.commands;

import com.mojang.brigadier.StringReader;
import net.minecraft.util.parsing.packrat.CachedParseState;
import net.minecraft.util.parsing.packrat.ErrorCollector;

public class StringReaderParserState extends CachedParseState<StringReader> {
   private final StringReader input;

   public StringReaderParserState(ErrorCollector<StringReader> p_327936_, StringReader p_332446_) {
      super(p_327936_);
      this.input = p_332446_;
   }

   public StringReader input() {
      return this.input;
   }

   @Override
   public int mark() {
      return this.input.getCursor();
   }

   @Override
   public void restore(int p_331895_) {
      this.input.setCursor(p_331895_);
   }
}
