package net.minecraft.client.gui.screens.inventory;

import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetTestBlockPacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TestBlockEntity;
import net.minecraft.world.level.block.state.properties.TestBlockMode;
import org.jspecify.annotations.Nullable;

public class TestBlockEditScreen extends Screen {
   private static final List<TestBlockMode> MODES = List.of(TestBlockMode.values());
   private static final Component TITLE = Component.translatable(Blocks.TEST_BLOCK.getDescriptionId());
   private static final Component MESSAGE_LABEL = Component.translatable("test_block.message");
   private final BlockPos position;
   private TestBlockMode mode;
   private String message;
   private @Nullable EditBox messageEdit;

   public TestBlockEditScreen(final TestBlockEntity block) {
      super(TITLE);
      this.position = block.getBlockPos();
      this.mode = block.getMode();
      this.message = block.getMessage();
   }

   @Override
   public void init() {
      this.messageEdit = new EditBox(this.font, this.width / 2 - 152, 80, 240, 20, Component.translatable("test_block.message"));
      this.messageEdit.setMaxLength(128);
      this.messageEdit.setValue(this.message);
      this.addRenderableWidget(this.messageEdit);
      this.updateMode(this.mode);
      this.addRenderableWidget(
         CycleButton.builder(TestBlockMode::getDisplayName, this.mode)
            .withValues(MODES)
            .displayOnlyValue()
            .create(this.width / 2 - 4 - 150, 185, 50, 20, TITLE, (button, value) -> this.updateMode(value))
      );
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onDone()).bounds(this.width / 2 - 4 - 150, 210, 150, 20).build());
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onCancel()).bounds(this.width / 2 + 4, 210, 150, 20).build());
   }

   @Override
   protected void setInitialFocus() {
      if (this.messageEdit != null) {
         this.setInitialFocus(this.messageEdit);
      } else {
         super.setInitialFocus();
      }
   }

   @Override
   public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      super.extractRenderState(graphics, mouseX, mouseY, a);
      graphics.centeredText(this.font, this.title, this.width / 2, 10, -1);
      if (this.mode != TestBlockMode.START) {
         graphics.text(this.font, MESSAGE_LABEL, this.width / 2 - 153, 70, -6250336);
      }

      graphics.text(this.font, this.mode.getDetailedMessage(), this.width / 2 - 153, 174, -6250336);
   }

   @Override
   public boolean isInGameUi() {
      return true;
   }

   private void onDone() {
      this.message = this.messageEdit.getValue();
      this.minecraft.getConnection().send(new ServerboundSetTestBlockPacket(this.position, this.mode, this.message));
      this.onClose();
   }

   @Override
   public void onClose() {
      this.onCancel();
   }

   private void onCancel() {
      this.minecraft.gui.setScreen(null);
   }

   private void updateMode(final TestBlockMode value) {
      this.mode = value;
      this.messageEdit.visible = value != TestBlockMode.START;
   }
}
