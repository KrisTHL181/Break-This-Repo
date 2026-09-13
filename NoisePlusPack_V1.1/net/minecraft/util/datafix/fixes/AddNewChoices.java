package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TaggedChoice.TaggedChoiceType;
import java.util.Locale;

public class AddNewChoices extends DataFix {
   private final String name;
   private final TypeReference type;

   public AddNewChoices(Schema p_14628_, String p_14629_, TypeReference p_14630_) {
      super(p_14628_, true);
      this.name = p_14629_;
      this.type = p_14630_;
   }

   public TypeRewriteRule makeRule() {
      TaggedChoiceType<?> taggedchoicetype = this.getInputSchema().findChoiceType(this.type);
      TaggedChoiceType<?> taggedchoicetype1 = this.getOutputSchema().findChoiceType(this.type);
      return this.cap(taggedchoicetype, taggedchoicetype1);
   }

   private <K> TypeRewriteRule cap(TaggedChoiceType<K> p_14639_, TaggedChoiceType<?> p_14640_) {
      if (p_14639_.getKeyType() != p_14640_.getKeyType()) {
         throw new IllegalStateException("Could not inject: key type is not the same");
      } else {
         return this.fixTypeEverywhere(
            this.name,
            p_14639_,
            p_14640_,
            p_14636_ -> p_326542_ -> {
               if (!p_14640_.hasType(p_326542_.getFirst())) {
                  throw new IllegalArgumentException(
                     String.format(Locale.ROOT, "%s: Unknown type %s in '%s'", this.name, p_326542_.getFirst(), this.type.typeName())
                  );
               } else {
                  return p_326542_;
               }
            }
         );
      }
   }
}
