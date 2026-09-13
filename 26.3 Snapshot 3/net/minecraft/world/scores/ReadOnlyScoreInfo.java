package net.minecraft.world.scores;

import java.util.Objects;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.numbers.NumberFormat;
import org.jspecify.annotations.Nullable;

public interface ReadOnlyScoreInfo {
   int value();

   boolean isLocked();

   @Nullable NumberFormat numberFormat();

   default MutableComponent formatValue(final NumberFormat defaultFormat) {
      return Objects.requireNonNullElse(this.numberFormat(), defaultFormat).format(this.value());
   }
}
