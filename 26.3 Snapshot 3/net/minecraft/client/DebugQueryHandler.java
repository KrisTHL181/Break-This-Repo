package net.minecraft.client;

import java.util.function.Consumer;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ServerboundBlockEntityTagQueryPacket;
import net.minecraft.network.protocol.game.ServerboundEntityTagQueryPacket;
import org.jspecify.annotations.Nullable;

public class DebugQueryHandler {
   private final ClientPacketListener connection;
   private int transactionId = -1;
   private @Nullable Consumer<CompoundTag> callback;

   public DebugQueryHandler(final ClientPacketListener connection) {
      this.connection = connection;
   }

   public boolean handleResponse(final int transactionId, final @Nullable CompoundTag tag) {
      if (this.transactionId == transactionId && this.callback != null) {
         this.callback.accept(tag);
         this.callback = null;
         return true;
      } else {
         return false;
      }
   }

   private int startTransaction(final Consumer<CompoundTag> callback) {
      this.callback = callback;
      return ++this.transactionId;
   }

   public void queryEntityTag(final int entityId, final Consumer<CompoundTag> callback) {
      int transactionId = this.startTransaction(callback);
      this.connection.send(new ServerboundEntityTagQueryPacket(transactionId, entityId));
   }

   public void queryBlockEntityTag(final BlockPos blockPos, final Consumer<CompoundTag> callback) {
      int transactionId = this.startTransaction(callback);
      this.connection.send(new ServerboundBlockEntityTagQueryPacket(transactionId, blockPos));
   }
}
