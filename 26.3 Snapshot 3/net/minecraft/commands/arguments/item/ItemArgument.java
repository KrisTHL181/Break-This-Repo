package net.minecraft.commands.arguments.item;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandBuildContext;

public class ItemArgument implements ArgumentType<ItemInput> {
   private static final Collection<String> EXAMPLES = Arrays.asList("stick", "minecraft:stick", "stick{foo=bar}");
   private final ItemParser parser;

   public ItemArgument(final CommandBuildContext context) {
      this.parser = new ItemParser(context);
   }

   public static ItemArgument item(final CommandBuildContext context) {
      return new ItemArgument(context);
   }

   public ItemInput parse(final StringReader reader) throws CommandSyntaxException {
      return this.parser.parse(reader);
   }

   public static <S> ItemInput getItem(final CommandContext<S> context, final String name) {
      return (ItemInput)context.getArgument(name, ItemInput.class);
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
      return this.parser.fillSuggestions(builder);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}
