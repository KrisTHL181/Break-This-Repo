package net.minecraft.world.attribute;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public record BedRule(BedRule.Rule canSleep, BedRule.Rule canSetSpawn, boolean explodes, Optional<Component> errorMessage) {
   public static final BedRule CAN_SLEEP_WHEN_DARK = new BedRule(
      BedRule.Rule.WHEN_DARK, BedRule.Rule.ALWAYS, false, Optional.of(Component.translatable("block.minecraft.bed.no_sleep"))
   );
   public static final BedRule EXPLODES = new BedRule(BedRule.Rule.NEVER, BedRule.Rule.NEVER, true, Optional.empty());
   public static final Codec<BedRule> CODEC = RecordCodecBuilder.create(
      p_460717_ -> p_460717_.group(
            BedRule.Rule.CODEC.fieldOf("can_sleep").forGetter(BedRule::canSleep),
            BedRule.Rule.CODEC.fieldOf("can_set_spawn").forGetter(BedRule::canSetSpawn),
            Codec.BOOL.optionalFieldOf("explodes", false).forGetter(BedRule::explodes),
            ComponentSerialization.CODEC.optionalFieldOf("error_message").forGetter(BedRule::errorMessage)
         )
         .apply(p_460717_, BedRule::new)
   );

   public boolean canSleep(Level p_456134_) {
      return this.canSleep.test(p_456134_);
   }

   public boolean canSetSpawn(Level p_456584_) {
      return this.canSetSpawn.test(p_456584_);
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

      Rule(final String p_454242_) {
         this.name = p_454242_;
      }

      public boolean test(Level p_452554_) {
         return switch (this) {
            case ALWAYS -> true;
            case WHEN_DARK -> p_452554_.isDarkOutside();
            case NEVER -> false;
         };
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }
   }
}
