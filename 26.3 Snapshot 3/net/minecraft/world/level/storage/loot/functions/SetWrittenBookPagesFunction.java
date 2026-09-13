package net.minecraft.world.level.storage.loot.functions;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetWrittenBookPagesFunction extends LootItemConditionalFunction {
   public static final MapCodec<SetWrittenBookPagesFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> commonFields(i)
         .and(i.group(WrittenBookContent.PAGES_CODEC.fieldOf("pages").forGetter(f -> f.pages), ListOperation.UNLIMITED_CODEC.forGetter(f -> f.pageOperation)))
         .apply(i, SetWrittenBookPagesFunction::new)
   );
   private final List<Filterable<Component>> pages;
   private final ListOperation pageOperation;

   protected SetWrittenBookPagesFunction(final List<LootItemCondition> predicates, final List<Filterable<Component>> pages, final ListOperation pageOperation) {
      super(predicates);
      this.pages = pages;
      this.pageOperation = pageOperation;
   }

   @Override
   protected ItemStack run(final ItemStack itemStack, final LootContext context) {
      itemStack.update(DataComponents.WRITTEN_BOOK_CONTENT, WrittenBookContent.EMPTY, this::apply);
      return itemStack;
   }

   @VisibleForTesting
   public WrittenBookContent apply(final WrittenBookContent original) {
      List<Filterable<Component>> newPages = this.pageOperation.apply(original.pages(), this.pages);
      return original.withReplacedPages(newPages);
   }

   @Override
   public MapCodec<SetWrittenBookPagesFunction> codec() {
      return MAP_CODEC;
   }
}
