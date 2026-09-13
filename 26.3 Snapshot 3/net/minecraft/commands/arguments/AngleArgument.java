package net.minecraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Arrays;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.WorldCoordinate;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class AngleArgument implements ArgumentType<AngleArgument.SingleAngle> {
   private static final Collection<String> EXAMPLES = Arrays.asList("0", "~", "~-5");
   public static final SimpleCommandExceptionType ERROR_NOT_COMPLETE = new SimpleCommandExceptionType(Component.translatable("argument.angle.incomplete"));
   public static final SimpleCommandExceptionType ERROR_INVALID_ANGLE = new SimpleCommandExceptionType(Component.translatable("argument.angle.invalid"));

   public static AngleArgument angle() {
      return new AngleArgument();
   }

   public static float getAngle(final CommandContext<CommandSourceStack> context, final String name) {
      return ((AngleArgument.SingleAngle)context.getArgument(name, AngleArgument.SingleAngle.class)).getAngle((CommandSourceStack)context.getSource());
   }

   public AngleArgument.SingleAngle parse(final StringReader reader) throws CommandSyntaxException {
      if (!reader.canRead()) {
         throw ERROR_NOT_COMPLETE.createWithContext(reader);
      } else {
         boolean isRelative = WorldCoordinate.isRelative(reader);
         float value = reader.canRead() && reader.peek() != ' ' ? reader.readFloat() : 0.0F;
         if (!Float.isNaN(value) && !Float.isInfinite(value)) {
            return new AngleArgument.SingleAngle(value, isRelative);
         } else {
            throw ERROR_INVALID_ANGLE.createWithContext(reader);
         }
      }
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   public static final class SingleAngle {
      private final float angle;
      private final boolean isRelative;

      private SingleAngle(final float angle, final boolean isRelative) {
         this.angle = angle;
         this.isRelative = isRelative;
      }

      public float getAngle(final CommandSourceStack sender) {
         return Mth.wrapDegrees(this.isRelative ? this.angle + sender.getRotation().y : this.angle);
      }
   }
}
