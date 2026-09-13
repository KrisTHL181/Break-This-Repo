package net.minecraft.advancements.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

public record TagPredicate<T>(TagKey<T> tag, boolean expected) {
   public static <T> Codec<TagPredicate<T>> codec(final ResourceKey<? extends Registry<T>> registryKey) {
      return RecordCodecBuilder.create(
         i -> i.group(TagKey.codec(registryKey).fieldOf("id").forGetter(TagPredicate::tag), Codec.BOOL.fieldOf("expected").forGetter(TagPredicate::expected))
            .apply(i, TagPredicate::new)
      );
   }

   public static <T> TagPredicate<T> is(final TagKey<T> tag) {
      return new TagPredicate<>(tag, true);
   }

   public static <T> TagPredicate<T> isNot(final TagKey<T> tag) {
      return new TagPredicate<>(tag, false);
   }

   public boolean matches(final Holder<T> holder) {
      return holder.is(this.tag) == this.expected;
   }
}
