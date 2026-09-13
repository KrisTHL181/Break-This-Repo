package net.minecraft.commands.arguments;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.serialization.DynamicOps;
import java.util.Arrays;
import java.util.Collection;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.SnbtGrammar;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.ResolutionContext;
import net.minecraft.util.parsing.packrat.commands.CommandArgumentParser;
import net.minecraft.util.parsing.packrat.commands.ParserBasedArgument;
import net.minecraft.world.entity.Entity;

public class ComponentArgument extends ParserBasedArgument<Component> {
   private static final Collection<String> EXAMPLES = Arrays.asList("\"hello world\"", "'hello world'", "\"\"", "{text:\"hello world\"}", "[\"\"]");
   public static final DynamicCommandExceptionType ERROR_INVALID_COMPONENT = new DynamicCommandExceptionType(
      message -> Component.translatableEscape("argument.component.invalid", message)
   );
   private static final DynamicOps<Tag> OPS = NbtOps.INSTANCE;
   private static final CommandArgumentParser<Tag> TAG_PARSER = SnbtGrammar.createParser(OPS);

   private ComponentArgument(final HolderLookup.Provider registries) {
      super(TAG_PARSER.withCodec(registries.createSerializationContext(OPS), TAG_PARSER, ComponentSerialization.CODEC, ERROR_INVALID_COMPONENT));
   }

   public static Component getRawComponent(final CommandContext<CommandSourceStack> context, final String name) {
      return (Component)context.getArgument(name, Component.class);
   }

   public static Component getResolvedComponent(final CommandContext<CommandSourceStack> context, final String name, final Entity contentEntity) throws CommandSyntaxException {
      return ComponentUtils.resolve(
         ResolutionContext.builder().withSource((CommandSourceStack)context.getSource()).withEntityOverride(contentEntity).build(),
         getRawComponent(context, name)
      );
   }

   public static Component getResolvedComponent(final CommandContext<CommandSourceStack> context, final String name) throws CommandSyntaxException {
      return ComponentUtils.resolve(ResolutionContext.create((CommandSourceStack)context.getSource()), getRawComponent(context, name));
   }

   public static ComponentArgument textComponent(final CommandBuildContext context) {
      return new ComponentArgument(context);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}
