package net.minecraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Arrays;
import java.util.Collection;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;

public class CompoundTagArgument implements ArgumentType<CompoundTag> {
   private static final Collection<String> EXAMPLES = Arrays.asList("{}", "{foo=bar}");

   private CompoundTagArgument() {
   }

   public static CompoundTagArgument compoundTag() {
      return new CompoundTagArgument();
   }

   public static <S> CompoundTag getCompoundTag(final CommandContext<S> context, final String name) {
      return (CompoundTag)context.getArgument(name, CompoundTag.class);
   }

   public CompoundTag parse(final StringReader reader) throws CommandSyntaxException {
      return TagParser.parseCompoundAsArgument(reader);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}
