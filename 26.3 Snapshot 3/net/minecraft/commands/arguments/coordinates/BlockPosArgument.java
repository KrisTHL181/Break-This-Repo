package net.minecraft.commands.arguments.coordinates;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class BlockPosArgument implements ArgumentType<Coordinates> {
   private static final Collection<String> EXAMPLES = Arrays.asList("0 0 0", "~ ~ ~", "^ ^ ^", "^1 ^ ^-5", "~0.5 ~1 ~-5");
   public static final SimpleCommandExceptionType ERROR_NOT_LOADED = new SimpleCommandExceptionType(Component.translatable("argument.pos.unloaded"));
   public static final SimpleCommandExceptionType ERROR_OUT_OF_WORLD = new SimpleCommandExceptionType(Component.translatable("argument.pos.outofworld"));
   public static final SimpleCommandExceptionType ERROR_OUT_OF_BOUNDS = new SimpleCommandExceptionType(Component.translatable("argument.pos.outofbounds"));

   public static BlockPosArgument blockPos() {
      return new BlockPosArgument();
   }

   public static BlockPos getLoadedBlockPos(final CommandContext<CommandSourceStack> context, final String name) throws CommandSyntaxException {
      ServerLevel level = ((CommandSourceStack)context.getSource()).getLevel();
      return getLoadedBlockPos(context, level, name);
   }

   public static BlockPos getLoadedBlockPos(final CommandContext<CommandSourceStack> context, final ServerLevel level, final String name) throws CommandSyntaxException {
      BlockPos pos = getBlockPos(context, name);
      if (!level.hasChunkAt(pos)) {
         throw ERROR_NOT_LOADED.create();
      } else if (!level.isInWorldBounds(pos)) {
         throw ERROR_OUT_OF_WORLD.create();
      } else {
         return pos;
      }
   }

   public static BlockPos getBlockPos(final CommandContext<CommandSourceStack> context, final String name) {
      return ((Coordinates)context.getArgument(name, Coordinates.class)).getBlockPos((CommandSourceStack)context.getSource());
   }

   public static BlockPos getSpawnablePos(final CommandContext<CommandSourceStack> context, final String name) throws CommandSyntaxException {
      BlockPos pos = getBlockPos(context, name);
      if (!Level.isInSpawnableBounds(pos)) {
         throw ERROR_OUT_OF_BOUNDS.create();
      } else {
         return pos;
      }
   }

   public Coordinates parse(final StringReader reader) throws CommandSyntaxException {
      return reader.canRead() && reader.peek() == '^' ? LocalCoordinates.parse(reader) : WorldCoordinates.parseInt(reader);
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
      if (!(context.getSource() instanceof SharedSuggestionProvider)) {
         return Suggestions.empty();
      }

      String remainder = builder.getRemaining();
      Collection<SharedSuggestionProvider.TextCoordinates> suggestedCoordinates;
      if (!remainder.isEmpty() && remainder.charAt(0) == '^') {
         suggestedCoordinates = Collections.singleton(SharedSuggestionProvider.TextCoordinates.DEFAULT_LOCAL);
      } else {
         suggestedCoordinates = ((SharedSuggestionProvider)context.getSource()).getRelevantCoordinates();
      }

      return SharedSuggestionProvider.suggestCoordinates(remainder, suggestedCoordinates, builder, Commands.createValidator(this::parse));
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}
