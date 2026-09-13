package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V3081 extends NamespacedSchema {
   public V3081(int p_216784_, Schema p_216785_) {
      super(p_216784_, p_216785_);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_216795_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_216795_);
      p_216795_.register(
         map,
         "minecraft:warden",
         () -> DSL.optionalFields("listener", DSL.optionalFields("event", DSL.optionalFields("game_event", References.GAME_EVENT_NAME.in(p_216795_))))
      );
      return map;
   }
}
