package net.minecraft.commands.arguments;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.serialization.DynamicOps;
import java.util.Collection;
import java.util.List;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.SnbtGrammar;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.parsing.packrat.commands.CommandArgumentParser;
import net.minecraft.util.parsing.packrat.commands.ParserBasedArgument;

public class StyleArgument extends ParserBasedArgument<Style> {
   private static final Collection<String> EXAMPLES = List.of("{bold: true}", "{color: 'red'}", "{}");
   public static final DynamicCommandExceptionType ERROR_INVALID_STYLE = new DynamicCommandExceptionType(
      message -> Component.translatableEscape("argument.style.invalid", message)
   );
   private static final DynamicOps<Tag> OPS = NbtOps.INSTANCE;
   private static final CommandArgumentParser<Tag> TAG_PARSER = SnbtGrammar.createParser(OPS);

   private StyleArgument(final HolderLookup.Provider registries) {
      super(TAG_PARSER.withCodec(registries.createSerializationContext(OPS), TAG_PARSER, Style.Serializer.CODEC, ERROR_INVALID_STYLE));
   }

   public static Style getStyle(final CommandContext<CommandSourceStack> context, final String name) {
      return (Style)context.getArgument(name, Style.class);
   }

   public static StyleArgument style(final CommandBuildContext context) {
      return new StyleArgument(context);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}
