package net.minecraft.core.component.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.advancements.criterion.CollectionPredicate;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.advancements.criterion.SingleComponentItemPredicate;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.component.WrittenBookContent;

public record WrittenBookPredicate(
   Optional<CollectionPredicate<Filterable<Component>, WrittenBookPredicate.PagePredicate>> pages,
   Optional<String> author,
   Optional<String> title,
   MinMaxBounds.Ints generation,
   Optional<Boolean> resolved
) implements SingleComponentItemPredicate<WrittenBookContent> {
   public static final Codec<WrittenBookPredicate> CODEC = RecordCodecBuilder.create(
      p_448618_ -> p_448618_.group(
            CollectionPredicate.codec(WrittenBookPredicate.PagePredicate.CODEC).optionalFieldOf("pages").forGetter(WrittenBookPredicate::pages),
            Codec.STRING.optionalFieldOf("author").forGetter(WrittenBookPredicate::author),
            Codec.STRING.optionalFieldOf("title").forGetter(WrittenBookPredicate::title),
            MinMaxBounds.Ints.CODEC.optionalFieldOf("generation", MinMaxBounds.Ints.ANY).forGetter(WrittenBookPredicate::generation),
            Codec.BOOL.optionalFieldOf("resolved").forGetter(WrittenBookPredicate::resolved)
         )
         .apply(p_448618_, WrittenBookPredicate::new)
   );

   @Override
   public DataComponentType<WrittenBookContent> componentType() {
      return DataComponents.WRITTEN_BOOK_CONTENT;
   }

   public boolean matches(WrittenBookContent p_396394_) {
      if (this.author.isPresent() && !this.author.get().equals(p_396394_.author())) {
         return false;
      } else if (this.title.isPresent() && !this.title.get().equals(p_396394_.title().raw())) {
         return false;
      } else if (!this.generation.matches(p_396394_.generation())) {
         return false;
      } else {
         return this.resolved.isPresent() && this.resolved.get() != p_396394_.resolved()
            ? false
            : !this.pages.isPresent() || this.pages.get().test(p_396394_.pages());
      }
   }

   public record PagePredicate(Component contents) implements Predicate<Filterable<Component>> {
      public static final Codec<WrittenBookPredicate.PagePredicate> CODEC = ComponentSerialization.CODEC
         .xmap(WrittenBookPredicate.PagePredicate::new, WrittenBookPredicate.PagePredicate::contents);

      public boolean test(Filterable<Component> p_396207_) {
         return p_396207_.raw().equals(this.contents);
      }
   }
}
