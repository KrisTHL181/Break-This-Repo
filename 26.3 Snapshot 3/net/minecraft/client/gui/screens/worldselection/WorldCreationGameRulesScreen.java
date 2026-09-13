package net.minecraft.client.gui.screens.worldselection;

import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.world.level.gamerules.GameRules;

public class WorldCreationGameRulesScreen extends AbstractGameRulesScreen {
   public WorldCreationGameRulesScreen(final GameRules gameRules, final Consumer<Optional<GameRules>> exitCallback) {
      super(gameRules, exitCallback);
   }

   @Override
   protected void initContent() {
      this.ruleList = this.layout.addToContents(new AbstractGameRulesScreen.RuleList(this.gameRules));
   }

   @Override
   protected void onDone() {
      this.closeAndApplyChanges();
   }

   @Override
   public void onClose() {
      this.closeAndDiscardChanges();
   }
}
