package net.minecraft.advancements.criterion;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderGetter;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

public class CriterionValidator {
   private final ProblemReporter reporter;
   private final HolderGetter.Provider lootData;

   public CriterionValidator(ProblemReporter p_458148_, HolderGetter.Provider p_454922_) {
      this.reporter = p_458148_;
      this.lootData = p_454922_;
   }

   public void validateEntity(Optional<ContextAwarePredicate> p_460767_, String p_455131_) {
      p_460767_.ifPresent(p_456612_ -> this.validateEntity(p_456612_, p_455131_));
   }

   public void validateEntities(List<ContextAwarePredicate> p_455058_, String p_453050_) {
      this.validate(p_455058_, LootContextParamSets.ADVANCEMENT_ENTITY, p_453050_);
   }

   public void validateEntity(ContextAwarePredicate p_454674_, String p_450357_) {
      this.validate(p_454674_, LootContextParamSets.ADVANCEMENT_ENTITY, p_450357_);
   }

   public void validate(ContextAwarePredicate p_456340_, ContextKeySet p_453179_, String p_450745_) {
      p_456340_.validate(new ValidationContext(this.reporter.forChild(new ProblemReporter.FieldPathElement(p_450745_)), p_453179_, this.lootData));
   }

   public void validate(List<ContextAwarePredicate> p_450485_, ContextKeySet p_450190_, String p_454315_) {
      for (int i = 0; i < p_450485_.size(); i++) {
         ContextAwarePredicate contextawarepredicate = p_450485_.get(i);
         contextawarepredicate.validate(
            new ValidationContext(this.reporter.forChild(new ProblemReporter.IndexedFieldPathElement(p_454315_, i)), p_450190_, this.lootData)
         );
      }
   }
}
