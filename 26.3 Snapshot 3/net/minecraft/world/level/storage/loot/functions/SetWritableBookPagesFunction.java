package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetWritableBookPagesFunction extends LootItemConditionalFunction {
   public static final MapCodec<SetWritableBookPagesFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> commonFields(i)
         .and(i.group(WritableBookContent.PAGES_CODEC.fieldOf("pages").forGetter(f -> f.pages), ListOperation.codec(100).forGetter(f -> f.pageOperation)))
         .apply(i, SetWritableBookPagesFunction::new)
   );
   private final List<Filterable<String>> pages;
   private final ListOperation pageOperation;

   protected SetWritableBookPagesFunction(final List<LootItemCondition> predicates, final List<Filterable<String>> pages, final ListOperation pageOperation) {
      super(predicates);
      this.pages = pages;
      this.pageOperation = pageOperation;
   }

   @Override
   protected ItemStack run(final ItemStack itemStack, final LootContext context) {
      itemStack.update(DataComponents.WRITABLE_BOOK_CONTENT, WritableBookContent.EMPTY, this::apply);
      return itemStack;
   }

   public WritableBookContent apply(final WritableBookContent original) {
      List<Filterable<String>> newPages = this.pageOperation.apply(original.pages(), this.pages, 100);
      return original.withReplacedPages(newPages);
   }

   @Override
   public MapCodec<SetWritableBookPagesFunction> codec() {
      return MAP_CODEC;
   }
}
