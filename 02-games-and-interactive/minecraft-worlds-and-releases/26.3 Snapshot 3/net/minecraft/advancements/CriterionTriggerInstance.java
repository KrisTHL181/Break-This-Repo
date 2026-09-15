package net.minecraft.advancements;

import net.minecraft.world.level.storage.loot.ValidationContextSource;

public interface CriterionTriggerInstance {
   void validate(ValidationContextSource validator);
}
