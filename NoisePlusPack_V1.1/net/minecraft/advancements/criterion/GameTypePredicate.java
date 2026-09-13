package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import java.util.Arrays;
import java.util.List;
import net.minecraft.world.level.GameType;

public record GameTypePredicate(List<GameType> types) {
   public static final GameTypePredicate ANY = of(GameType.values());
   public static final GameTypePredicate SURVIVAL_LIKE = of(GameType.SURVIVAL, GameType.ADVENTURE);
   public static final Codec<GameTypePredicate> CODEC = GameType.CODEC.listOf().xmap(GameTypePredicate::new, GameTypePredicate::types);

   public static GameTypePredicate of(GameType... p_452164_) {
      return new GameTypePredicate(Arrays.stream(p_452164_).toList());
   }

   public boolean matches(GameType p_460269_) {
      return this.types.contains(p_460269_);
   }
}
