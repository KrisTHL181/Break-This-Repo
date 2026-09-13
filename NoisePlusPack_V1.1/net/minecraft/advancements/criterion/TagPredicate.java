package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

public record TagPredicate<T>(TagKey<T> tag, boolean expected) {
   public static <T> Codec<TagPredicate<T>> codec(ResourceKey<? extends Registry<T>> p_450815_) {
      return RecordCodecBuilder.create(
         p_452722_ -> p_452722_.group(
               TagKey.codec(p_450815_).fieldOf("id").forGetter(TagPredicate::tag), Codec.BOOL.fieldOf("expected").forGetter(TagPredicate::expected)
            )
            .apply(p_452722_, TagPredicate::new)
      );
   }

   public static <T> TagPredicate<T> is(TagKey<T> p_452631_) {
      return new TagPredicate<>(p_452631_, true);
   }

   public static <T> TagPredicate<T> isNot(TagKey<T> p_450294_) {
      return new TagPredicate<>(p_450294_, false);
   }

   public boolean matches(Holder<T> p_454262_) {
      return p_454262_.is(this.tag) == this.expected;
   }
}
