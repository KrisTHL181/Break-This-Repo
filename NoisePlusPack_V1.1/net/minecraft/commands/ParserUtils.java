package net.minecraft.commands;

import com.mojang.brigadier.StringReader;
import net.minecraft.CharPredicate;

public class ParserUtils {
   public static String readWhile(StringReader p_333885_, CharPredicate p_328669_) {
      int i = p_333885_.getCursor();

      while (p_333885_.canRead() && p_328669_.test(p_333885_.peek())) {
         p_333885_.skip();
      }

      return p_333885_.getString().substring(i, p_333885_.getCursor());
   }
}
