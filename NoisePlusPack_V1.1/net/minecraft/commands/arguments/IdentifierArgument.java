package net.minecraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Arrays;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.Identifier;

public class IdentifierArgument implements ArgumentType<Identifier> {
   private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012");

   public static IdentifierArgument id() {
      return new IdentifierArgument();
   }

   public static Identifier getId(CommandContext<CommandSourceStack> p_458057_, String p_459465_) {
      return (Identifier)p_458057_.getArgument(p_459465_, Identifier.class);
   }

   public Identifier parse(StringReader p_452815_) throws CommandSyntaxException {
      return Identifier.read(p_452815_);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}
