package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

public class AddFieldFix extends DataFix {
   private final String name;
   private final TypeReference type;
   private final String fieldName;
   private final String[] path;
   private final Function<Dynamic<?>, Dynamic<?>> fieldGenerator;

   public AddFieldFix(Schema p_424069_, TypeReference p_424274_, String p_424135_, Function<Dynamic<?>, Dynamic<?>> p_428082_, String... p_425569_) {
      super(p_424069_, false);
      this.name = "Adding field `" + p_424135_ + "` to type `" + p_424274_.typeName().toLowerCase(Locale.ROOT) + "`";
      this.type = p_424274_;
      this.fieldName = p_424135_;
      this.path = p_425569_;
      this.fieldGenerator = p_428082_;
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         this.name,
         this.getInputSchema().getType(this.type),
         this.getOutputSchema().getType(this.type),
         p_430625_ -> p_430625_.update(DSL.remainderFinder(), p_431277_ -> this.addField(p_431277_, 0))
      );
   }

   private Dynamic<?> addField(Dynamic<?> p_429854_, int p_422386_) {
      if (p_422386_ >= this.path.length) {
         return p_429854_.set(this.fieldName, this.fieldGenerator.apply(p_429854_));
      }

      Optional<? extends Dynamic<?>> optional = p_429854_.get(this.path[p_422386_]).result();
      return optional.isEmpty() ? p_429854_ : this.addField((Dynamic<?>)optional.get(), p_422386_ + 1);
   }
}
