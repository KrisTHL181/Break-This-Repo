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

   public static BlockPos getLoadedBlockPos(CommandContext<CommandSourceStack> p_118243_, String p_118244_) throws CommandSyntaxException {
      ServerLevel serverlevel = ((CommandSourceStack)p_118243_.getSource()).getLevel();
      return getLoadedBlockPos(p_118243_, serverlevel, p_118244_);
   }

   public static BlockPos getLoadedBlockPos(CommandContext<CommandSourceStack> p_265283_, ServerLevel p_265219_, String p_265677_) throws CommandSyntaxException {
      BlockPos blockpos = getBlockPos(p_265283_, p_265677_);
      if (!p_265219_.hasChunkAt(blockpos)) {
         throw ERROR_NOT_LOADED.create();
      } else if (!p_265219_.isInWorldBounds(blockpos)) {
         throw ERROR_OUT_OF_WORLD.create();
      } else {
         return blockpos;
      }
   }

   public static BlockPos getBlockPos(CommandContext<CommandSourceStack> p_265651_, String p_265039_) {
      return ((Coordinates)p_265651_.getArgument(p_265039_, Coordinates.class)).getBlockPos((CommandSourceStack)p_265651_.getSource());
   }

   public static BlockPos getSpawnablePos(CommandContext<CommandSourceStack> p_174396_, String p_174397_) throws CommandSyntaxException {
      BlockPos blockpos = getBlockPos(p_174396_, p_174397_);
      if (!Level.isInSpawnableBounds(blockpos)) {
         throw ERROR_OUT_OF_BOUNDS.create();
      } else {
         return blockpos;
      }
   }

   public Coordinates parse(StringReader p_118241_) throws CommandSyntaxException {
      return (Coordinates)(p_118241_.canRead() && p_118241_.peek() == '^' ? LocalCoordinates.parse(p_118241_) : WorldCoordinates.parseInt(p_118241_));
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> p_118250_, SuggestionsBuilder p_118251_) {
      if (!(p_118250_.getSource() instanceof SharedSuggestionProvider)) {
         return Suggestions.empty();
      }

      String s = p_118251_.getRemaining();
      Collection<SharedSuggestionProvider.TextCoordinates> collection;
      if (!s.isEmpty() && s.charAt(0) == '^') {
         collection = Collections.singleton(SharedSuggestionProvider.TextCoordinates.DEFAULT_LOCAL);
      } else {
         collection = ((SharedSuggestionProvider)p_118250_.getSource()).getRelevantCoordinates();
      }

      return SharedSuggestionProvider.suggestCoordinates(s, collection, p_118251_, Commands.createValidator(this::parse));
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}
