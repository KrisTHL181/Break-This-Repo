package net.minecraft.server.level;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

public class DemoMode extends ServerPlayerGameMode {
   public static final int DEMO_DAYS = 5;
   public static final int TOTAL_PLAY_TICKS = 120500;
   private boolean displayedIntro;
   private boolean demoHasEnded;
   private int demoEndedReminder;
   private int gameModeTicks;

   public DemoMode(ServerPlayer p_143204_) {
      super(p_143204_);
   }

   @Override
   public void tick() {
      super.tick();
      this.gameModeTicks++;
      long i = this.level.getGameTime();
      long j = i / 24000L + 1L;
      if (!this.displayedIntro && this.gameModeTicks > 20) {
         this.displayedIntro = true;
         this.player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.DEMO_EVENT, 0.0F));
      }

      this.demoHasEnded = i > 120500L;
      if (this.demoHasEnded) {
         this.demoEndedReminder++;
      }

      if (i % 24000L == 500L) {
         if (j <= 6L) {
            if (j == 6L) {
               this.player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.DEMO_EVENT, 104.0F));
            } else {
               this.player.sendSystemMessage(Component.translatable("demo.day." + j));
            }
         }
      } else if (j == 1L) {
         if (i == 100L) {
            this.player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.DEMO_EVENT, 101.0F));
         } else if (i == 175L) {
            this.player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.DEMO_EVENT, 102.0F));
         } else if (i == 250L) {
            this.player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.DEMO_EVENT, 103.0F));
         }
      } else if (j == 5L && i % 24000L == 22000L) {
         this.player.sendSystemMessage(Component.translatable("demo.day.warning"));
      }
   }

   private void outputDemoReminder() {
      if (this.demoEndedReminder > 100) {
         this.player.sendSystemMessage(Component.translatable("demo.reminder"));
         this.demoEndedReminder = 0;
      }
   }

   @Override
   public void handleBlockBreakAction(BlockPos p_214976_, ServerboundPlayerActionPacket.Action p_214977_, Direction p_214978_, int p_214979_, int p_214980_) {
      if (this.demoHasEnded) {
         this.outputDemoReminder();
      } else {
         super.handleBlockBreakAction(p_214976_, p_214977_, p_214978_, p_214979_, p_214980_);
      }
   }

   @Override
   public InteractionResult useItem(ServerPlayer p_140742_, Level p_140743_, ItemStack p_140744_, InteractionHand p_140745_) {
      if (this.demoHasEnded) {
         this.outputDemoReminder();
         return InteractionResult.PASS;
      } else {
         return super.useItem(p_140742_, p_140743_, p_140744_, p_140745_);
      }
   }

   @Override
   public InteractionResult useItemOn(ServerPlayer p_140747_, Level p_140748_, ItemStack p_140749_, InteractionHand p_140750_, BlockHitResult p_140751_) {
      if (this.demoHasEnded) {
         this.outputDemoReminder();
         return InteractionResult.PASS;
      } else {
         return super.useItemOn(p_140747_, p_140748_, p_140749_, p_140750_, p_140751_);
      }
   }
}
