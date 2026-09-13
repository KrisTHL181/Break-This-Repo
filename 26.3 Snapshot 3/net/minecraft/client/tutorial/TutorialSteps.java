package net.minecraft.client.tutorial;

import java.util.function.Function;

public enum TutorialSteps {
   MOVEMENT("movement", MovementTutorialStepInstance::new),
   FIND_TREE("find_tree", FindTreeTutorialStepInstance::new),
   PUNCH_TREE("punch_tree", PunchTreeTutorialStepInstance::new),
   OPEN_INVENTORY("open_inventory", OpenInventoryTutorialStep::new),
   CRAFT_PLANKS("craft_planks", CraftPlanksTutorialStep::new),
   NONE("none", CompletedTutorialStepInstance::new);

   private final String name;
   private final Function<Tutorial, ? extends TutorialStepInstance> constructor;

   <T extends TutorialStepInstance> TutorialSteps(final String name, final Function<Tutorial, T> constructor) {
      this.name = name;
      this.constructor = constructor;
   }

   public TutorialStepInstance create(final Tutorial tutorial) {
      return this.constructor.apply(tutorial);
   }

   public String getName() {
      return this.name;
   }

   public static TutorialSteps getByName(final String name) {
      for (TutorialSteps step : values()) {
         if (step.name.equals(name)) {
            return step;
         }
      }

      return NONE;
   }
}
