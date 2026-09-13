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
import net.minecraft.util.parsing.packrat.commands.CommandArgumentParser;
import net.minecraft.util.parsing.packrat.commands.ParserBasedArgument;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

public class ComponentArgument extends ParserBasedArgument<Component> {
   private static final Collection<String> EXAMPLES = Arrays.asList("\"hello world\"", "'hello world'", "\"\"", "{text:\"hello world\"}", "[\"\"]");
   public static final DynamicCommandExceptionType ERROR_INVALID_COMPONENT = new DynamicCommandExceptionType(
      p_308346_ -> Component.translatableEscape("argument.component.invalid", p_308346_)
   );
   private static final DynamicOps<Tag> OPS = NbtOps.INSTANCE;
   private static final CommandArgumentParser<Tag> TAG_PARSER = SnbtGrammar.createParser(OPS);

   private ComponentArgument(HolderLookup.Provider p_328965_) {
      super(TAG_PARSER.withCodec(p_328965_.createSerializationContext(OPS), TAG_PARSER, ComponentSerialization.CODEC, ERROR_INVALID_COMPONENT));
   }

   public static Component getRawComponent(CommandContext<CommandSourceStack> p_87118_, String p_87119_) {
      return (Component)p_87118_.getArgument(p_87119_, Component.class);
   }

   public static Component getResolvedComponent(CommandContext<CommandSourceStack> p_395258_, String p_396011_, @Nullable Entity p_391430_) throws CommandSyntaxException {
      return ComponentUtils.updateForEntity((CommandSourceStack)p_395258_.getSource(), getRawComponent(p_395258_, p_396011_), p_391430_, 0);
   }

   public static Component getResolvedComponent(CommandContext<CommandSourceStack> p_394442_, String p_394478_) throws CommandSyntaxException {
      return getResolvedComponent(p_394442_, p_394478_, ((CommandSourceStack)p_394442_.getSource()).getEntity());
   }

   public static ComponentArgument textComponent(CommandBuildContext p_330669_) {
      return new ComponentArgument(p_330669_);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}
