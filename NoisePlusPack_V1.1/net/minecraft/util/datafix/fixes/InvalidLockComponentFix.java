package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;

public class InvalidLockComponentFix extends DataComponentRemainderFix {
   private static final Optional<String> INVALID_LOCK_CUSTOM_NAME = Optional.of("\"\"");

   public InvalidLockComponentFix(Schema p_376929_) {
      super(p_376929_, "InvalidLockComponentPredicateFix", "minecraft:lock");
   }

   @Override
   protected <T> @Nullable Dynamic<T> fixComponent(Dynamic<T> p_377274_) {
      return fixLock(p_377274_);
   }

   public static <T> @Nullable Dynamic<T> fixLock(Dynamic<T> p_376516_) {
      return isBrokenLock(p_376516_) ? null : p_376516_;
   }

   private static <T> boolean isBrokenLock(Dynamic<T> p_375919_) {
      return isMapWithOneField(
         p_375919_,
         "components",
         p_378206_ -> isMapWithOneField(p_378206_, "minecraft:custom_name", p_377439_ -> p_377439_.asString().result().equals(INVALID_LOCK_CUSTOM_NAME))
      );
   }

   private static <T> boolean isMapWithOneField(Dynamic<T> p_378567_, String p_378713_, Predicate<Dynamic<T>> p_378445_) {
      Optional<Map<Dynamic<T>, Dynamic<T>>> optional = p_378567_.getMapValues().result();
      return !optional.isEmpty() && optional.get().size() == 1 ? p_378567_.get(p_378713_).result().filter(p_378445_).isPresent() : false;
   }
}
