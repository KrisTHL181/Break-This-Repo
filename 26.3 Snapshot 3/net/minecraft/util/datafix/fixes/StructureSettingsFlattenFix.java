package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import net.minecraft.util.Util;

public class StructureSettingsFlattenFix extends DataFix {
   public StructureSettingsFlattenFix(final Schema outputSchema) {
      super(outputSchema, false);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> worldGenSettingsType = this.getInputSchema().getType(References.WORLD_GEN_SETTINGS);
      OpticFinder<?> dimensions = worldGenSettingsType.findField("dimensions");
      return this.fixTypeEverywhereTyped(
         "StructureSettingsFlatten",
         worldGenSettingsType,
         input -> input.updateTyped(
            dimensions,
            typed -> Util.writeAndReadTypedOrThrow(
               typed, dimensions.type(), serialized -> serialized.updateMapValues(StructureSettingsFlattenFix::fixDimension)
            )
         )
      );
   }

   private static Pair<Dynamic<?>, Dynamic<?>> fixDimension(final Pair<Dynamic<?>, Dynamic<?>> entry) {
      Dynamic<?> dimension = (Dynamic<?>)entry.getSecond();
      return Pair.of(
         (Dynamic)entry.getFirst(),
         dimension.update("generator", g -> g.update("settings", s -> s.update("structures", StructureSettingsFlattenFix::fixStructures)))
      );
   }

   private static Dynamic<?> fixStructures(final Dynamic<?> input) {
      Dynamic<?> structures = input.get("structures")
         .orElseEmptyMap()
         .updateMapValues(p -> p.mapSecond(s -> s.set("type", input.createString("minecraft:random_spread"))));
      return (Dynamic<?>)DataFixUtils.orElse(
         input.get("stronghold")
            .result()
            .map(stronghold -> structures.set("minecraft:stronghold", stronghold.set("type", input.createString("minecraft:concentric_rings")))),
         structures
      );
   }
}
