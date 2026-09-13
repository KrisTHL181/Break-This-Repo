package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.network.Filterable;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetBookCoverFunction extends LootItemConditionalFunction {
   public static final MapCodec<SetBookCoverFunction> CODEC = RecordCodecBuilder.mapCodec(
      p_334246_ -> commonFields(p_334246_)
         .and(
            p_334246_.group(
               Filterable.codec(Codec.string(0, 32)).optionalFieldOf("title").forGetter(p_332216_ -> p_332216_.title),
               Codec.STRING.optionalFieldOf("author").forGetter(p_329081_ -> p_329081_.author),
               ExtraCodecs.intRange(0, 3).optionalFieldOf("generation").forGetter(p_332368_ -> p_332368_.generation)
            )
         )
         .apply(p_334246_, SetBookCoverFunction::new)
   );
   private final Optional<String> author;
   private final Optional<Filterable<String>> title;
   private final Optional<Integer> generation;

   public SetBookCoverFunction(
      List<LootItemCondition> p_335903_, Optional<Filterable<String>> p_331140_, Optional<String> p_331575_, Optional<Integer> p_328783_
   ) {
      super(p_335903_);
      this.author = p_331575_;
      this.title = p_331140_;
      this.generation = p_328783_;
   }

   @Override
   protected ItemStack run(ItemStack p_331816_, LootContext p_333079_) {
      p_331816_.update(DataComponents.WRITTEN_BOOK_CONTENT, WrittenBookContent.EMPTY, this::apply);
      return p_331816_;
   }

   private WrittenBookContent apply(WrittenBookContent p_331548_) {
      return new WrittenBookContent(
         this.title.orElseGet(p_331548_::title),
         this.author.orElseGet(p_331548_::author),
         this.generation.orElseGet(p_331548_::generation),
         p_331548_.pages(),
         p_331548_.resolved()
      );
   }

   @Override
   public LootItemFunctionType<SetBookCoverFunction> getType() {
      return LootItemFunctions.SET_BOOK_COVER;
   }
}
