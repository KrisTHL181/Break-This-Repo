package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.SequencedMap;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V4307 extends NamespacedSchema {
   public V4307(int p_394198_, Schema p_393601_) {
      super(p_394198_, p_393601_);
   }

   public static SequencedMap<String, Supplier<TypeTemplate>> components(Schema p_392155_) {
      SequencedMap<String, Supplier<TypeTemplate>> sequencedmap = V4059.components(p_392155_);
      sequencedmap.put("minecraft:can_place_on", () -> adventureModePredicate(p_392155_));
      sequencedmap.put("minecraft:can_break", () -> adventureModePredicate(p_392155_));
      return sequencedmap;
   }

   private static TypeTemplate adventureModePredicate(Schema p_394845_) {
      TypeTemplate typetemplate = DSL.optionalFields("blocks", DSL.or(References.BLOCK_NAME.in(p_394845_), DSL.list(References.BLOCK_NAME.in(p_394845_))));
      return DSL.or(typetemplate, DSL.list(typetemplate));
   }

   public void registerTypes(Schema p_395189_, Map<String, Supplier<TypeTemplate>> p_393725_, Map<String, Supplier<TypeTemplate>> p_391596_) {
      super.registerTypes(p_395189_, p_393725_, p_391596_);
      p_395189_.registerType(true, References.DATA_COMPONENTS, () -> DSL.optionalFieldsLazy(components(p_395189_)));
   }
}
