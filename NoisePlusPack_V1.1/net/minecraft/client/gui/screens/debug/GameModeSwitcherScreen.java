package net.minecraft.client.gui.screens.debug;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ServerboundChangeGameModePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.commands.GameModeCommand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GameModeSwitcherScreen extends Screen {
   static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("gamemode_switcher/slot");
   static final Identifier SELECTION_SPRITE = Identifier.withDefaultNamespace("gamemode_switcher/selection");
   private static final Identifier GAMEMODE_SWITCHER_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/gamemode_switcher.png");
   private static final int SPRITE_SHEET_WIDTH = 128;
   private static final int SPRITE_SHEET_HEIGHT = 128;
   private static final int SLOT_AREA = 26;
   private static final int SLOT_PADDING = 5;
   private static final int SLOT_AREA_PADDED = 31;
   private static final int HELP_TIPS_OFFSET_Y = 5;
   private static final int ALL_SLOTS_WIDTH = GameModeSwitcherScreen.GameModeIcon.values().length * 31 - 5;
   private final GameModeSwitcherScreen.GameModeIcon previousHovered;
   private GameModeSwitcherScreen.GameModeIcon currentlyHovered;
   private int firstMouseX;
   private int firstMouseY;
   private boolean setFirstMousePos;
   private final List<GameModeSwitcherScreen.GameModeSlot> slots = Lists.newArrayList();

   public GameModeSwitcherScreen() {
      super(GameNarrator.NO_TITLE);
      this.previousHovered = GameModeSwitcherScreen.GameModeIcon.getFromGameType(this.getDefaultSelected());
      this.currentlyHovered = this.previousHovered;
   }

   private GameType getDefaultSelected() {
      MultiPlayerGameMode multiplayergamemode = Minecraft.getInstance().gameMode;
      GameType gametype = multiplayergamemode.getPreviousPlayerMode();
      if (gametype != null) {
         return gametype;
      } else {
         return multiplayergamemode.getPlayerMode() == GameType.CREATIVE ? GameType.SURVIVAL : GameType.CREATIVE;
      }
   }

   @Override
   protected void init() {
      super.init();
      this.slots.clear();
      this.currentlyHovered = this.previousHovered;

      for (int i = 0; i < GameModeSwitcherScreen.GameModeIcon.VALUES.length; i++) {
         GameModeSwitcherScreen.GameModeIcon gamemodeswitcherscreen$gamemodeicon = GameModeSwitcherScreen.GameModeIcon.VALUES[i];
         this.slots
            .add(
               new GameModeSwitcherScreen.GameModeSlot(gamemodeswitcherscreen$gamemodeicon, this.width / 2 - ALL_SLOTS_WIDTH / 2 + i * 31, this.height / 2 - 31)
            );
      }
   }

   @Override
   public void render(GuiGraphics p_281834_, int p_283223_, int p_282178_, float p_281339_) {
      p_281834_.drawCenteredString(this.font, this.currentlyHovered.name, this.width / 2, this.height / 2 - 31 - 20, -1);
      MutableComponent mutablecomponent = Component.translatable(
         "debug.gamemodes.select_next", this.minecraft.options.keyDebugSwitchGameMode.getTranslatedKeyMessage().copy().withStyle(ChatFormatting.AQUA)
      );
      p_281834_.drawCenteredString(this.font, mutablecomponent, this.width / 2, this.height / 2 + 5, -1);
      if (!this.setFirstMousePos) {
         this.firstMouseX = p_283223_;
         this.firstMouseY = p_282178_;
         this.setFirstMousePos = true;
      }

      boolean flag = this.firstMouseX == p_283223_ && this.firstMouseY == p_282178_;

      for (GameModeSwitcherScreen.GameModeSlot gamemodeswitcherscreen$gamemodeslot : this.slots) {
         gamemodeswitcherscreen$gamemodeslot.render(p_281834_, p_283223_, p_282178_, p_281339_);
         gamemodeswitcherscreen$gamemodeslot.setSelected(this.currentlyHovered == gamemodeswitcherscreen$gamemodeslot.icon);
         if (!flag && gamemodeswitcherscreen$gamemodeslot.isHoveredOrFocused()) {
            this.currentlyHovered = gamemodeswitcherscreen$gamemodeslot.icon;
         }
      }
   }

   @Override
   public void renderBackground(GuiGraphics p_300820_, int p_297775_, int p_300982_, float p_298921_) {
      int i = this.width / 2 - 62;
      int j = this.height / 2 - 31 - 27;
      p_300820_.blit(RenderPipelines.GUI_TEXTURED, GAMEMODE_SWITCHER_LOCATION, i, j, 0.0F, 0.0F, 125, 75, 128, 128);
   }

   private void switchToHoveredGameMode() {
      switchToHoveredGameMode(this.minecraft, this.currentlyHovered);
   }

   private static void switchToHoveredGameMode(Minecraft p_281340_, GameModeSwitcherScreen.GameModeIcon p_281358_) {
      if (p_281340_.canSwitchGameMode()) {
         GameModeSwitcherScreen.GameModeIcon gamemodeswitcherscreen$gamemodeicon = GameModeSwitcherScreen.GameModeIcon.getFromGameType(
            p_281340_.gameMode.getPlayerMode()
         );
         if (p_281358_ != gamemodeswitcherscreen$gamemodeicon && GameModeCommand.PERMISSION_CHECK.check(p_281340_.player.permissions())) {
            p_281340_.player.connection.send(new ServerboundChangeGameModePacket(p_281358_.mode));
         }
      }
   }

   @Override
   public boolean keyPressed(KeyEvent p_428664_) {
      if (this.minecraft.options.keyDebugSwitchGameMode.matches(p_428664_)) {
         this.setFirstMousePos = false;
         this.currentlyHovered = this.currentlyHovered.getNext();
         return true;
      } else {
         return super.keyPressed(p_428664_);
      }
   }

   @Override
   public boolean keyReleased(KeyEvent p_457048_) {
      if (this.minecraft.options.keyDebugModifier.matches(p_457048_)) {
         this.switchToHoveredGameMode();
         this.minecraft.setScreen(null);
         return true;
      } else {
         return super.keyReleased(p_457048_);
      }
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent p_455998_) {
      if (this.minecraft.options.keyDebugModifier.matchesMouse(p_455998_)) {
         this.switchToHoveredGameMode();
         this.minecraft.setScreen(null);
         return true;
      } else {
         return super.mouseReleased(p_455998_);
      }
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   @OnlyIn(Dist.CLIENT)
   enum GameModeIcon {
      CREATIVE(Component.translatable("gameMode.creative"), GameType.CREATIVE, new ItemStack(Blocks.GRASS_BLOCK)),
      SURVIVAL(Component.translatable("gameMode.survival"), GameType.SURVIVAL, new ItemStack(Items.IRON_SWORD)),
      ADVENTURE(Component.translatable("gameMode.adventure"), GameType.ADVENTURE, new ItemStack(Items.MAP)),
      SPECTATOR(Component.translatable("gameMode.spectator"), GameType.SPECTATOR, new ItemStack(Items.ENDER_EYE));

      static final GameModeSwitcherScreen.GameModeIcon[] VALUES = values();
      private static final int ICON_AREA = 16;
      private static final int ICON_TOP_LEFT = 5;
      final Component name;
      final GameType mode;
      private final ItemStack renderStack;

      GameModeIcon(final Component p_97594_, final GameType p_410562_, final ItemStack p_97596_) {
         this.name = p_97594_;
         this.mode = p_410562_;
         this.renderStack = p_97596_;
      }

      void drawIcon(GuiGraphics p_282609_, int p_283301_, int p_281692_) {
         p_282609_.renderItem(this.renderStack, p_283301_, p_281692_);
      }

      GameModeSwitcherScreen.GameModeIcon getNext() {
         return switch (this) {
            case CREATIVE -> SURVIVAL;
            case SURVIVAL -> ADVENTURE;
            case ADVENTURE -> SPECTATOR;
            case SPECTATOR -> CREATIVE;
         };
      }

      static GameModeSwitcherScreen.GameModeIcon getFromGameType(GameType p_283307_) {
         return switch (p_283307_) {
            case SPECTATOR -> SPECTATOR;
            case SURVIVAL -> SURVIVAL;
            case CREATIVE -> CREATIVE;
            case ADVENTURE -> ADVENTURE;
         };
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class GameModeSlot extends AbstractWidget {
      final GameModeSwitcherScreen.GameModeIcon icon;
      private boolean isSelected;

      public GameModeSlot(GameModeSwitcherScreen.GameModeIcon p_97627_, int p_97628_, int p_97629_) {
         super(p_97628_, p_97629_, 26, 26, p_97627_.name);
         this.icon = p_97627_;
      }

      @Override
      public void renderWidget(GuiGraphics p_281380_, int p_283094_, int p_283558_, float p_282631_) {
         this.drawSlot(p_281380_);
         if (this.isSelected) {
            this.drawSelection(p_281380_);
         }

         this.icon.drawIcon(p_281380_, this.getX() + 5, this.getY() + 5);
      }

      @Override
      public void updateWidgetNarration(NarrationElementOutput p_259120_) {
         this.defaultButtonNarrationText(p_259120_);
      }

      @Override
      public boolean isHoveredOrFocused() {
         return super.isHoveredOrFocused() || this.isSelected;
      }

      public void setSelected(boolean p_97644_) {
         this.isSelected = p_97644_;
      }

      private void drawSlot(GuiGraphics p_281786_) {
         p_281786_.blitSprite(RenderPipelines.GUI_TEXTURED, GameModeSwitcherScreen.SLOT_SPRITE, this.getX(), this.getY(), 26, 26);
      }

      private void drawSelection(GuiGraphics p_281820_) {
         p_281820_.blitSprite(RenderPipelines.GUI_TEXTURED, GameModeSwitcherScreen.SELECTION_SPRITE, this.getX(), this.getY(), 26, 26);
      }
   }
}
