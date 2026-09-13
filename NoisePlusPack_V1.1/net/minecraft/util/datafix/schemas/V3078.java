package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V3078 extends NamespacedSchema {
   public V3078(int p_216769_, Schema p_216770_) {
      super(p_216769_, p_216770_);
   }

   protected static void registerMob(Schema p_216774_, Map<String, Supplier<TypeTemplate>> p_216775_, String p_216776_) {
      p_216774_.registerSimple(p_216775_, p_216776_);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_216782_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_216782_);
      registerMob(p_216782_, map, "minecraft:frog");
      registerMob(p_216782_, map, "minecraft:tadpole");
      return map;
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema p_216780_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(p_216780_);
      p_216780_.register(
         map,
         "minecraft:sculk_shrieker",
         () -> DSL.optionalFields("listener", DSL.optionalFields("event", DSL.optionalFields("game_event", References.GAME_EVENT_NAME.in(p_216780_))))
      );
      return map;
   }
}
