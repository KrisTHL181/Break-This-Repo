package net.minecraft.core.component.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.advancements.predicates.CollectionPredicate;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.advancements.predicates.SingleComponentItemPredicate;
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
      i -> i.group(
            CollectionPredicate.codec(WrittenBookPredicate.PagePredicate.CODEC).optionalFieldOf("pages").forGetter(WrittenBookPredicate::pages),
            Codec.STRING.optionalFieldOf("author").forGetter(WrittenBookPredicate::author),
            Codec.STRING.optionalFieldOf("title").forGetter(WrittenBookPredicate::title),
            MinMaxBounds.Ints.CODEC.optionalFieldOf("generation", MinMaxBounds.Ints.ANY).forGetter(WrittenBookPredicate::generation),
            Codec.BOOL.optionalFieldOf("resolved").forGetter(WrittenBookPredicate::resolved)
         )
         .apply(i, WrittenBookPredicate::new)
   );

   @Override
   public DataComponentType<WrittenBookContent> componentType() {
      return DataComponents.WRITTEN_BOOK_CONTENT;
   }

   public boolean matches(final WrittenBookContent value) {
      if (this.author.isPresent() && !this.author.get().equals(value.author())) {
         return false;
      } else if (this.title.isPresent() && !this.title.get().equals(value.title().raw())) {
         return false;
      } else if (!this.generation.matches(value.generation())) {
         return false;
      } else {
         return this.resolved.isPresent() && this.resolved.get() != value.resolved() ? false : !this.pages.isPresent() || this.pages.get().test(value.pages());
      }
   }

   public record PagePredicate(Component contents) implements Predicate<Filterable<Component>> {
      public static final Codec<WrittenBookPredicate.PagePredicate> CODEC = ComponentSerialization.CODEC
         .xmap(WrittenBookPredicate.PagePredicate::new, WrittenBookPredicate.PagePredicate::contents);

      public boolean test(final Filterable<Component> value) {
         return value.raw().equals(this.contents);
      }
   }
}
