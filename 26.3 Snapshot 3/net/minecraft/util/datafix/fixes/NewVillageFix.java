package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.CompoundList.CompoundListType;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class NewVillageFix extends DataFix {
   public NewVillageFix(final Schema outputSchema, final boolean changesType) {
      super(outputSchema, changesType);
   }

   protected TypeRewriteRule makeRule() {
      CompoundListType<String, ?> startsType = DSL.compoundList(DSL.string(), this.getInputSchema().getType(References.STRUCTURE_FEATURE));
      OpticFinder<? extends List<? extends Pair<String, ?>>> finder = startsType.finder();
      return this.cap(startsType);
   }

   private <SF> TypeRewriteRule cap(final CompoundListType<String, SF> startsType) {
      Type<?> chunkType = this.getInputSchema().getType(References.CHUNK);
      Type<?> structureType = this.getInputSchema().getType(References.STRUCTURE_FEATURE);
      OpticFinder<?> levelFinder = chunkType.findField("Level");
      OpticFinder<?> structuresFinder = levelFinder.type().findField("Structures");
      OpticFinder<?> startsFinder = structuresFinder.type().findField("Starts");
      OpticFinder<List<Pair<String, SF>>> listFinder = startsType.finder();
      return TypeRewriteRule.seq(
         this.fixTypeEverywhereTyped(
            "NewVillageFix",
            chunkType,
            input -> input.updateTyped(
               levelFinder,
               level -> level.updateTyped(
                  structuresFinder,
                  structures -> structures.updateTyped(
                        startsFinder,
                        starts -> starts.update(
                           listFinder,
                           list -> list.stream()
                              .filter(pair -> !Objects.equals(pair.getFirst(), "Village"))
                              .map(pair -> pair.mapFirst(name -> name.equals("New_Village") ? "Village" : name))
                              .collect(Collectors.toList())
                        )
                     )
                     .update(DSL.remainderFinder(), tag -> tag.update("References", references -> {
                        Optional<? extends Dynamic<?>> village = references.get("New_Village").result();
                        return ((Dynamic)DataFixUtils.orElse(village.map(v -> references.remove("New_Village").set("Village", v)), references))
                           .remove("Village");
                     }))
               )
            )
         ),
         this.fixTypeEverywhereTyped(
            "NewVillageStartFix",
            structureType,
            input -> input.update(
               DSL.remainderFinder(),
               tag -> tag.update(
                  "id",
                  id -> Objects.equals(NamespacedSchema.ensureNamespaced(id.asString("")), "minecraft:new_village") ? id.createString("minecraft:village") : id
               )
            )
         )
      );
   }
}
