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
   public static final MapCodec<SetWritableBookPagesFunction> CODEC = RecordCodecBuilder.mapCodec(
      p_330843_ -> commonFields(p_330843_)
         .and(
            p_330843_.group(
               WritableBookContent.PAGES_CODEC.fieldOf("pages").forGetter(p_329804_ -> p_329804_.pages),
               ListOperation.codec(100).forGetter(p_333000_ -> p_333000_.pageOperation)
            )
         )
         .apply(p_330843_, SetWritableBookPagesFunction::new)
   );
   private final List<Filterable<String>> pages;
   private final ListOperation pageOperation;

   protected SetWritableBookPagesFunction(List<LootItemCondition> p_330949_, List<Filterable<String>> p_330006_, ListOperation p_334902_) {
      super(p_330949_);
      this.pages = p_330006_;
      this.pageOperation = p_334902_;
   }

   @Override
   protected ItemStack run(ItemStack p_329402_, LootContext p_330509_) {
      p_329402_.update(DataComponents.WRITABLE_BOOK_CONTENT, WritableBookContent.EMPTY, this::apply);
      return p_329402_;
   }

   public WritableBookContent apply(WritableBookContent p_328886_) {
      List<Filterable<String>> list = this.pageOperation.apply(p_328886_.pages(), this.pages, 100);
      return p_328886_.withReplacedPages(list);
   }

   @Override
   public LootItemFunctionType<SetWritableBookPagesFunction> getType() {
      return LootItemFunctions.SET_WRITABLE_BOOK_PAGES;
   }
}
