package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class RenameEnchantmentsFix extends DataFix {
   final String name;
   final Map<String, String> renames;

   public RenameEnchantmentsFix(Schema p_330639_, String p_335402_, Map<String, String> p_335538_) {
      super(p_330639_, false);
      this.name = p_335402_;
      this.renames = p_335538_;
   }

   protected TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(References.ITEM_STACK);
      OpticFinder<?> opticfinder = type.findField("tag");
      return this.fixTypeEverywhereTyped(
         this.name, type, p_327788_ -> p_327788_.updateTyped(opticfinder, p_330012_ -> p_330012_.update(DSL.remainderFinder(), this::fixTag))
      );
   }

   private Dynamic<?> fixTag(Dynamic<?> p_331261_) {
      p_331261_ = this.fixEnchantmentList(p_331261_, "Enchantments");
      return this.fixEnchantmentList(p_331261_, "StoredEnchantments");
   }

   private Dynamic<?> fixEnchantmentList(Dynamic<?> p_329694_, String p_331585_) {
      return p_329694_.update(
         p_331585_,
         p_335338_ -> (Dynamic)p_335338_.asStreamOpt()
            .map(
               p_329289_ -> p_329289_.map(
                  p_333784_ -> p_333784_.update(
                     "id",
                     p_336146_ -> (Dynamic)p_336146_.asString()
                        .map(p_341248_ -> p_333784_.createString(this.renames.getOrDefault(NamespacedSchema.ensureNamespaced(p_341248_), p_341248_)))
                        .mapOrElse(Function.identity(), p_332784_ -> p_336146_)
                  )
               )
            )
            .map(p_335338_::createList)
            .mapOrElse(Function.identity(), p_334640_ -> p_335338_)
      );
   }
}
