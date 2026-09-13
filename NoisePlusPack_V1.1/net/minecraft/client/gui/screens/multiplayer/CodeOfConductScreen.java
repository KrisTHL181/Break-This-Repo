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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class CodeOfConductScreen extends WarningScreen {
   private static final Component TITLE = Component.translatable("multiplayer.codeOfConduct.title").withStyle(ChatFormatting.BOLD);
   private static final Component CHECK = Component.translatable("multiplayer.codeOfConduct.check");
   private final @Nullable ServerData serverData;
   private final String codeOfConductText;
   private final BooleanConsumer resultConsumer;
   private final Screen parent;

   private CodeOfConductScreen(@Nullable ServerData p_428059_, Screen p_423797_, Component p_430883_, String p_431387_, BooleanConsumer p_426320_) {
      super(TITLE, p_430883_, CHECK, TITLE.copy().append("\n").append(p_430883_));
      this.serverData = p_428059_;
      this.parent = p_423797_;
      this.codeOfConductText = p_431387_;
      this.resultConsumer = p_426320_;
   }

   public CodeOfConductScreen(@Nullable ServerData p_429980_, Screen p_424964_, String p_427379_, BooleanConsumer p_428635_) {
      this(p_429980_, p_424964_, Component.literal(p_427379_), p_427379_, p_428635_);
   }

   @Override
   protected Layout addFooterButtons() {
      LinearLayout linearlayout = LinearLayout.horizontal().spacing(8);
      linearlayout.addChild(Button.builder(CommonComponents.GUI_ACKNOWLEDGE, p_427194_ -> this.onResult(true)).build());
      linearlayout.addChild(Button.builder(CommonComponents.GUI_DISCONNECT, p_427513_ -> this.onResult(false)).build());
      return linearlayout;
   }

   private void onResult(boolean p_424550_) {
      this.resultConsumer.accept(p_424550_);
      if (this.serverData != null) {
         if (p_424550_ && this.stopShowing.selected()) {
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
