package net.minecraft.util.parsing.packrat;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.util.parsing.packrat.commands.StringReaderTerms;

public interface DelayedException<T extends Exception> {
   T create(String var1, int var2);

   static DelayedException<CommandSyntaxException> create(SimpleCommandExceptionType p_394319_) {
      return (p_397051_, p_397771_) -> p_394319_.createWithContext(StringReaderTerms.createReader(p_397051_, p_397771_));
   }

   static DelayedException<CommandSyntaxException> create(DynamicCommandExceptionType p_392559_, String p_394444_) {
      return (p_397984_, p_394109_) -> p_392559_.createWithContext(StringReaderTerms.createReader(p_397984_, p_394109_), p_394444_);
   }
}
