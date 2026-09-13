package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V3818_4 extends NamespacedSchema {
   public V3818_4(int p_335928_, Schema p_330609_) {
      super(p_335928_, p_330609_);
   }

   public void registerTypes(Schema p_328262_, Map<String, Supplier<TypeTemplate>> p_328638_, Map<String, Supplier<TypeTemplate>> p_333207_) {
      super.registerTypes(p_328262_, p_328638_, p_333207_);
      p_328262_.registerType(
         true, References.PARTICLE, () -> DSL.optionalFields("item", References.ITEM_STACK.in(p_328262_), "block_state", References.BLOCK_STATE.in(p_328262_))
      );
   }
}
