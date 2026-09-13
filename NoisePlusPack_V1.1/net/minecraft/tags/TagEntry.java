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
      p_215937_ -> p_215937_.group(
            ExtraCodecs.TAG_OR_ELEMENT_ID.fieldOf("id").forGetter(TagEntry::elementOrTag),
            Codec.BOOL.optionalFieldOf("required", true).forGetter(p_215952_ -> p_215952_.required)
         )
         .apply(p_215937_, TagEntry::new)
   );
   public static final Codec<TagEntry> CODEC = Codec.either(ExtraCodecs.TAG_OR_ELEMENT_ID, FULL_CODEC)
      .xmap(
         p_215935_ -> (TagEntry)p_215935_.map(p_215933_ -> new TagEntry(p_215933_, true), p_215946_ -> p_215946_),
         p_215931_ -> p_215931_.required ? Either.left(p_215931_.elementOrTag()) : Either.right(p_215931_)
      );
   private final Identifier id;
   private final boolean tag;
   private final boolean required;

   private TagEntry(Identifier p_452085_, boolean p_215919_, boolean p_215920_) {
      this.id = p_452085_;
      this.tag = p_215919_;
      this.required = p_215920_;
   }

   private TagEntry(ExtraCodecs.TagOrElementLocation p_215922_, boolean p_215923_) {
      this.id = p_215922_.id();
      this.tag = p_215922_.tag();
      this.required = p_215923_;
   }

   private ExtraCodecs.TagOrElementLocation elementOrTag() {
      return new ExtraCodecs.TagOrElementLocation(this.id, this.tag);
   }

   public static TagEntry element(Identifier p_451361_) {
      return new TagEntry(p_451361_, false, true);
   }

   public static TagEntry optionalElement(Identifier p_456623_) {
      return new TagEntry(p_456623_, false, false);
   }

   public static TagEntry tag(Identifier p_454941_) {
      return new TagEntry(p_454941_, true, true);
   }

   public static TagEntry optionalTag(Identifier p_451022_) {
      return new TagEntry(p_451022_, true, false);
   }

   public <T> boolean build(TagEntry.Lookup<T> p_215928_, Consumer<T> p_215929_) {
      if (this.tag) {
         Collection<T> collection = p_215928_.tag(this.id);
         if (collection == null) {
            return !this.required;
         }

         collection.forEach(p_215929_);
      } else {
         T t = p_215928_.element(this.id, this.required);
         if (t == null) {
            return !this.required;
         }

         p_215929_.accept(t);
      }

      return true;
   }

   public void visitRequiredDependencies(Consumer<Identifier> p_215939_) {
      if (this.tag && this.required) {
         p_215939_.accept(this.id);
      }
   }

   public void visitOptionalDependencies(Consumer<Identifier> p_215948_) {
      if (this.tag && !this.required) {
         p_215948_.accept(this.id);
      }
   }

   public boolean verifyIfPresent(Predicate<Identifier> p_215941_, Predicate<Identifier> p_215942_) {
      return !this.required || (this.tag ? p_215942_ : p_215941_).test(this.id);
   }

   @Override
   public String toString() {
      StringBuilder stringbuilder = new StringBuilder();
      if (this.tag) {
         stringbuilder.append('#');
      }

      stringbuilder.append(this.id);
      if (!this.required) {
         stringbuilder.append('?');
      }

      return stringbuilder.toString();
   }

   public interface Lookup<T> {
      @Nullable T element(Identifier var1, boolean var2);

      @Nullable Collection<T> tag(Identifier var1);
   }
}
