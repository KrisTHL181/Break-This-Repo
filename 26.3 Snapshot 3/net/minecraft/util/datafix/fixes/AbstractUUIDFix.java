package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public abstract class AbstractUUIDFix extends DataFix {
   protected final TypeReference typeReference;

   public AbstractUUIDFix(final Schema outputSchema, final TypeReference typeReference) {
      super(outputSchema, false);
      this.typeReference = typeReference;
   }

   protected Typed<?> updateNamedChoice(final Typed<?> input, final String name, final Function<Dynamic<?>, Dynamic<?>> function) {
      Type<?> oldType = this.getInputSchema().getChoiceType(this.typeReference, name);
      Type<?> newType = this.getOutputSchema().getChoiceType(this.typeReference, name);
      return input.updateTyped(DSL.namedChoice(name, oldType), newType, typedTag -> typedTag.update(DSL.remainderFinder(), function));
   }

   protected static Optional<Dynamic<?>> replaceUUIDString(final Dynamic<?> tag, final String oldKey, final String newKey) {
      return createUUIDFromString(tag, oldKey).map(uuidTag -> tag.remove(oldKey).set(newKey, uuidTag));
   }

   protected static Optional<Dynamic<?>> replaceUUIDMLTag(final Dynamic<?> tag, final String oldKey, final String newKey) {
      return tag.get(oldKey).result().flatMap(AbstractUUIDFix::createUUIDFromML).map(uuidTag -> tag.remove(oldKey).set(newKey, uuidTag));
   }

   protected static Optional<Dynamic<?>> replaceUUIDLeastMost(final Dynamic<?> tag, final String oldKey, final String newKey) {
      String mostKey = oldKey + "Most";
      String leastKey = oldKey + "Least";
      return createUUIDFromLongs(tag, mostKey, leastKey).map(uuidTag -> tag.remove(mostKey).remove(leastKey).set(newKey, uuidTag));
   }

   protected static Optional<Dynamic<?>> createUUIDFromString(final Dynamic<?> tag, final String oldKey) {
      return tag.get(oldKey).result().flatMap(uuidStringTag -> {
         String uuidString = uuidStringTag.asString(null);
         if (uuidString != null) {
            try {
               UUID uuid = UUID.fromString(uuidString);
               return createUUIDTag(tag, uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
            } catch (IllegalArgumentException var4) {
            }
         }

         return Optional.empty();
      });
   }

   protected static Optional<Dynamic<?>> createUUIDFromML(final Dynamic<?> tag) {
      return createUUIDFromLongs(tag, "M", "L");
   }

   protected static Optional<Dynamic<?>> createUUIDFromLongs(final Dynamic<?> tag, final String mostKey, final String leastKey) {
      long mostSignificantBits = tag.get(mostKey).asLong(0L);
      long leastSignificantBits = tag.get(leastKey).asLong(0L);
      return mostSignificantBits != 0L && leastSignificantBits != 0L ? createUUIDTag(tag, mostSignificantBits, leastSignificantBits) : Optional.empty();
   }

   protected static Optional<Dynamic<?>> createUUIDTag(final Dynamic<?> tag, final long mostSignificantBits, final long leastSignificantBits) {
      return Optional.of(
         tag.createIntList(
            Arrays.stream(new int[]{(int)(mostSignificantBits >> 32), (int)mostSignificantBits, (int)(leastSignificantBits >> 32), (int)leastSignificantBits})
         )
      );
   }
}
