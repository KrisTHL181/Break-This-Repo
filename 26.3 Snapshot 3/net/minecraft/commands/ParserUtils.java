package net.minecraft.commands;

import com.mojang.brigadier.StringReader;
import net.minecraft.CharPredicate;

public class ParserUtils {
   public static String readWhile(final StringReader reader, final CharPredicate predicate) {
      int start = reader.getCursor();

      while (reader.canRead() && predicate.test(reader.peek())) {
         reader.skip();
      }

      return reader.getString().substring(start, reader.getCursor());
   }
}
