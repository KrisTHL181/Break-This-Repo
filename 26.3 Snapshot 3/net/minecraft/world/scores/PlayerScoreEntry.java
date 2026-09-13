package net.minecraft.world.scores;

import java.util.Objects;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.numbers.NumberFormat;
import org.jspecify.annotations.Nullable;

public record PlayerScoreEntry(String owner, int value, @Nullable Component display, @Nullable NumberFormat numberFormatOverride) {
   public boolean isHidden() {
      return this.owner.startsWith("#");
   }

   public Component ownerName() {
      return this.display != null ? this.display : Component.literal(this.owner());
   }

   public MutableComponent formatValue(final NumberFormat _default) {
      return Objects.requireNonNullElse(this.numberFormatOverride, _default).format(this.value);
   }
}
