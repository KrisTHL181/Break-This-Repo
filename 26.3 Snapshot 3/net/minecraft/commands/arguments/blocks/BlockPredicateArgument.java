package net.minecraft.commands.arguments.blocks;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;

public class BlockPredicateArgument implements ArgumentType<BlockPredicateArgument.Result> {
   private static final Collection<String> EXAMPLES = Arrays.asList("stone", "minecraft:stone", "stone[foo=bar]", "#stone", "#stone[foo=bar]{baz=nbt}");
   private final HolderLookup<Block> blocks;

   public BlockPredicateArgument(final CommandBuildContext context) {
      this.blocks = context.lookupOrThrow(Registries.BLOCK);
   }

   public static BlockPredicateArgument blockPredicate(final CommandBuildContext context) {
      return new BlockPredicateArgument(context);
   }

   public BlockPredicateArgument.Result parse(final StringReader reader) throws CommandSyntaxException {
      return parse(this.blocks, reader);
   }

   public static BlockPredicateArgument.Result parse(final HolderLookup<Block> blocks, final StringReader reader) throws CommandSyntaxException {
      return (BlockPredicateArgument.Result)BlockStateParser.parseForTesting(blocks, reader, true)
         .map(
            block -> new BlockPredicateArgument.BlockPredicate(block.blockState(), block.properties().keySet(), block.nbt()),
            tag -> new BlockPredicateArgument.TagPredicate(tag.tag(), tag.vagueProperties(), tag.nbt())
         );
   }

   public static Predicate<BlockInWorld> getBlockPredicate(final CommandContext<CommandSourceStack> context, final String name) throws CommandSyntaxException {
      return (Predicate<BlockInWorld>)context.getArgument(name, BlockPredicateArgument.Result.class);
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
      return BlockStateParser.fillSuggestions(this.blocks, builder, true, true);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   private static class BlockPredicate implements BlockPredicateArgument.Result {
      private final BlockState state;
      private final Set<Property<?>> properties;
      private final @Nullable CompoundTag nbt;

      public BlockPredicate(final BlockState state, final Set<Property<?>> properties, final @Nullable CompoundTag nbt) {
         this.state = state;
         this.properties = properties;
         this.nbt = nbt;
      }

      public boolean test(final BlockInWorld blockInWorld) {
         BlockState state = blockInWorld.getState();
         if (!state.is(this.state.getBlock())) {
            return false;
         }

         for (Property<?> property : this.properties) {
            if (state.getValue(property) != this.state.getValue(property)) {
               return false;
            }
         }

         if (this.nbt == null) {
            return true;
         }

         BlockEntity entity = blockInWorld.getEntity();
         return entity != null && NbtUtils.compareNbt(this.nbt, entity.saveWithFullMetadata(blockInWorld.getLevel().registryAccess()), true);
      }

      @Override
      public boolean requiresNbt() {
         return this.nbt != null;
      }
   }

   public interface Result extends Predicate<BlockInWorld> {
      boolean requiresNbt();
   }

   private static class TagPredicate implements BlockPredicateArgument.Result {
      private final HolderSet<Block> tag;
      private final @Nullable CompoundTag nbt;
      private final Map<String, String> vagueProperties;

      private TagPredicate(final HolderSet<Block> tag, final Map<String, String> vagueProperties, final @Nullable CompoundTag nbt) {
         this.tag = tag;
         this.vagueProperties = vagueProperties;
         this.nbt = nbt;
      }

      public boolean test(final BlockInWorld blockInWorld) {
         BlockState state = blockInWorld.getState();
         if (!state.is(this.tag)) {
            return false;
         }

         for (Entry<String, String> entry : this.vagueProperties.entrySet()) {
            Property<?> property = state.getBlock().getStateDefinition().getProperty(entry.getKey());
            if (property == null) {
               return false;
            }

            Comparable<?> value = (Comparable<?>)property.getValue(entry.getValue()).orElse(null);
            if (value == null) {
               return false;
            }

            if (state.getValue(property) != value) {
               return false;
            }
         }

         if (this.nbt == null) {
            return true;
         }

         BlockEntity entity = blockInWorld.getEntity();
         return entity != null && NbtUtils.compareNbt(this.nbt, entity.saveWithFullMetadata(blockInWorld.getLevel().registryAccess()), true);
      }

      @Override
      public boolean requiresNbt() {
         return this.nbt != null;
      }
   }
}
