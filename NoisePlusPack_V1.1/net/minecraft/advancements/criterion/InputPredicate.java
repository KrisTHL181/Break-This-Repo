package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.world.entity.player.Input;

public record InputPredicate(
   Optional<Boolean> forward,
   Optional<Boolean> backward,
   Optional<Boolean> left,
   Optional<Boolean> right,
   Optional<Boolean> jump,
   Optional<Boolean> sneak,
   Optional<Boolean> sprint
) {
   public static final Codec<InputPredicate> CODEC = RecordCodecBuilder.create(
      p_457098_ -> p_457098_.group(
            Codec.BOOL.optionalFieldOf("forward").forGetter(InputPredicate::forward),
            Codec.BOOL.optionalFieldOf("backward").forGetter(InputPredicate::backward),
            Codec.BOOL.optionalFieldOf("left").forGetter(InputPredicate::left),
            Codec.BOOL.optionalFieldOf("right").forGetter(InputPredicate::right),
            Codec.BOOL.optionalFieldOf("jump").forGetter(InputPredicate::jump),
            Codec.BOOL.optionalFieldOf("sneak").forGetter(InputPredicate::sneak),
            Codec.BOOL.optionalFieldOf("sprint").forGetter(InputPredicate::sprint)
         )
         .apply(p_457098_, InputPredicate::new)
   );

   public boolean matches(Input p_450726_) {
      return this.matches(this.forward, p_450726_.forward())
         && this.matches(this.backward, p_450726_.backward())
         && this.matches(this.left, p_450726_.left())
         && this.matches(this.right, p_450726_.right())
         && this.matches(this.jump, p_450726_.jump())
         && this.matches(this.sneak, p_450726_.shift())
         && this.matches(this.sprint, p_450726_.sprint());
   }

   private boolean matches(Optional<Boolean> p_453005_, boolean p_454250_) {
      return p_453005_.<Boolean>map(p_456130_ -> p_456130_ == p_454250_).orElse(true);
   }
}
