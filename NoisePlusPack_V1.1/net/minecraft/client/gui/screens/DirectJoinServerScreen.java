package net.minecraft.client.gui.screens;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DirectJoinServerScreen extends Screen {
   private static final Component ENTER_IP_LABEL = Component.translatable("manageServer.enterIp");
   private Button selectButton;
   private final ServerData serverData;
   private EditBox ipEdit;
   private final BooleanConsumer callback;
   private final Screen lastScreen;

   public DirectJoinServerScreen(Screen p_95960_, BooleanConsumer p_95961_, ServerData p_95962_) {
      super(Component.translatable("selectServer.direct"));
      this.lastScreen = p_95960_;
      this.serverData = p_95962_;
      this.callback = p_95961_;
   }

   @Override
   public boolean keyPressed(KeyEvent p_422310_) {
      if (this.selectButton.active && this.getFocused() == this.ipEdit && p_422310_.isConfirmation()) {
         this.onSelect();
         return true;
      } else {
         return super.keyPressed(p_422310_);
      }
   }

   @Override
   protected void init() {
      this.ipEdit = new EditBox(this.font, this.width / 2 - 100, 116, 200, 20, ENTER_IP_LABEL);
      this.ipEdit.setMaxLength(128);
      this.ipEdit.setValue(this.minecraft.options.lastMpIp);
      this.ipEdit.setResponder(p_95983_ -> this.updateSelectButtonStatus());
      this.addWidget(this.ipEdit);
      this.selectButton = this.addRenderableWidget(
         Button.builder(Component.translatable("selectServer.select"), p_95981_ -> this.onSelect())
            .bounds(this.width / 2 - 100, this.height / 4 + 96 + 12, 200, 20)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_CANCEL, p_95977_ -> this.callback.accept(false))
            .bounds(this.width / 2 - 100, this.height / 4 + 120 + 12, 200, 20)
            .build()
      );
      this.updateSelectButtonStatus();
   }

   @Override
   protected void setInitialFocus() {
      this.setInitialFocus(this.ipEdit);
   }

   @Override
   public void resize(int p_95974_, int p_95975_) {
      String s = this.ipEdit.getValue();
      this.init(p_95974_, p_95975_);
      this.ipEdit.setValue(s);
   }

   private void onSelect() {
      this.serverData.ip = this.ipEdit.getValue();
      this.callback.accept(true);
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
   }

   @Override
   public void removed() {
      this.minecraft.options.lastMpIp = this.ipEdit.getValue();
      this.minecraft.options.save();
   }

   private void updateSelectButtonStatus() {
      this.selectButton.active = ServerAddress.isValidAddress(this.ipEdit.getValue());
   }

   @Override
   public void render(GuiGraphics p_282464_, int p_95969_, int p_95970_, float p_95971_) {
      super.render(p_282464_, p_95969_, p_95970_, p_95971_);
      p_282464_.drawCenteredString(this.font, this.title, this.width / 2, 20, -1);
      p_282464_.drawString(this.font, ENTER_IP_LABEL, this.width / 2 - 100 + 1, 100, -6250336);
      this.ipEdit.render(p_282464_, p_95969_, p_95970_, p_95971_);
   }
}
