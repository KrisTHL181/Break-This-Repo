package net.minecraft.client.gui.screens.inventory;

import java.util.stream.IntStream;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.IMEPreeditOverlay;
import net.minecraft.client.gui.components.TextCursorUtils;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.joml.Vector2f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public abstract class AbstractSignEditScreen extends Screen {
   protected final SignBlockEntity sign;
   private SignText text;
   private final String[] messages;
   private final boolean isFrontText;
   protected final WoodType woodType;
   private long cursorBlinkStartTime;
   private int line;
   private @Nullable TextFieldHelper signField;
   private @Nullable IMEPreeditOverlay preeditOverlay;
   private final Vector2f cursorPosScratch = new Vector2f();

   public AbstractSignEditScreen(final SignBlockEntity sign, final boolean isFrontText, final boolean shouldFilter) {
      this(sign, isFrontText, shouldFilter, Component.translatable("sign.edit"));
   }

   public AbstractSignEditScreen(final SignBlockEntity sign, final boolean isFrontText, final boolean shouldFilter, final Component title) {
      super(title);
      this.sign = sign;
      this.text = sign.getText(isFrontText);
      this.isFrontText = isFrontText;
      this.woodType = SignBlock.getWoodType(sign.getBlockState().getBlock());
      this.messages = IntStream.range(0, 4).mapToObj(index -> this.text.getMessage(index, shouldFilter)).map(Component::getString).toArray(String[]::new);
   }

   @Override
   protected void init() {
      this.minecraft.textInputManager().startTextInput();
      this.cursorBlinkStartTime = Util.getMillis();
      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_DONE, button -> this.onDone()).bounds(this.width / 2 - 100, this.height / 4 + 144, 200, 20).build()
      );
      this.signField = new TextFieldHelper(
         () -> this.messages[this.line],
         this::setMessage,
         TextFieldHelper.createClipboardGetter(this.minecraft),
         TextFieldHelper.createClipboardSetter(this.minecraft),
         s -> this.minecraft.font.width(s) <= this.sign.getMaxTextLineWidth()
      );
   }

   @Override
   public void tick() {
      if (!this.isValid()) {
         this.onDone();
      }
   }

   private boolean isValid() {
      return this.minecraft.player != null && !this.sign.isRemoved() && !this.sign.playerIsTooFarAwayToEdit(this.minecraft.player.getUUID());
   }

   @Override
   public boolean keyPressed(final KeyEvent event) {
      if (event.isUp()) {
         this.line = this.line - 1 & 3;
         this.signField.setCursorToEnd();
         return true;
      } else if (event.isDown() || event.isConfirmation()) {
         this.line = this.line + 1 & 3;
         this.signField.setCursorToEnd();
         return true;
      } else {
         return this.signField.keyPressed(event) ? true : super.keyPressed(event);
      }
   }

   @Override
   public boolean isInputCaptured() {
      return this.signField != null;
   }

   @Override
   public boolean charTyped(final CharacterEvent event) {
      this.signField.charTyped(event);
      return true;
   }

   @Override
   public boolean preeditUpdated(final @Nullable PreeditEvent event) {
      this.preeditOverlay = event != null ? new IMEPreeditOverlay(event, this.font, this.sign.getTextLineHeight()) : null;
      return true;
   }

   @Override
   public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      super.extractRenderState(graphics, mouseX, mouseY, a);
      graphics.centeredText(this.font, this.title, this.width / 2, 40, -1);
      this.extractSign(graphics);
   }

   @Override
   public void onClose() {
      this.onDone();
   }

   @Override
   public void removed() {
      ClientPacketListener connection = this.minecraft.getConnection();
      if (connection != null) {
         connection.send(
            new ServerboundSignUpdatePacket(this.sign.getBlockPos(), this.isFrontText, this.messages[0], this.messages[1], this.messages[2], this.messages[3])
         );
      }

      this.minecraft.textInputManager().stopTextInput();
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   @Override
   public boolean isInGameUi() {
      return true;
   }

   protected abstract void extractSignBackground(GuiGraphicsExtractor graphics);

   protected abstract Vector3fc getSignTextScale();

   protected abstract float getSignYOffset();

   private void extractSign(final GuiGraphicsExtractor graphics) {
      graphics.pose().pushMatrix();
      float offsetX = this.width / 2.0F;
      float offsetY = this.getSignYOffset();
      graphics.pose().translate(offsetX, offsetY);
      graphics.pose().pushMatrix();
      this.extractSignBackground(graphics);
      graphics.pose().popMatrix();
      Vector3fc textScale = this.getSignTextScale();
      graphics.pose().scale(textScale.x(), textScale.y());
      this.cursorPosScratch.zero();
      this.extractSignText(graphics, this.cursorPosScratch);
      graphics.pose().popMatrix();
      if (this.preeditOverlay != null) {
         this.cursorPosScratch.mul(textScale.x(), textScale.y()).add(offsetX, offsetY);
         this.preeditOverlay.updateInputPosition((int)this.cursorPosScratch.x, (int)this.cursorPosScratch.y);
         graphics.setPreeditOverlay(this.preeditOverlay);
      }
   }

   private void extractSignText(final GuiGraphicsExtractor graphics, final Vector2f cursorPosOutput) {
      int color = this.text.hasGlowingText() ? this.text.getColor().getTextColor() : AbstractSignRenderer.getDarkColor(this.text);
      boolean showCursor = TextCursorUtils.isCursorVisible(Util.getMillis() - this.cursorBlinkStartTime);
      boolean needsValidCursorPos = this.preeditOverlay != null;
      int cursorPos = this.signField.getCursorPos();
      int selectionPos = this.signField.getSelectionPos();
      int signMidpoint = 4 * this.sign.getTextLineHeight() / 2;
      int cursorY = this.line * this.sign.getTextLineHeight() - signMidpoint;

      for (int i = 0; i < this.messages.length; i++) {
         String line = this.messages[i];
         if (line != null) {
            if (this.font.isBidirectional()) {
               line = this.font.bidirectionalShaping(line);
            }

            int x1 = -this.font.width(line) / 2;
            graphics.text(this.font, line, x1, i * this.sign.getTextLineHeight() - signMidpoint, color, false);
            if (i == this.line && cursorPos >= 0 && (showCursor || needsValidCursorPos)) {
               int cursorPosition = this.font.width(line.substring(0, Math.max(Math.min(cursorPos, line.length()), 0)));
               int cursorX = cursorPosition - this.font.width(line) / 2;
               if (cursorPos >= line.length()) {
                  if (showCursor) {
                     TextCursorUtils.extractAppendCursor(graphics, this.font, cursorX, cursorY, color, false);
                  }

                  cursorPosOutput.set(cursorX, cursorY);
               }
            }
         }
      }

      for (int i = 0; i < this.messages.length; i++) {
         String line = this.messages[i];
         if (line != null && i == this.line && cursorPos >= 0) {
            int cursorPosition = this.font.width(line.substring(0, Math.max(Math.min(cursorPos, line.length()), 0)));
            int cursorX = cursorPosition - this.font.width(line) / 2;
            if (cursorPos < line.length()) {
               if (showCursor) {
                  TextCursorUtils.extractInsertCursor(graphics, cursorX, cursorY, ARGB.opaque(color), this.sign.getTextLineHeight());
               }

               cursorPosOutput.set(cursorX, cursorY);
            }

            if (selectionPos != cursorPos) {
               int startIndex = Math.min(cursorPos, selectionPos);
               int endIndex = Math.max(cursorPos, selectionPos);
               int startPosX = this.font.width(line.substring(0, startIndex)) - this.font.width(line) / 2;
               int endPosX = this.font.width(line.substring(0, endIndex)) - this.font.width(line) / 2;
               int fromX = Math.min(startPosX, endPosX);
               int toX = Math.max(startPosX, endPosX);
               graphics.textHighlight(fromX, cursorY, toX, cursorY + this.sign.getTextLineHeight(), true);
            }
         }
      }
   }

   private void setMessage(final String message) {
      this.messages[this.line] = message;
      this.text = this.text.setMessage(this.line, Component.literal(message));
      this.sign.setText(this.text, this.isFrontText);
   }

   private void onDone() {
      this.minecraft.gui.setScreen(null);
   }
}
