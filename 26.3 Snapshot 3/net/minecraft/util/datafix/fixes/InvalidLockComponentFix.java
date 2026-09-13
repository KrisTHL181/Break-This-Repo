package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;

public class InvalidLockComponentFix extends DataComponentRemainderFix {
   private static final Optional<String> INVALID_LOCK_CUSTOM_NAME = Optional.of("\"\"");

   public InvalidLockComponentFix(final Schema outputSchema) {
      super(outputSchema, "InvalidLockComponentPredicateFix", "minecraft:lock");
   }

   @Override
   protected <T> @Nullable Dynamic<T> fixComponent(final Dynamic<T> input) {
      return fixLock(input);
   }

   public static <T> @Nullable Dynamic<T> fixLock(final Dynamic<T> input) {
      return isBrokenLock(input) ? null : input;
   }

   private static <T> boolean isBrokenLock(final Dynamic<T> input) {
      return isMapWithOneField(
         input,
         "components",
         components -> isMapWithOneField(components, "minecraft:custom_name", customName -> customName.asString().result().equals(INVALID_LOCK_CUSTOM_NAME))
      );
   }

   private static <T> boolean isMapWithOneField(final Dynamic<T> input, final String fieldName, final Predicate<Dynamic<T>> predicate) {
      Optional<Map<Dynamic<T>, Dynamic<T>>> map = input.getMapValues().result();
      return !map.isEmpty() && map.get().size() == 1 ? input.get(fieldName).result().filter(predicate).isPresent() : false;
   }
}
