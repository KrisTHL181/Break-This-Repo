package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.Optional;

public class HeightmapRenamingFix extends DataFix {
   public HeightmapRenamingFix(final Schema outputSchema, final boolean changesType) {
      super(outputSchema, changesType);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> inputType = this.getInputSchema().getType(References.CHUNK);
      OpticFinder<?> levelF = inputType.findField("Level");
      return this.fixTypeEverywhereTyped(
         "HeightmapRenamingFix", inputType, input -> input.updateTyped(levelF, level -> level.update(DSL.remainderFinder(), this::fix))
      );
   }

   private Dynamic<?> fix(final Dynamic<?> tag) {
      Optional<? extends Dynamic<?>> heightmaps = tag.get("Heightmaps").result();
      if (heightmaps.isEmpty()) {
         return tag;
      }

      Dynamic<?> heightmapsTag = (Dynamic<?>)heightmaps.get();
      Optional<? extends Dynamic<?>> liquid = heightmapsTag.get("LIQUID").result();
      if (liquid.isPresent()) {
         heightmapsTag = heightmapsTag.remove("LIQUID");
         heightmapsTag = heightmapsTag.set("WORLD_SURFACE_WG", liquid.get());
      }

      Optional<? extends Dynamic<?>> solid = heightmapsTag.get("SOLID").result();
      if (solid.isPresent()) {
         heightmapsTag = heightmapsTag.remove("SOLID");
         heightmapsTag = heightmapsTag.set("OCEAN_FLOOR_WG", solid.get());
         heightmapsTag = heightmapsTag.set("OCEAN_FLOOR", solid.get());
      }

      Optional<? extends Dynamic<?>> light = heightmapsTag.get("LIGHT").result();
      if (light.isPresent()) {
         heightmapsTag = heightmapsTag.remove("LIGHT");
         heightmapsTag = heightmapsTag.set("LIGHT_BLOCKING", light.get());
      }

      Optional<? extends Dynamic<?>> rain = heightmapsTag.get("RAIN").result();
      if (rain.isPresent()) {
         heightmapsTag = heightmapsTag.remove("RAIN");
         heightmapsTag = heightmapsTag.set("MOTION_BLOCKING", rain.get());
         heightmapsTag = heightmapsTag.set("MOTION_BLOCKING_NO_LEAVES", rain.get());
      }

      return tag.set("Heightmaps", heightmapsTag);
   }
}
