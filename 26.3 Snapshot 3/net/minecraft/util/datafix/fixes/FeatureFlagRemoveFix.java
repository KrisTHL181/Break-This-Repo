package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class FeatureFlagRemoveFix extends DataFix {
   private final String name;
   private final Set<String> flagsToRemove;

   public FeatureFlagRemoveFix(final Schema outputSchema, final String name, final Set<String> flagsToRemove) {
      super(outputSchema, false);
      this.name = name;
      this.flagsToRemove = flagsToRemove;
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         this.name, this.getInputSchema().getType(References.LIGHTWEIGHT_LEVEL), input -> input.update(DSL.remainderFinder(), this::fixTag)
      );
   }

   private <T> Dynamic<T> fixTag(final Dynamic<T> tag) {
      List<Dynamic<T>> inactiveFeatures = tag.get("removed_features").asStream().collect(Collectors.toCollection(ArrayList::new));
      Dynamic<T> result = tag.update(
         "enabled_features", features -> (Dynamic)DataFixUtils.orElse(features.asStreamOpt().result().map(s -> s.filter(feature -> {
            Optional<String> asString = feature.asString().result();
            if (asString.isEmpty()) {
               return true;
            }

            boolean shouldRemove = this.flagsToRemove.contains(asString.get());
            if (shouldRemove) {
               inactiveFeatures.add(tag.createString(asString.get()));
            }

            return !shouldRemove;
         })).map(tag::createList), features)
      );
      if (!inactiveFeatures.isEmpty()) {
         result = result.set("removed_features", tag.createList(inactiveFeatures.stream()));
      }

      return result;
   }
}
