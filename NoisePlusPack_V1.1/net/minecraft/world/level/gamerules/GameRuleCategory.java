package net.minecraft.world.level.gamerules;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

public record GameRuleCategory(Identifier id) {
   private static final List<GameRuleCategory> SORT_ORDER = new ArrayList<>();
   public static final GameRuleCategory PLAYER = register("player");
   public static final GameRuleCategory MOBS = register("mobs");
   public static final GameRuleCategory SPAWNING = register("spawning");
   public static final GameRuleCategory DROPS = register("drops");
   public static final GameRuleCategory UPDATES = register("updates");
   public static final GameRuleCategory CHAT = register("chat");
   public static final GameRuleCategory MISC = register("misc");

   public Identifier getDescriptionId() {
      return this.id;
   }

   private static GameRuleCategory register(String p_452327_) {
      return register(Identifier.withDefaultNamespace(p_452327_));
   }

   public static GameRuleCategory register(Identifier p_455769_) {
      GameRuleCategory gamerulecategory = new GameRuleCategory(p_455769_);
      if (SORT_ORDER.contains(gamerulecategory)) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "Category '%s' is already registered.", p_455769_));
      }

      SORT_ORDER.add(gamerulecategory);
      return gamerulecategory;
   }

   public MutableComponent label() {
      return Component.translatable(this.id.toLanguageKey("gamerule.category"));
   }
}
