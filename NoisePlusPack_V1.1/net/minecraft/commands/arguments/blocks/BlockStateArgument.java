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

   public BlockStateArgument(CommandBuildContext p_234649_) {
      this.blocks = p_234649_.lookupOrThrow(Registries.BLOCK);
   }

   public static BlockStateArgument block(CommandBuildContext p_234651_) {
      return new BlockStateArgument(p_234651_);
   }

   public BlockInput parse(StringReader p_116122_) throws CommandSyntaxException {
      BlockStateParser.BlockResult blockstateparser$blockresult = BlockStateParser.parseForBlock(this.blocks, p_116122_, true);
      return new BlockInput(blockstateparser$blockresult.blockState(), blockstateparser$blockresult.properties().keySet(), blockstateparser$blockresult.nbt());
   }

   public static BlockInput getBlock(CommandContext<CommandSourceStack> p_116124_, String p_116125_) {
      return (BlockInput)p_116124_.getArgument(p_116125_, BlockInput.class);
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> p_116128_, SuggestionsBuilder p_116129_) {
      return BlockStateParser.fillSuggestions(this.blocks, p_116129_, false, true);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}
