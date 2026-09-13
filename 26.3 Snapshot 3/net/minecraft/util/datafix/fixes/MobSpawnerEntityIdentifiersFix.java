package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import java.util.stream.Stream;

public class MobSpawnerEntityIdentifiersFix extends DataFix {
   public MobSpawnerEntityIdentifiersFix(final Schema outputSchema, final boolean changesType) {
      super(outputSchema, changesType);
   }

   private Dynamic<?> fix(Dynamic<?> input) {
      if (!"MobSpawner".equals(input.get("id").asString(""))) {
         return input;
      }

      Optional<String> entityId = input.get("EntityId").asString().result();
      if (entityId.isPresent()) {
         Dynamic<?> spawnData = (Dynamic<?>)DataFixUtils.orElse(input.get("SpawnData").result(), input.emptyMap());
         spawnData = spawnData.set("id", spawnData.createString(entityId.get().isEmpty() ? "Pig" : entityId.get()));
         input = input.set("SpawnData", spawnData);
         input = input.remove("EntityId");
      }

      Optional<? extends Stream<? extends Dynamic<?>>> spawnPotentials = input.get("SpawnPotentials").asStreamOpt().result();
      if (spawnPotentials.isPresent()) {
         input = input.set(
            "SpawnPotentials",
            input.createList(
               spawnPotentials.get()
                  .map(
                     spawnPotential -> {
                        Optional<String> type = spawnPotential.get("Type").asString().result();
                        if (type.isPresent()) {
                           Dynamic<?> spawnData = ((Dynamic)DataFixUtils.orElse(spawnPotential.get("Properties").result(), spawnPotential.emptyMap()))
                              .set("id", spawnPotential.createString(type.get()));
                           return spawnPotential.set("Entity", spawnData).remove("Type").remove("Properties");
                        } else {
                           return (Dynamic)spawnPotential;
                        }
                     }
                  )
            )
         );
      }

      return input;
   }

   public TypeRewriteRule makeRule() {
      Type<?> newType = this.getOutputSchema().getType(References.UNTAGGED_SPAWNER);
      return this.fixTypeEverywhereTyped("MobSpawnerEntityIdentifiersFix", this.getInputSchema().getType(References.UNTAGGED_SPAWNER), newType, input -> {
         Dynamic<?> tag = (Dynamic<?>)input.get(DSL.remainderFinder());
         tag = tag.set("id", tag.createString("MobSpawner"));
         DataResult<? extends Pair<? extends Typed<?>, ?>> fixed = newType.readTyped(this.fix(tag));
         return fixed.result().isEmpty() ? input : (Typed)((Pair)fixed.result().get()).getFirst();
      });
   }
}
