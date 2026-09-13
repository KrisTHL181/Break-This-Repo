package net.minecraft.client.gui.screens;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class ManageServerScreen extends Screen {
   private static final Component NAME_LABEL = Component.translatable("manageServer.enterName");
   private static final Component IP_LABEL = Component.translatable("manageServer.enterIp");
   private static final Component DEFAULT_SERVER_NAME = Component.translatable("selectServer.defaultName");
   private Button addButton;
   private final BooleanConsumer callback;
   private final ServerData serverData;
   private EditBox ipEdit;
   private EditBox nameEdit;
   private final Screen lastScreen;

   public ManageServerScreen(final Screen lastScreen, final Component title, final BooleanConsumer callback, final ServerData serverData) {
      super(title);
      this.lastScreen = lastScreen;
      this.callback = callback;
      this.serverData = serverData;
   }

   @Override
   protected void init() {
      this.nameEdit = new EditBox(this.font, this.width / 2 - 100, 66, 200, 20, NAME_LABEL);
      this.nameEdit.setValue(this.serverData.name);
      this.nameEdit.setHint(DEFAULT_SERVER_NAME);
      this.nameEdit.setResponder(v -> this.updateAddButtonStatus());
      this.addWidget(this.nameEdit);
      this.ipEdit = new EditBox(this.font, this.width / 2 - 100, 106, 200, 20, IP_LABEL);
      this.ipEdit.setMaxLength(128);
      this.ipEdit.setValue(this.serverData.ip);
      this.ipEdit.setResponder(v -> this.updateAddButtonStatus());
      this.addWidget(this.ipEdit);
      this.addRenderableWidget(
         CycleButton.builder(ServerData.ServerPackStatus::getName, this.serverData.getResourcePackStatus())
            .withValues(ServerData.ServerPackStatus.values())
            .create(
               this.width / 2 - 100,
               this.height / 4 + 72,
               200,
               20,
               Component.translatable("manageServer.resourcePack"),
               (button, value) -> this.serverData.setResourcePackStatus(value)
            )
      );
      this.addButton = this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_DONE, button -> this.onAdd()).bounds(this.width / 2 - 100, this.height / 4 + 96 + 18, 200, 20).build()
      );
      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_CANCEL, button -> this.callback.accept(false))
            .bounds(this.width / 2 - 100, this.height / 4 + 120 + 18, 200, 20)
            .build()
      );
      this.updateAddButtonStatus();
   }

   @Override
   protected void setInitialFocus() {
      this.setInitialFocus(this.nameEdit);
   }

   @Override
   public void resize(final int width, final int height) {
      String oldIpEdit = this.ipEdit.getValue();
      String oldNameEdit = this.nameEdit.getValue();
      this.init(width, height);
      this.ipEdit.setValue(oldIpEdit);
      this.nameEdit.setValue(oldNameEdit);
   }

   private void onAdd() {
      String name = this.nameEdit.getValue();
      this.serverData.name = name.isEmpty() ? DEFAULT_SERVER_NAME.getString() : name;
      this.serverData.ip = this.ipEdit.getValue();
      this.callback.accept(true);
   }

   @Override
   public void onClose() {
      this.minecraft.gui.setScreen(this.lastScreen);
   }

   private void updateAddButtonStatus() {
      this.addButton.active = ServerAddress.isValidAddress(this.ipEdit.getValue());
   }

   @Override
   public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      super.extractRenderState(graphics, mouseX, mouseY, a);
      graphics.centeredText(this.font, this.title, this.width / 2, 17, -1);
      graphics.text(this.font, NAME_LABEL, this.width / 2 - 100 + 1, 53, -6250336);
      graphics.text(this.font, IP_LABEL, this.width / 2 - 100 + 1, 94, -6250336);
      this.nameEdit.extractRenderState(graphics, mouseX, mouseY, a);
      this.ipEdit.extractRenderState(graphics, mouseX, mouseY, a);
   }
}
