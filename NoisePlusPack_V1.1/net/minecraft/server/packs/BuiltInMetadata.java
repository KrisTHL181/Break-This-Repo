package net.minecraft.server.packs;

import java.util.Map;
import net.minecraft.server.packs.metadata.MetadataSectionType;

public class BuiltInMetadata {
   private static final BuiltInMetadata EMPTY = new BuiltInMetadata(Map.of());
   private final Map<MetadataSectionType<?>, ?> values;

   private BuiltInMetadata(Map<MetadataSectionType<?>, ?> p_251588_) {
      this.values = p_251588_;
   }

   public <T> T get(MetadataSectionType<T> p_377219_) {
      return (T)this.values.get(p_377219_);
   }

   public static BuiltInMetadata of() {
      return EMPTY;
   }

   public static <T> BuiltInMetadata of(MetadataSectionType<T> p_376773_, T p_249997_) {
      return new BuiltInMetadata(Map.of(p_376773_, p_249997_));
   }

   public static <T1, T2> BuiltInMetadata of(MetadataSectionType<T1> p_377897_, T1 p_252174_, MetadataSectionType<T2> p_377603_, T2 p_250020_) {
      return new BuiltInMetadata(Map.of(p_377897_, p_252174_, p_377603_, (T1)p_250020_));
   }
}
