package net.minecraft.client.gui.screens.advancements;

import net.minecraft.advancements.AdvancementType;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
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

   AdvancementWidgetType(final Identifier p_451998_, final Identifier p_454128_, final Identifier p_456436_, final Identifier p_459461_) {
      this.boxSprite = p_451998_;
      this.taskFrameSprite = p_454128_;
      this.challengeFrameSprite = p_456436_;
      this.goalFrameSprite = p_459461_;
   }

   public Identifier boxSprite() {
      return this.boxSprite;
   }

   public Identifier frameSprite(AdvancementType p_311711_) {
      return switch (p_311711_) {
         case TASK -> this.taskFrameSprite;
         case CHALLENGE -> this.challengeFrameSprite;
         case GOAL -> this.goalFrameSprite;
      };
   }
}
