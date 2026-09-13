package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.Const.PrimitiveType;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.PrimitiveCodec;
import net.minecraft.resources.Identifier;

public class NamespacedSchema extends Schema {
   public static final PrimitiveCodec<String> NAMESPACED_STRING_CODEC = new PrimitiveCodec<String>() {
      public <T> DataResult<String> read(DynamicOps<T> p_17321_, T p_17322_) {
         return p_17321_.getStringValue(p_17322_).map(NamespacedSchema::ensureNamespaced);
      }

      public <T> T write(DynamicOps<T> p_17318_, String p_17319_) {
         return (T)p_17318_.createString(p_17319_);
      }

      @Override
      public String toString() {
         return "NamespacedString";
      }
   };
   private static final Type<String> NAMESPACED_STRING = new PrimitiveType(NAMESPACED_STRING_CODEC);

   public NamespacedSchema(int p_17308_, Schema p_17309_) {
      super(p_17308_, p_17309_);
   }

   public static String ensureNamespaced(String p_17312_) {
      Identifier identifier = Identifier.tryParse(p_17312_);
      return identifier != null ? identifier.toString() : p_17312_;
   }

   public static Type<String> namespacedString() {
      return NAMESPACED_STRING;
   }

   public Type<?> getChoiceType(TypeReference p_17314_, String p_17315_) {
      return super.getChoiceType(p_17314_, ensureNamespaced(p_17315_));
   }
}
