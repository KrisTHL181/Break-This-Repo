package net.minecraft.commands.arguments.selector;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

public record SelectorPattern(String pattern, EntitySelector resolved) {
   public static final Codec<SelectorPattern> CODEC = Codec.STRING.comapFlatMap(SelectorPattern::parse, SelectorPattern::pattern);

   public static DataResult<SelectorPattern> parse(String p_366515_) {
      try {
         EntitySelectorParser entityselectorparser = new EntitySelectorParser(new StringReader(p_366515_), true);
         return DataResult.success(new SelectorPattern(p_366515_, entityselectorparser.parse()));
      } catch (CommandSyntaxException commandsyntaxexception) {
         return DataResult.error(() -> "Invalid selector component: " + p_366515_ + ": " + commandsyntaxexception.getMessage());
      }
   }

   @Override
   public boolean equals(Object p_368445_) {
      return p_368445_ instanceof SelectorPattern selectorpattern && this.pattern.equals(selectorpattern.pattern);
   }

   @Override
   public int hashCode() {
      return this.pattern.hashCode();
   }

   @Override
   public String toString() {
      return this.pattern;
   }
}
