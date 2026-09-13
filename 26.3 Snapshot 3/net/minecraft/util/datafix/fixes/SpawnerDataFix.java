package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.List;

public class SpawnerDataFix extends DataFix {
   public SpawnerDataFix(final Schema outputSchema) {
      super(outputSchema, true);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> oldType = this.getInputSchema().getType(References.UNTAGGED_SPAWNER);
      Type<?> newType = this.getOutputSchema().getType(References.UNTAGGED_SPAWNER);
      OpticFinder<?> spawnDataFinder = oldType.findField("SpawnData");
      Type<?> newSpawnDataType = newType.findField("SpawnData").type();
      OpticFinder<?> spawnPotentialsFinder = oldType.findField("SpawnPotentials");
      Type<?> newSpawnPotentialsType = newType.findField("SpawnPotentials").type();
      return this.fixTypeEverywhereTyped(
         "Fix mob spawner data structure",
         oldType,
         newType,
         spawner -> spawner.updateTyped(spawnDataFinder, newSpawnDataType, spawnData -> this.wrapEntityToSpawnData(newSpawnDataType, spawnData))
            .updateTyped(
               spawnPotentialsFinder,
               newSpawnPotentialsType,
               spawnPotentials -> this.wrapSpawnPotentialsToWeightedEntries(newSpawnPotentialsType, spawnPotentials)
            )
      );
   }

   private <T> Typed<T> wrapEntityToSpawnData(final Type<T> newType, final Typed<?> spawnData) {
      DynamicOps<?> ops = spawnData.getOps();
      return new Typed(newType, ops, Pair.of(spawnData.getValue(), new Dynamic(ops)));
   }

   private <T> Typed<T> wrapSpawnPotentialsToWeightedEntries(final Type<T> newType, final Typed<?> spawnPotentials) {
      DynamicOps<?> ops = spawnPotentials.getOps();
      List<?> entries = (List<?>)spawnPotentials.getValue();
      List<?> wrappedEntries = entries.stream().map(o -> {
         Pair<Object, Dynamic<?>> entry = (Pair<Object, Dynamic<?>>)o;
         int weight = ((Dynamic)entry.getSecond()).get("Weight").asNumber().result().orElse(1).intValue();
         Dynamic<?> newEntryRemainder = new Dynamic(ops);
         newEntryRemainder = newEntryRemainder.set("weight", newEntryRemainder.createInt(weight));
         Dynamic<?> newInnerRemainder = ((Dynamic)entry.getSecond()).remove("Weight").remove("Entity");
         return Pair.of(Pair.of(entry.getFirst(), newInnerRemainder), newEntryRemainder);
      }).toList();
      return new Typed(newType, ops, wrappedEntries);
   }
}
