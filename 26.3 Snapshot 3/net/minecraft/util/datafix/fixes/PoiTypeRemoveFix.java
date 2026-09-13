package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class PoiTypeRemoveFix extends AbstractPoiSectionFix {
   private final Predicate<String> typesToKeep;

   public PoiTypeRemoveFix(final Schema outputSchema, final String name, final Predicate<String> typesToRemove) {
      super(outputSchema, name);
      this.typesToKeep = typesToRemove.negate();
   }

   @Override
   protected <T> Stream<Dynamic<T>> processRecords(final Stream<Dynamic<T>> records) {
      return records.filter(this::shouldKeepRecord);
   }

   private <T> boolean shouldKeepRecord(final Dynamic<T> record) {
      return record.get("type").asString().result().filter(this.typesToKeep).isPresent();
   }
}
