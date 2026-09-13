package net.minecraft.client.gui.screens.inventory;

import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractCommandBlockEditScreen extends Screen {
   private static final Component SET_COMMAND_LABEL = Component.translatable("advMode.setCommand");
   private static final Component COMMAND_LABEL = Component.translatable("advMode.command");
   private static final Component PREVIOUS_OUTPUT_LABEL = Component.translatable("advMode.previousOutput");
   protected EditBox commandEdit;
   protected EditBox previousEdit;
   protected Button doneButton;
   protected Button cancelButton;
   protected CycleButton<Boolean> outputButton;
   CommandSuggestions commandSuggestions;

   public AbstractCommandBlockEditScreen() {
      super(GameNarrator.NO_TITLE);
   }

   @Override
   public void tick() {
      if (!this.getCommandBlock().isValid()) {
         this.onClose();
      }
   }

   abstract BaseCommandBlock getCommandBlock();

   abstract int getPreviousY();

   @Override
   protected void init() {
      boolean flag = this.getCommandBlock().isTrackOutput();
      this.commandEdit = new EditBox(this.font, this.width / 2 - 150, 50, 300, 20, Component.translatable("advMode.command")) {
         @Override
         protected MutableComponent createNarrationMessage() {
            return super.createNarrationMessage().append(AbstractCommandBlockEditScreen.this.commandSuggestions.getNarrationMessage());
         }
      };
      this.commandEdit.setMaxLength(32500);
      this.commandEdit.setResponder(this::onEdited);
      this.addWidget(this.commandEdit);
      this.previousEdit = new EditBox(this.font, this.width / 2 - 150, this.getPreviousY(), 276, 20, Component.translatable("advMode.previousOutput"));
      this.previousEdit.setMaxLength(32500);
      this.previousEdit.setEditable(false);
      this.previousEdit.setValue("-");
      this.addWidget(this.previousEdit);
      this.outputButton = this.addRenderableWidget(
         CycleButton.booleanBuilder(Component.literal("O"), Component.literal("X"), flag)
            .displayOnlyValue()
            .create(this.width / 2 + 150 - 20, this.getPreviousY(), 20, 20, Component.translatable("advMode.trackOutput"), (p_169596_, p_169597_) -> {
               BaseCommandBlock basecommandblock = this.getCommandBlock();
               basecommandblock.setTrackOutput(p_169597_);
               this.updatePreviousOutput(p_169597_);
            })
      );
      this.addExtraControls();
      this.doneButton = this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_DONE, p_97691_ -> this.onDone()).bounds(this.width / 2 - 4 - 150, this.height / 4 + 120 + 12, 150, 20).build()
      );
      this.cancelButton = this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_CANCEL, p_420755_ -> this.onClose()).bounds(this.width / 2 + 4, this.height / 4 + 120 + 12, 150, 20).build()
      );
      this.commandSuggestions = new CommandSuggestions(this.minecraft, this, this.commandEdit, this.font, true, true, 0, 7, false, Integer.MIN_VALUE);
      this.commandSuggestions.setAllowSuggestions(true);
      this.commandSuggestions.updateCommandInfo();
      this.updatePreviousOutput(flag);
   }

   protected void addExtraControls() {
   }

   @Override
   protected void setInitialFocus() {
      this.setInitialFocus(this.commandEdit);
   }

   @Override
   protected Component getUsageNarration() {
      return this.commandSuggestions.isVisible() ? this.commandSuggestions.getUsageNarration() : super.getUsageNarration();
   }

   @Override
   public void resize(int p_97678_, int p_97679_) {
      String s = this.commandEdit.getValue();
      this.init(p_97678_, p_97679_);
      this.commandEdit.setValue(s);
      this.commandSuggestions.updateCommandInfo();
   }

   protected void updatePreviousOutput(boolean p_169599_) {
      this.previousEdit.setValue(p_169599_ ? this.getCommandBlock().getLastOutput().getString() : "-");
   }

   protected void onDone() {
      this.populateAndSendPacket();
      BaseCommandBlock basecommandblock = this.getCommandBlock();
      if (!basecommandblock.isTrackOutput()) {
         basecommandblock.setLastOutput(null);
      }

      this.minecraft.setScreen(null);
   }

   protected abstract void populateAndSendPacket();

   private void onEdited(String p_97689_) {
      this.commandSuggestions.updateCommandInfo();
   }

   @Override
   public boolean isInGameUi() {
      return true;
   }

   @Override
   public boolean keyPressed(KeyEvent p_431601_) {
      if (this.commandSuggestions.keyPressed(p_431601_)) {
         return true;
      } else if (super.keyPressed(p_431601_)) {
         return true;
      } else if (p_431601_.isConfirmation()) {
         this.onDone();
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean mouseScrolled(double p_97659_, double p_97660_, double p_97661_, double p_299742_) {
      return this.commandSuggestions.mouseScrolled(p_299742_) ? true : super.mouseScrolled(p_97659_, p_97660_, p_97661_, p_299742_);
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent p_424709_, boolean p_427904_) {
      return this.commandSuggestions.mouseClicked(p_424709_) ? true : super.mouseClicked(p_424709_, p_427904_);
   }

   @Override
   public void render(GuiGraphics p_283074_, int p_97673_, int p_97674_, float p_97675_) {
      super.render(p_283074_, p_97673_, p_97674_, p_97675_);
      p_283074_.drawCenteredString(this.font, SET_COMMAND_LABEL, this.width / 2, 20, -1);
      p_283074_.drawString(this.font, COMMAND_LABEL, this.width / 2 - 150 + 1, 40, -6250336);
      this.commandEdit.render(p_283074_, p_97673_, p_97674_, p_97675_);
      int i = 75;
      if (!this.previousEdit.getValue().isEmpty()) {
         i += 46 + this.getPreviousY() - 135;
         p_283074_.drawString(this.font, PREVIOUS_OUTPUT_LABEL, this.width / 2 - 150 + 1, i + 4, -6250336);
         this.previousEdit.render(p_283074_, p_97673_, p_97674_, p_97675_);
      }

      this.commandSuggestions.render(p_283074_, p_97673_, p_97674_);
   }
}
