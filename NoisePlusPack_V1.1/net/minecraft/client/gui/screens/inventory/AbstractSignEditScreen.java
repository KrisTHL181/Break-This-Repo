package net.minecraft.client.gui.screens.inventory;

import java.util.stream.IntStream;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractSignEditScreen extends Screen {
   protected final SignBlockEntity sign;
   private SignText text;
   private final String[] messages;
   private final boolean isFrontText;
   protected final WoodType woodType;
   private int frame;
   private int line;
   private @Nullable TextFieldHelper signField;

   public AbstractSignEditScreen(SignBlockEntity p_277842_, boolean p_277719_, boolean p_277969_) {
      this(p_277842_, p_277719_, p_277969_, Component.translatable("sign.edit"));
   }

   public AbstractSignEditScreen(SignBlockEntity p_277792_, boolean p_277607_, boolean p_278039_, Component p_277393_) {
      super(p_277393_);
      this.sign = p_277792_;
      this.text = p_277792_.getText(p_277607_);
      this.isFrontText = p_277607_;
      this.woodType = SignBlock.getWoodType(p_277792_.getBlockState().getBlock());
      this.messages = IntStream.range(0, 4).mapToObj(p_277214_ -> this.text.getMessage(p_277214_, p_278039_)).map(Component::getString).toArray(String[]::new);
   }

   @Override
   protected void init() {
      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_DONE, p_251194_ -> this.onDone()).bounds(this.width / 2 - 100, this.height / 4 + 144, 200, 20).build()
      );
      this.signField = new TextFieldHelper(
         () -> this.messages[this.line],
         this::setMessage,
         TextFieldHelper.createClipboardGetter(this.minecraft),
         TextFieldHelper.createClipboardSetter(this.minecraft),
         p_280850_ -> this.minecraft.font.width(p_280850_) <= this.sign.getMaxTextLineWidth()
      );
   }

   @Override
   public void tick() {
      this.frame++;
      if (!this.isValid()) {
         this.onDone();
      }
   }

   private boolean isValid() {
      return this.minecraft.player != null && !this.sign.isRemoved() && !this.sign.playerIsTooFarAwayToEdit(this.minecraft.player.getUUID());
   }

   @Override
   public boolean keyPressed(KeyEvent p_425946_) {
      if (p_425946_.isUp()) {
         this.line = this.line - 1 & 3;
         this.signField.setCursorToEnd();
         return true;
      }

      if (!p_425946_.isDown() && !p_425946_.isConfirmation()) {
         return this.signField.keyPressed(p_425946_) ? true : super.keyPressed(p_425946_);
      }

      this.line = this.line + 1 & 3;
      this.signField.setCursorToEnd();
      return true;
   }

   @Override
   public boolean charTyped(CharacterEvent p_429405_) {
      this.signField.charTyped(p_429405_);
      return true;
   }

   @Override
   public void render(GuiGraphics p_282418_, int p_281700_, int p_283040_, float p_282799_) {
      super.render(p_282418_, p_281700_, p_283040_, p_282799_);
      p_282418_.drawCenteredString(this.font, this.title, this.width / 2, 40, -1);
      this.renderSign(p_282418_);
   }

   @Override
   public void onClose() {
      this.onDone();
   }

   @Override
   public void removed() {
      ClientPacketListener clientpacketlistener = this.minecraft.getConnection();
      if (clientpacketlistener != null) {
         clientpacketlistener.send(
            new ServerboundSignUpdatePacket(this.sign.getBlockPos(), this.isFrontText, this.messages[0], this.messages[1], this.messages[2], this.messages[3])
         );
      }
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   @Override
   public boolean isInGameUi() {
      return true;
   }

   protected abstract void renderSignBackground(GuiGraphics var1);

   protected abstract Vector3f getSignTextScale();

   protected abstract float getSignYOffset();

   private void renderSign(GuiGraphics p_282006_) {
      p_282006_.pose().pushMatrix();
      p_282006_.pose().translate(this.width / 2.0F, this.getSignYOffset());
      p_282006_.pose().pushMatrix();
      this.renderSignBackground(p_282006_);
      p_282006_.pose().popMatrix();
      this.renderSignText(p_282006_);
      p_282006_.pose().popMatrix();
   }

   private void renderSignText(GuiGraphics p_282366_) {
      Vector3f vector3f = this.getSignTextScale();
      p_282366_.pose().scale(vector3f.x(), vector3f.y());
      int i = this.text.hasGlowingText() ? this.text.getColor().getTextColor() : AbstractSignRenderer.getDarkColor(this.text);
      boolean flag = this.frame / 6 % 2 == 0;
      int j = this.signField.getCursorPos();
      int k = this.signField.getSelectionPos();
      int l = 4 * this.sign.getTextLineHeight() / 2;
      int i1 = this.line * this.sign.getTextLineHeight() - l;

      for (int j1 = 0; j1 < this.messages.length; j1++) {
         String s = this.messages[j1];
         if (s != null) {
            if (this.font.isBidirectional()) {
               s = this.font.bidirectionalShaping(s);
            }

            int k1 = -this.font.width(s) / 2;
            p_282366_.drawString(this.font, s, k1, j1 * this.sign.getTextLineHeight() - l, i, false);
            if (j1 == this.line && j >= 0 && flag) {
               int l1 = this.font.width(s.substring(0, Math.max(Math.min(j, s.length()), 0)));
               int i2 = l1 - this.font.width(s) / 2;
               if (j >= s.length()) {
                  p_282366_.drawString(this.font, "_", i2, i1, i, false);
               }
            }
         }
      }

      for (int k3 = 0; k3 < this.messages.length; k3++) {
         String s1 = this.messages[k3];
         if (s1 != null && k3 == this.line && j >= 0) {
            int l3 = this.font.width(s1.substring(0, Math.max(Math.min(j, s1.length()), 0)));
            int i4 = l3 - this.font.width(s1) / 2;
            if (flag && j < s1.length()) {
               p_282366_.fill(i4, i1 - 1, i4 + 1, i1 + this.sign.getTextLineHeight(), ARGB.opaque(i));
            }

            if (k != j) {
               int j4 = Math.min(j, k);
               int j2 = Math.max(j, k);
               int k2 = this.font.width(s1.substring(0, j4)) - this.font.width(s1) / 2;
               int l2 = this.font.width(s1.substring(0, j2)) - this.font.width(s1) / 2;
               int i3 = Math.min(k2, l2);
               int j3 = Math.max(k2, l2);
               p_282366_.textHighlight(i3, i1, j3, i1 + this.sign.getTextLineHeight(), true);
            }
         }
      }
   }

   private void setMessage(String p_277913_) {
      this.messages[this.line] = p_277913_;
      this.text = this.text.setMessage(this.line, Component.literal(p_277913_));
      this.sign.setText(this.text, this.isFrontText);
   }

   private void onDone() {
      this.minecraft.setScreen(null);
   }
}
