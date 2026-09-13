package net.minecraft.commands.arguments.blocks;

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
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;

public class BlockStateArgument implements ArgumentType<BlockInput> {
   private static final Collection<String> EXAMPLES = Arrays.asList("stone", "minecraft:stone", "stone[foo=bar]", "foo{bar=baz}");
   private final HolderLookup<Block> blocks;

   public BlockStateArgument(final CommandBuildContext context) {
      this.blocks = context.lookupOrThrow(Registries.BLOCK);
   }

   public static BlockStateArgument block(final CommandBuildContext context) {
      return new BlockStateArgument(context);
   }

   public BlockInput parse(final StringReader reader) throws CommandSyntaxException {
      BlockStateParser.BlockResult result = BlockStateParser.parseForBlock(this.blocks, reader, true);
      return new BlockInput(result.blockState(), result.properties().keySet(), result.nbt());
   }

   public static BlockInput getBlock(final CommandContext<CommandSourceStack> context, final String name) {
      return (BlockInput)context.getArgument(name, BlockInput.class);
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
      return BlockStateParser.fillSuggestions(this.blocks, builder, false, true);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}
