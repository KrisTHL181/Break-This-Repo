package net.minecraft.core;

import java.util.stream.Stream;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

public interface TypedInstance<T> {
   Holder<T> typeHolder();

   default Stream<TagKey<T>> tags() {
      return this.typeHolder().tags();
   }

   default boolean is(final TagKey<T> tag) {
      return this.typeHolder().is(tag);
   }

   default boolean is(final HolderSet<T> set) {
      return set.contains(this.typeHolder());
   }

   default boolean is(final T rawType) {
      return this.typeHolder().value() == rawType;
   }

   default boolean is(final Holder<T> type) {
      return this.typeHolder() == type;
   }

   default boolean is(final ResourceKey<T> type) {
      return this.typeHolder().is(type);
   }
}
