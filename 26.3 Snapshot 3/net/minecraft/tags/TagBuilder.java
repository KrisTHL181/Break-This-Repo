package net.minecraft.tags;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;

public class TagBuilder {
   private final List<TagEntry> entries = new ArrayList<>();
   private boolean replace = false;

   public static TagBuilder create() {
      return new TagBuilder();
   }

   public List<TagEntry> build() {
      return List.copyOf(this.entries);
   }

   public boolean shouldReplace() {
      return this.replace;
   }

   public TagBuilder setReplace(final boolean replace) {
      this.replace = replace;
      return this;
   }

   public TagBuilder add(final TagEntry entry) {
      this.entries.add(entry);
      return this;
   }

   public TagBuilder addElement(final Identifier id) {
      return this.add(TagEntry.element(id));
   }

   public TagBuilder addOptionalElement(final Identifier id) {
      return this.add(TagEntry.optionalElement(id));
   }

   public TagBuilder addTag(final Identifier id) {
      return this.add(TagEntry.tag(id));
   }

   public TagBuilder addOptionalTag(final Identifier id) {
      return this.add(TagEntry.optionalTag(id));
   }
}
