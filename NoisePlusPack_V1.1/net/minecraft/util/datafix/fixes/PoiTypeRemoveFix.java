package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class PoiTypeRemoveFix extends AbstractPoiSectionFix {
   private final Predicate<String> typesToKeep;

   public PoiTypeRemoveFix(Schema p_216701_, String p_216702_, Predicate<String> p_216703_) {
      super(p_216701_, p_216702_);
      this.typesToKeep = p_216703_.negate();
   }

   @Override
   protected <T> Stream<Dynamic<T>> processRecords(Stream<Dynamic<T>> p_216707_) {
      return p_216707_.filter(this::shouldKeepRecord);
   }

   private <T> boolean shouldKeepRecord(Dynamic<T> p_216705_) {
      return p_216705_.get("type").asString().result().filter(this.typesToKeep).isPresent();
   }
}
