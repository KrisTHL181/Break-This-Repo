package net.minecraft.world.attribute;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public record BedRule(BedRule.Rule canSleep, BedRule.Rule canSetSpawn, boolean destroyOnUse, boolean destroyOnLeave, Optional<Component> errorMessage) {
   public static final BedRule CAN_SLEEP_WHEN_DARK = new BedRule(
      BedRule.Rule.WHEN_DARK, BedRule.Rule.ALWAYS, false, false, Optional.of(Component.translatable("block.minecraft.bed.no_sleep"))
   );
   public static final BedRule DESTROY_ON_USE = new BedRule(BedRule.Rule.NEVER, BedRule.Rule.NEVER, true, false, Optional.empty());
   public static final BedRule DESTROY_ON_LEAVE = new BedRule(
      BedRule.Rule.WHEN_DARK, BedRule.Rule.NEVER, false, true, Optional.of(Component.translatable("block.minecraft.bed.no_sleep"))
   );
   public static final Codec<BedRule> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            BedRule.Rule.CODEC.fieldOf("can_sleep").forGetter(BedRule::canSleep),
            BedRule.Rule.CODEC.fieldOf("can_set_spawn").forGetter(BedRule::canSetSpawn),
            Codec.BOOL.optionalFieldOf("destroy_on_use", false).forGetter(BedRule::destroyOnUse),
            Codec.BOOL.optionalFieldOf("destroy_on_leave", false).forGetter(BedRule::destroyOnLeave),
            ComponentSerialization.CODEC.optionalFieldOf("error_message").forGetter(BedRule::errorMessage)
         )
         .apply(i, BedRule::new)
   );

   public boolean canSleep(final Level level) {
      return this.canSleep.test(level);
   }

   public boolean canSetSpawn(final Level level) {
      return this.canSetSpawn.test(level);
   }

   public Player.BedSleepingProblem asProblem() {
      return new Player.BedSleepingProblem(this.errorMessage.orElse(null));
   }

   public enum Rule implements StringRepresentable {
      ALWAYS("always"),
      WHEN_DARK("when_dark"),
      NEVER("never");

      public static final Codec<BedRule.Rule> CODEC = StringRepresentable.fromEnum(BedRule.Rule::values);
      private final String name;

      Rule(final String name) {
         this.name = name;
      }

      public boolean test(final Level level) {
         return switch (this) {
            case ALWAYS -> true;
            case WHEN_DARK -> level.isDarkOutside();
            case NEVER -> false;
         };
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }
   }
}
