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
   public static final MapCodec<SetWrittenBookPagesFunction> CODEC = RecordCodecBuilder.mapCodec(
      p_391135_ -> commonFields(p_391135_)
         .and(
            p_391135_.group(
               WrittenBookContent.PAGES_CODEC.fieldOf("pages").forGetter(p_336220_ -> p_336220_.pages),
               ListOperation.UNLIMITED_CODEC.forGetter(p_335675_ -> p_335675_.pageOperation)
            )
         )
         .apply(p_391135_, SetWrittenBookPagesFunction::new)
   );
   private final List<Filterable<Component>> pages;
   private final ListOperation pageOperation;

   protected SetWrittenBookPagesFunction(List<LootItemCondition> p_334314_, List<Filterable<Component>> p_336337_, ListOperation p_336120_) {
      super(p_334314_);
      this.pages = p_336337_;
      this.pageOperation = p_336120_;
   }

   @Override
   protected ItemStack run(ItemStack p_328011_, LootContext p_329576_) {
      p_328011_.update(DataComponents.WRITTEN_BOOK_CONTENT, WrittenBookContent.EMPTY, this::apply);
      return p_328011_;
   }

   @VisibleForTesting
   public WrittenBookContent apply(WrittenBookContent p_335075_) {
      List<Filterable<Component>> list = this.pageOperation.apply(p_335075_.pages(), this.pages);
      return p_335075_.withReplacedPages(list);
   }

   @Override
   public LootItemFunctionType<SetWrittenBookPagesFunction> getType() {
      return LootItemFunctions.SET_WRITTEN_BOOK_PAGES;
   }
}
