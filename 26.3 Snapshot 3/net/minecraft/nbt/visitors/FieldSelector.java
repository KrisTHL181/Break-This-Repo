package net.minecraft.nbt.visitors;

import java.util.List;
import net.minecraft.nbt.TagType;

public record FieldSelector(List<String> path, TagType<?> type, String name) {
   public FieldSelector(final TagType<?> type, final String name) {
      this(List.of(), type, name);
   }

   public FieldSelector(final String parent, final TagType<?> type, final String name) {
      this(List.of(parent), type, name);
   }

   public FieldSelector(final String grandparent, final String parent, final TagType<?> type, final String name) {
      this(List.of(grandparent, parent), type, name);
   }
}
