package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public abstract class AbstractBlockPropertyFix extends DataFix {
   private final String name;

   public AbstractBlockPropertyFix(Schema p_392685_, String p_391388_) {
      super(p_392685_, false);
      this.name = p_391388_;
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         this.name, this.getInputSchema().getType(References.BLOCK_STATE), p_393544_ -> p_393544_.update(DSL.remainderFinder(), this::fixBlockState)
      );
   }

   private Dynamic<?> fixBlockState(Dynamic<?> p_396797_) {
      Optional<String> optional = p_396797_.get("Name").asString().result().map(NamespacedSchema::ensureNamespaced);
      return optional.isPresent() && this.shouldFix(optional.get())
         ? p_396797_.update("Properties", p_395220_ -> this.fixProperties(optional.get(), p_395220_))
         : p_396797_;
   }

   protected abstract boolean shouldFix(String var1);

   protected abstract <T> Dynamic<T> fixProperties(String var1, Dynamic<T> var2);
}
