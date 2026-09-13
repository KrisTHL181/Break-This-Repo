package net.minecraft.client.gui.screens.inventory;

import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCommandBlockPacket;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CommandBlockEditScreen extends AbstractCommandBlockEditScreen {
   private final CommandBlockEntity autoCommandBlock;
   private CycleButton<CommandBlockEntity.Mode> modeButton;
   private CycleButton<Boolean> conditionalButton;
   private CycleButton<Boolean> autoexecButton;
   private CommandBlockEntity.Mode mode = CommandBlockEntity.Mode.REDSTONE;
   private boolean conditional;
   private boolean autoexec;

   public CommandBlockEditScreen(CommandBlockEntity p_98382_) {
      this.autoCommandBlock = p_98382_;
   }

   @Override
   BaseCommandBlock getCommandBlock() {
      return this.autoCommandBlock.getCommandBlock();
   }

   @Override
   int getPreviousY() {
      return 135;
   }

   @Override
   protected void init() {
      super.init();
      this.enableControls(false);
   }

   @Override
   protected void addExtraControls() {
      this.modeButton = this.addRenderableWidget(
         CycleButton.<CommandBlockEntity.Mode>builder(p_325380_ -> {
               return switch (p_325380_) {
                  case SEQUENCE -> Component.translatable("advMode.mode.sequence");
                  case AUTO -> Component.translatable("advMode.mode.auto");
                  case REDSTONE -> Component.translatable("advMode.mode.redstone");
               };
            }, this.mode)
            .withValues(CommandBlockEntity.Mode.values())
            .displayOnlyValue()
            .create(this.width / 2 - 50 - 100 - 4, 165, 100, 20, Component.translatable("advMode.mode"), (p_169721_, p_169722_) -> this.mode = p_169722_)
      );
      this.conditionalButton = this.addRenderableWidget(
         CycleButton.booleanBuilder(Component.translatable("advMode.mode.conditional"), Component.translatable("advMode.mode.unconditional"), this.conditional)
            .displayOnlyValue()
            .create(this.width / 2 - 50, 165, 100, 20, Component.translatable("advMode.type"), (p_169727_, p_169728_) -> this.conditional = p_169728_)
      );
      this.autoexecButton = this.addRenderableWidget(
         CycleButton.booleanBuilder(
               Component.translatable("advMode.mode.autoexec.bat"), Component.translatable("advMode.mode.redstoneTriggered"), this.autoexec
            )
            .displayOnlyValue()
            .create(this.width / 2 + 50 + 4, 165, 100, 20, Component.translatable("advMode.triggering"), (p_169724_, p_169725_) -> this.autoexec = p_169725_)
      );
   }

   private void enableControls(boolean p_169730_) {
      this.doneButton.active = p_169730_;
      this.outputButton.active = p_169730_;
      this.modeButton.active = p_169730_;
      this.conditionalButton.active = p_169730_;
      this.autoexecButton.active = p_169730_;
   }

   public void updateGui() {
      BaseCommandBlock basecommandblock = this.autoCommandBlock.getCommandBlock();
      this.commandEdit.setValue(basecommandblock.getCommand());
      boolean flag = basecommandblock.isTrackOutput();
      this.mode = this.autoCommandBlock.getMode();
      this.conditional = this.autoCommandBlock.isConditional();
      this.autoexec = this.autoCommandBlock.isAutomatic();
      this.outputButton.setValue(flag);
      this.modeButton.setValue(this.mode);
      this.conditionalButton.setValue(this.conditional);
      this.autoexecButton.setValue(this.autoexec);
      this.updatePreviousOutput(flag);
      this.enableControls(true);
   }

   @Override
   public void resize(int p_98387_, int p_98388_) {
      super.resize(p_98387_, p_98388_);
      this.enableControls(true);
   }

   @Override
   protected void populateAndSendPacket() {
      this.minecraft
         .getConnection()
         .send(
            new ServerboundSetCommandBlockPacket(
               this.autoCommandBlock.getBlockPos(),
               this.commandEdit.getValue(),
               this.mode,
               this.autoCommandBlock.getCommandBlock().isTrackOutput(),
               this.conditional,
               this.autoexec
            )
         );
   }
}
