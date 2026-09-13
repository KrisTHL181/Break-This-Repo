package net.minecraft.tags;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import org.jspecify.annotations.Nullable;

public class TagEntry {
   private static final Codec<TagEntry> FULL_CODEC = RecordCodecBuilder.create(
      i -> i.group(
            ExtraCodecs.TAG_OR_ELEMENT_ID.fieldOf("id").forGetter(TagEntry::elementOrTag),
            Codec.BOOL.optionalFieldOf("required", true).forGetter(e -> e.required)
         )
         .apply(i, TagEntry::new)
   );
   public static final Codec<TagEntry> CODEC = Codec.either(ExtraCodecs.TAG_OR_ELEMENT_ID, FULL_CODEC)
      .xmap(e -> (TagEntry)e.map(l -> new TagEntry(l, true), r -> r), entry -> entry.required ? Either.left(entry.elementOrTag()) : Either.right(entry));
   private final Identifier id;
   private final boolean tag;
   private final boolean required;

   private TagEntry(final Identifier id, final boolean tag, final boolean required) {
      this.id = id;
      this.tag = tag;
      this.required = required;
   }

   private TagEntry(final ExtraCodecs.TagOrElementLocation elementOrTag, final boolean required) {
      this.id = elementOrTag.id();
      this.tag = elementOrTag.tag();
      this.required = required;
   }

   private ExtraCodecs.TagOrElementLocation elementOrTag() {
      return new ExtraCodecs.TagOrElementLocation(this.id, this.tag);
   }

   public static TagEntry element(final Identifier id) {
      return new TagEntry(id, false, true);
   }

   public static TagEntry optionalElement(final Identifier id) {
      return new TagEntry(id, false, false);
   }

   public static TagEntry tag(final Identifier id) {
      return new TagEntry(id, true, true);
   }

   public static TagEntry optionalTag(final Identifier id) {
      return new TagEntry(id, true, false);
   }

   public <T> boolean build(final TagEntry.Lookup<T> lookup, final Consumer<T> output) {
      if (this.tag) {
         Collection<T> result = lookup.tag(this.id);
         if (result == null) {
            return !this.required;
         }

         result.forEach(output);
      } else {
         T result = lookup.element(this.id, this.required);
         if (result == null) {
            return !this.required;
         }

         output.accept(result);
      }

      return true;
   }

   public void visitRequiredDependencies(final Consumer<Identifier> output) {
      if (this.tag && this.required) {
         output.accept(this.id);
      }
   }

   public void visitOptionalDependencies(final Consumer<Identifier> output) {
      if (this.tag && !this.required) {
         output.accept(this.id);
      }
   }

   public boolean verifyIfPresent(final Predicate<Identifier> elementCheck, final Predicate<Identifier> tagCheck) {
      return !this.required || (this.tag ? tagCheck : elementCheck).test(this.id);
   }

   @Override
   public String toString() {
      StringBuilder result = new StringBuilder();
      if (this.tag) {
         result.append('#');
      }

      result.append(this.id);
      if (!this.required) {
         result.append('?');
      }

      return result.toString();
   }

   public interface Lookup<T> {
      @Nullable T element(Identifier key, boolean required);

      @Nullable Collection<T> tag(Identifier key);
   }
}
