package net.minecraft.nbt.visitors;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.TagType;

public record FieldTree(int depth, Map<String, TagType<?>> selectedFields, Map<String, FieldTree> fieldsToRecurse) {
   private FieldTree(final int depth) {
      this(depth, new HashMap<>(), new HashMap<>());
   }

   public static FieldTree createRoot() {
      return new FieldTree(1);
   }

   public void addEntry(final FieldSelector field) {
      if (this.depth <= field.path().size()) {
         this.fieldsToRecurse.computeIfAbsent(field.path().get(this.depth - 1), s -> new FieldTree(this.depth + 1)).addEntry(field);
      } else {
         this.selectedFields.put(field.name(), field.type());
      }
   }

   public boolean isSelected(final TagType<?> type, final String id) {
      return type.equals(this.selectedFields().get(id));
   }
}
