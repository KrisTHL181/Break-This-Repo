package net.minecraft.client.gui.screens;

import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.multiplayer.p2p.SignalingException;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class P2PConnectScreen extends Screen {
   private static final Component CONNECTING = Component.translatable("connect.p2p.establishing");
   private static final Component FAILURE_GENERIC = Component.translatable("connect.p2p.failed.generic");
   private static final Component FAILURE_TIMEOUT = Component.translatable("connect.p2p.failed.timeout");
   private static final Component FAILURE_UNREACHABLE = Component.translatable("connect.p2p.failed.unreachable");
   private static final Component FAILURE_SIGNALING = Component.translatable("connect.p2p.failed.signaling");
   private final Screen parent;
   private final String peerPmid;
   private Component status = CONNECTING;
   private boolean cancelled;

   private P2PConnectScreen(final Screen parent, final UUID peerPmid) {
      super(GameNarrator.NO_TITLE);
      this.parent = parent;
      this.peerPmid = peerPmid.toString();
   }

   public static void startConnecting(final Screen parent, final Minecraft minecraft, final UUID peerPmid) {
      P2PConnectScreen screen = new P2PConnectScreen(parent, peerPmid);
      minecraft.prepareForMultiplayer();
      minecraft.gui.setScreen(screen);
      screen.connect(minecraft);
   }

   private void connect(final Minecraft minecraft) {
      minecraft.p2pManager.joinPlayer(this.peerPmid).whenComplete((var2, error) -> minecraft.execute(() -> {
         if (!this.cancelled && minecraft.gui.screen() == this) {
            if (error != null) {
               minecraft.gui.setScreen(new DisconnectedScreen(this.parent, CommonComponents.CONNECT_FAILED, reasonFor(error)));
            } else {
               this.status = Component.translatable("connect.joining");
            }
         }
      }));
   }

   private static Component reasonFor(final Throwable error) {
      Throwable cause = error instanceof CompletionException e && e.getCause() != null ? e.getCause() : error;
      if (cause instanceof TimeoutException) {
         return FAILURE_TIMEOUT;
      } else if (cause instanceof SignalingException.UnknownPlayerException) {
         return FAILURE_UNREACHABLE;
      } else {
         return !(cause instanceof SignalingException.MessageUndeliveredException)
               && !(cause instanceof SignalingException.SignalingAuthException)
               && !(cause instanceof SignalingException.TurnAuthFailedException)
            ? FAILURE_GENERIC
            : FAILURE_SIGNALING;
      }
   }

   @Override
   protected void init() {
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, var1 -> {
         this.cancelled = true;
         this.minecraft.p2pManager.cancelOutgoingJoins();
         this.minecraft.gui.setScreen(this.parent);
      }).bounds(this.width / 2 - 100, this.height / 4 + 120 + 12, 200, 20).build());
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }

   @Override
   public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      super.extractRenderState(graphics, mouseX, mouseY, a);
      graphics.centeredText(this.font, this.status, this.width / 2, this.height / 2 - 50, -1);
   }
}
