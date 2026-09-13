package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;

public class MemoryExpiryDataFix extends NamedEntityFix {
   public MemoryExpiryDataFix(final Schema schema, final String entityType) {
      super(schema, false, "Memory expiry data fix (" + entityType + ")", References.ENTITY, entityType);
   }

   @Override
   protected Typed<?> fix(final Typed<?> entity) {
      return entity.update(DSL.remainderFinder(), this::fixTag);
   }

   public Dynamic<?> fixTag(final Dynamic<?> input) {
      return input.update("Brain", this::updateBrain);
   }

   private Dynamic<?> updateBrain(final Dynamic<?> input) {
      return input.update("memories", this::updateMemories);
   }

   private Dynamic<?> updateMemories(final Dynamic<?> memories) {
      return memories.updateMapValues(this::updateMemoryEntry);
   }

   private Pair<Dynamic<?>, Dynamic<?>> updateMemoryEntry(final Pair<Dynamic<?>, Dynamic<?>> memoryEntry) {
      return memoryEntry.mapSecond(this::wrapMemoryValue);
   }

   private Dynamic<?> wrapMemoryValue(final Dynamic<?> dynamic) {
      return dynamic.createMap(ImmutableMap.of(dynamic.createString("value"), dynamic));
   }
}
