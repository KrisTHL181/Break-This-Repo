package net.minecraft.client.gui.screens.multiplayer;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

public class CodeOfConductScreen extends WarningScreen {
   private static final Component TITLE = Component.translatable("multiplayer.codeOfConduct.title").withStyle(ChatFormatting.BOLD);
   private static final Component CHECK = Component.translatable("multiplayer.codeOfConduct.check");
   private final @Nullable ServerData serverData;
   private final String codeOfConductText;
   private final BooleanConsumer resultConsumer;
   private final Screen parent;

   private CodeOfConductScreen(
      final @Nullable ServerData serverData,
      final Screen parent,
      final Component contents,
      final String codeOfConductText,
      final BooleanConsumer resultConsumer
   ) {
      super(TITLE, contents, CHECK, TITLE.copy().append("\n").append(contents));
      this.serverData = serverData;
      this.parent = parent;
      this.codeOfConductText = codeOfConductText;
      this.resultConsumer = resultConsumer;
   }

   public CodeOfConductScreen(final @Nullable ServerData serverData, final Screen parent, final String codeOfConductText, final BooleanConsumer resultConsumer) {
      this(serverData, parent, Component.literal(codeOfConductText), codeOfConductText, resultConsumer);
   }

   @Override
   protected Layout addFooterButtons() {
      LinearLayout footer = LinearLayout.horizontal().spacing(8);
      footer.addChild(Button.builder(CommonComponents.GUI_ACKNOWLEDGE, button -> this.onResult(true)).build());
      footer.addChild(Button.builder(CommonComponents.GUI_DISCONNECT, button -> this.onResult(false)).build());
      return footer;
   }

   private void onResult(final boolean accepted) {
      this.resultConsumer.accept(accepted);
      if (this.serverData != null) {
         if (accepted && this.stopShowing.selected()) {
            this.serverData.acceptCodeOfConduct(this.codeOfConductText);
         } else {
            this.serverData.clearCodeOfConduct();
         }

         ServerList.saveSingleServer(this.serverData);
      }
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }

   @Override
   public void tick() {
      super.tick();
      if (this.parent instanceof ConnectScreen || this.parent instanceof ServerReconfigScreen) {
         this.parent.tick();
      }
   }
}
