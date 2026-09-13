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
import net.minecraft.server.level.ColumnPos;

public class ColumnPosArgument implements ArgumentType<Coordinates> {
   private static final Collection<String> EXAMPLES = Arrays.asList("0 0", "~ ~", "~1 ~-2", "^ ^", "^-1 ^0");
   public static final SimpleCommandExceptionType ERROR_NOT_COMPLETE = new SimpleCommandExceptionType(Component.translatable("argument.pos2d.incomplete"));

   public static ColumnPosArgument columnPos() {
      return new ColumnPosArgument();
   }

   public static ColumnPos getColumnPos(final CommandContext<CommandSourceStack> context, final String name) {
      BlockPos pos = ((Coordinates)context.getArgument(name, Coordinates.class)).getBlockPos((CommandSourceStack)context.getSource());
      return new ColumnPos(pos.getX(), pos.getZ());
   }

   public Coordinates parse(final StringReader reader) throws CommandSyntaxException {
      int start = reader.getCursor();
      if (!reader.canRead()) {
         throw ERROR_NOT_COMPLETE.createWithContext(reader);
      } else {
         WorldCoordinate x = WorldCoordinate.parseInt(reader);
         if (reader.canRead() && reader.peek() == ' ') {
            reader.skip();
            WorldCoordinate z = WorldCoordinate.parseInt(reader);
            return new WorldCoordinates(x, new WorldCoordinate(true, 0.0), z);
         } else {
            reader.setCursor(start);
            throw ERROR_NOT_COMPLETE.createWithContext(reader);
         }
      }
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

      return SharedSuggestionProvider.suggest2DCoordinates(remainder, suggestedCoordinates, builder, Commands.createValidator(this::parse));
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}
