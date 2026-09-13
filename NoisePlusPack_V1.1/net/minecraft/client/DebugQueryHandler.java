package net.minecraft.client;

import java.util.function.Consumer;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ServerboundBlockEntityTagQueryPacket;
import net.minecraft.network.protocol.game.ServerboundEntityTagQueryPacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class DebugQueryHandler {
   private final ClientPacketListener connection;
   private int transactionId = -1;
   private @Nullable Consumer<CompoundTag> callback;

   public DebugQueryHandler(ClientPacketListener p_90701_) {
      this.connection = p_90701_;
   }

   public boolean handleResponse(int p_90706_, @Nullable CompoundTag p_90707_) {
      if (this.transactionId == p_90706_ && this.callback != null) {
         this.callback.accept(p_90707_);
         this.callback = null;
         return true;
      } else {
         return false;
      }
   }

   private int startTransaction(Consumer<CompoundTag> p_90712_) {
      this.callback = p_90712_;
      return ++this.transactionId;
   }

   public void queryEntityTag(int p_90703_, Consumer<CompoundTag> p_90704_) {
      int i = this.startTransaction(p_90704_);
      this.connection.send(new ServerboundEntityTagQueryPacket(i, p_90703_));
   }

   public void queryBlockEntityTag(BlockPos p_90709_, Consumer<CompoundTag> p_90710_) {
      int i = this.startTransaction(p_90710_);
      this.connection.send(new ServerboundBlockEntityTagQueryPacket(i, p_90709_));
   }
}
