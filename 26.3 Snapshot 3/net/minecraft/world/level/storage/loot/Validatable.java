package net.minecraft.world.level.storage.loot;

import com.mojang.serialization.DataResult;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.context.ContextKeySet;

public interface Validatable {
   void validate(ValidationContext context);

   static void validate(final ValidationContext context, final String name, final Validatable v) {
      v.validate(context.forField(name));
   }

   static void validate(final ValidationContext context, final String name, final Optional<? extends Validatable> optional) {
      optional.ifPresent(v -> v.validate(context.forField(name)));
   }

   static void validate(final ValidationContext context, final String name, final List<? extends Validatable> list) {
      for (int i = 0; i < list.size(); i++) {
         list.get(i).validate(context.forIndexedField(name, i));
      }
   }

   static void validate(final ValidationContext context, final List<? extends Validatable> list) {
      for (int i = 0; i < list.size(); i++) {
         list.get(i).validate(context.forChild(new ProblemReporter.IndexedPathElement(i)));
      }
   }

   static <T extends Validatable> void validateReference(final ValidationContext context, final ResourceKey<T> id) {
      if (!context.allowsReferences()) {
         context.reportProblem(new ValidationContext.ReferenceNotAllowedProblem(id));
      } else if (context.hasVisitedElement(id)) {
         context.reportProblem(new ValidationContext.RecursiveReferenceProblem(id));
      } else {
         context.resolver()
            .get(id)
            .ifPresentOrElse(
               element -> element.value().validate(context.enterElement(new ProblemReporter.ElementReferencePathElement(id), id)),
               () -> context.reportProblem(new ValidationContext.MissingReferenceProblem(id))
            );
      }
   }

   static <T extends Validatable> Function<T, DataResult<T>> validatorForContext(final ContextKeySet params) {
      return v -> {
         ProblemReporter.Collector problemCollector = new ProblemReporter.Collector();
         ValidationContext validationContext = new ValidationContext(problemCollector, params);
         v.validate(validationContext);
         return !problemCollector.isEmpty() ? DataResult.error(() -> "Validation error: " + problemCollector.getReport()) : DataResult.success(v);
      };
   }

   static <T extends Validatable> Function<List<T>, DataResult<List<T>>> listValidatorForContext(final ContextKeySet params) {
      return v -> {
         ProblemReporter.Collector problemCollector = new ProblemReporter.Collector();
         ValidationContext validationContext = new ValidationContext(problemCollector, params);
         validate(validationContext, v);
         return !problemCollector.isEmpty() ? DataResult.error(() -> "Validation error: " + problemCollector.getReport()) : DataResult.success(v);
      };
   }
}
