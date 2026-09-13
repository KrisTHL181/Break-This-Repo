package net.minecraft.client.gui.screens;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
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

   public ManageServerScreen(Screen p_422541_, Component p_423637_, BooleanConsumer p_431383_, ServerData p_426065_) {
      super(p_423637_);
      this.lastScreen = p_422541_;
      this.callback = p_431383_;
      this.serverData = p_426065_;
   }

   @Override
   protected void init() {
      this.nameEdit = new EditBox(this.font, this.width / 2 - 100, 66, 200, 20, NAME_LABEL);
      this.nameEdit.setValue(this.serverData.name);
      this.nameEdit.setHint(DEFAULT_SERVER_NAME);
      this.nameEdit.setResponder(p_424250_ -> this.updateAddButtonStatus());
      this.addWidget(this.nameEdit);
      this.ipEdit = new EditBox(this.font, this.width / 2 - 100, 106, 200, 20, IP_LABEL);
      this.ipEdit.setMaxLength(128);
      this.ipEdit.setValue(this.serverData.ip);
      this.ipEdit.setResponder(p_427210_ -> this.updateAddButtonStatus());
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
               (p_427981_, p_423372_) -> this.serverData.setResourcePackStatus(p_423372_)
            )
      );
      this.addButton = this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_DONE, p_429360_ -> this.onAdd()).bounds(this.width / 2 - 100, this.height / 4 + 96 + 18, 200, 20).build()
      );
      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_CANCEL, p_429638_ -> this.callback.accept(false))
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
   public void resize(int p_429730_, int p_430843_) {
      String s = this.ipEdit.getValue();
      String s1 = this.nameEdit.getValue();
      this.init(p_429730_, p_430843_);
      this.ipEdit.setValue(s);
      this.nameEdit.setValue(s1);
   }

   private void onAdd() {
      String s = this.nameEdit.getValue();
      this.serverData.name = s.isEmpty() ? DEFAULT_SERVER_NAME.getString() : s;
      this.serverData.ip = this.ipEdit.getValue();
      this.callback.accept(true);
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
   }

   private void updateAddButtonStatus() {
      this.addButton.active = ServerAddress.isValidAddress(this.ipEdit.getValue());
   }

   @Override
   public void render(GuiGraphics p_427803_, int p_424352_, int p_429377_, float p_428630_) {
      super.render(p_427803_, p_424352_, p_429377_, p_428630_);
      p_427803_.drawCenteredString(this.font, this.title, this.width / 2, 17, -1);
      p_427803_.drawString(this.font, NAME_LABEL, this.width / 2 - 100 + 1, 53, -6250336);
      p_427803_.drawString(this.font, IP_LABEL, this.width / 2 - 100 + 1, 94, -6250336);
      this.nameEdit.render(p_427803_, p_424352_, p_429377_, p_428630_);
      this.ipEdit.render(p_427803_, p_424352_, p_429377_, p_428630_);
   }
}
