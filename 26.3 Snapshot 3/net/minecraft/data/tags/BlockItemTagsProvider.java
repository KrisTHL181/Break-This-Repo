package net.minecraft.data.tags;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.references.BlockItemId;
import net.minecraft.tags.BlockItemTagId;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public abstract class BlockItemTagsProvider {
   private final Function<BlockItemTagId, BlockItemTagsProvider.CombinedAppender> tagSupplier;

   protected BlockItemTagsProvider(final Function<BlockItemTagId, BlockItemTagsProvider.CombinedAppender> tagSupplier) {
      this.tagSupplier = tagSupplier;
   }

   protected BlockItemTagsProvider.CombinedAppender tag(final BlockItemTagId tag) {
      return this.tagSupplier.apply(tag);
   }

   protected abstract void run();

   public static BlockItemTagsProvider.CombinedAppender wrapForBlocks(final TagAppender<Block> appender) {
      return new BlockItemTagsProvider.CombinedAppender() {
         @Override
         public BlockItemTagsProvider.CombinedAppender addAll(final Stream<BlockItemId> ids) {
            appender.addAll(ids.map(BlockItemId::block));
            return this;
         }

         @Override
         public BlockItemTagsProvider.CombinedAppender addTag(final BlockItemTagId id) {
            appender.addTag(id.block());
            return this;
         }
      };
   }

   public static BlockItemTagsProvider.CombinedAppender wrapForItems(final TagAppender<Item> appender) {
      return new BlockItemTagsProvider.CombinedAppender() {
         @Override
         public BlockItemTagsProvider.CombinedAppender addAll(final Stream<BlockItemId> ids) {
            appender.addAll(ids.map(BlockItemId::item));
            return this;
         }

         @Override
         public BlockItemTagsProvider.CombinedAppender addTag(final BlockItemTagId id) {
            appender.addTag(id.item());
            return this;
         }
      };
   }

   public interface CombinedAppender {
      BlockItemTagsProvider.CombinedAppender addAll(Stream<BlockItemId> ids);

      BlockItemTagsProvider.CombinedAppender addTag(BlockItemTagId id);

      default BlockItemTagsProvider.CombinedAppender add(final BlockItemId... ids) {
         this.addAll(Arrays.stream(ids));
         return this;
      }

      default BlockItemTagsProvider.CombinedAppender addAll(final Collection<BlockItemId> ids) {
         this.addAll(ids.stream());
         return this;
      }
   }
}
