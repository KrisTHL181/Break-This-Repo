package net.minecraft.data.tags;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagKey;

public interface TagAppender<T> {
   TagAppender<T> add(ResourceKey<T> element);

   default TagAppender<T> add(final ResourceKey<T>... elements) {
      return this.addAll(Arrays.stream(elements));
   }

   default TagAppender<T> addAll(final Collection<ResourceKey<T>> elements) {
      elements.forEach(this::add);
      return this;
   }

   default TagAppender<T> addAll(final Stream<ResourceKey<T>> elements) {
      elements.forEach(this::add);
      return this;
   }

   TagAppender<T> addOptional(ResourceKey<T> element);

   TagAppender<T> addTag(TagKey<T> tag);

   TagAppender<T> addOptionalTag(TagKey<T> tag);

   static <T> TagAppender<T> forBuilder(final TagBuilder builder) {
      return new TagAppender<T>() {
         @Override
         public TagAppender<T> add(final ResourceKey<T> element) {
            builder.addElement(element.identifier());
            return this;
         }

         @Override
         public TagAppender<T> addOptional(final ResourceKey<T> element) {
            builder.addOptionalElement(element.identifier());
            return this;
         }

         @Override
         public TagAppender<T> addTag(final TagKey<T> tag) {
            builder.addTag(tag.location());
            return this;
         }

         @Override
         public TagAppender<T> addOptionalTag(final TagKey<T> tag) {
            builder.addOptionalTag(tag.location());
            return this;
         }
      };
   }
}
