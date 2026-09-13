package net.minecraft.client.gui.screens.advancements;

import net.minecraft.advancements.AdvancementType;
import net.minecraft.resources.Identifier;

public enum AdvancementWidgetType {
   OBTAINED(
      Identifier.withDefaultNamespace("advancements/box_obtained"),
      Identifier.withDefaultNamespace("advancements/task_frame_obtained"),
      Identifier.withDefaultNamespace("advancements/challenge_frame_obtained"),
      Identifier.withDefaultNamespace("advancements/goal_frame_obtained")
   ),
   UNOBTAINED(
      Identifier.withDefaultNamespace("advancements/box_unobtained"),
      Identifier.withDefaultNamespace("advancements/task_frame_unobtained"),
      Identifier.withDefaultNamespace("advancements/challenge_frame_unobtained"),
      Identifier.withDefaultNamespace("advancements/goal_frame_unobtained")
   );

   private final Identifier boxSprite;
   private final Identifier taskFrameSprite;
   private final Identifier challengeFrameSprite;
   private final Identifier goalFrameSprite;

   AdvancementWidgetType(final Identifier boxSprite, final Identifier taskFrameSprite, final Identifier challengeFrameSprite, final Identifier goalFrameSprite) {
      this.boxSprite = boxSprite;
      this.taskFrameSprite = taskFrameSprite;
      this.challengeFrameSprite = challengeFrameSprite;
      this.goalFrameSprite = goalFrameSprite;
   }

   public Identifier boxSprite() {
      return this.boxSprite;
   }

   public Identifier frameSprite(final AdvancementType type) {
      return switch (type) {
         case TASK -> this.taskFrameSprite;
         case CHALLENGE -> this.challengeFrameSprite;
         case GOAL -> this.goalFrameSprite;
      };
   }
}
