package net.minecraft.data.tags;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagKey;

public interface TagAppender<E, T> {
   TagAppender<E, T> add(E var1);

   default TagAppender<E, T> add(E... p_406204_) {
      return this.addAll(Arrays.stream(p_406204_));
   }

   default TagAppender<E, T> addAll(Collection<E> p_405923_) {
      p_405923_.forEach(this::add);
      return this;
   }

   default TagAppender<E, T> addAll(Stream<E> p_406577_) {
      p_406577_.forEach(this::add);
      return this;
   }

   TagAppender<E, T> addOptional(E var1);

   TagAppender<E, T> addTag(TagKey<T> var1);

   TagAppender<E, T> addOptionalTag(TagKey<T> var1);

   static <T> TagAppender<ResourceKey<T>, T> forBuilder(final TagBuilder p_407840_) {
      return new TagAppender<ResourceKey<T>, T>() {
         public TagAppender<ResourceKey<T>, T> add(ResourceKey<T> p_406023_) {
            p_407840_.addElement(p_406023_.identifier());
            return this;
         }

         public TagAppender<ResourceKey<T>, T> addOptional(ResourceKey<T> p_409233_) {
            p_407840_.addOptionalElement(p_409233_.identifier());
            return this;
         }

         @Override
         public TagAppender<ResourceKey<T>, T> addTag(TagKey<T> p_410264_) {
            p_407840_.addTag(p_410264_.location());
            return this;
         }

         @Override
         public TagAppender<ResourceKey<T>, T> addOptionalTag(TagKey<T> p_407223_) {
            p_407840_.addOptionalTag(p_407223_.location());
            return this;
         }
      };
   }

   default <U> TagAppender<U, T> map(final Function<U, E> p_409081_) {
      final TagAppender<E, T> tagappender = this;
      return new TagAppender<U, T>() {
         @Override
         public TagAppender<U, T> add(U p_409420_) {
            tagappender.add(p_409081_.apply(p_409420_));
            return this;
         }

         @Override
         public TagAppender<U, T> addOptional(U p_410494_) {
            tagappender.add(p_409081_.apply(p_410494_));
            return this;
         }

         @Override
         public TagAppender<U, T> addTag(TagKey<T> p_409457_) {
            tagappender.addTag(p_409457_);
            return this;
         }

         @Override
         public TagAppender<U, T> addOptionalTag(TagKey<T> p_409695_) {
            tagappender.addOptionalTag(p_409695_);
            return this;
         }
      };
   }
}
