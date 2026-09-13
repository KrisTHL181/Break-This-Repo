package net.minecraft.util.datafix.fixes;

import com.google.common.collect.Maps;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class EntityPaintingMotiveFix extends NamedEntityFix {
   private static final Map<String, String> MAP = (Map<String, String>)DataFixUtils.make(Maps.newHashMap(), map -> {
      map.put("donkeykong", "donkey_kong");
      map.put("burningskull", "burning_skull");
      map.put("skullandroses", "skull_and_roses");
   });

   public EntityPaintingMotiveFix(final Schema outputSchema, final boolean changesType) {
      super(outputSchema, changesType, "EntityPaintingMotiveFix", References.ENTITY, "minecraft:painting");
   }

   public Dynamic<?> fixTag(final Dynamic<?> input) {
      Optional<String> motive = input.get("Motive").asString().result();
      if (motive.isPresent()) {
         String lowerCaseMotive = motive.get().toLowerCase(Locale.ROOT);
         return input.set("Motive", input.createString(NamespacedSchema.ensureNamespaced(MAP.getOrDefault(lowerCaseMotive, lowerCaseMotive))));
      } else {
         return input;
      }
   }

   @Override
   protected Typed<?> fix(final Typed<?> entity) {
      return entity.update(DSL.remainderFinder(), this::fixTag);
   }
}
