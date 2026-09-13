package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class DebugProfileLookingAtSplitFix extends DataFix {
   public DebugProfileLookingAtSplitFix(final Schema outputSchema) {
      super(outputSchema, false);
   }

   public TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "DebugProfileLookingAtSplitFix",
         this.getInputSchema().getType(References.DEBUG_PROFILE),
         input -> input.update(DSL.remainderFinder(), tag -> tag.update("custom", DebugProfileLookingAtSplitFix::updateOptions))
      );
   }

   private static <T> Dynamic<T> updateOptions(final Dynamic<T> custom) {
      return (Dynamic<T>)DataFixUtils.orElse(
         custom.getMapValues()
            .map(
               map -> {
                  Map<Dynamic<T>, Dynamic<T>> newOptions = new HashMap<>();
                  map.forEach(
                     (key, value) -> renamedKey((Dynamic<T>)key)
                        .ifPresentOrElse(newKey -> newOptions.putIfAbsent((Dynamic<T>)newKey, value), () -> newOptions.put((Dynamic<T>)key, (Dynamic<T>)value))
                  );
                  return custom.createMap(newOptions);
               }
            )
            .result(),
         custom
      );
   }

   private static <T> Optional<Dynamic<T>> renamedKey(final Dynamic<T> keyDynamic) {
      return keyDynamic.asString().result().flatMap(key -> {
         return switch (key) {
            case "minecraft:looking_at_block" -> Optional.of("minecraft:looking_at_block_state");
            case "minecraft:looking_at_fluid" -> Optional.of("minecraft:looking_at_fluid_state");
            default -> Optional.empty();
         };
      }).map(keyDynamic::createString);
   }
}
