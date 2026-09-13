package net.minecraft.client.multiplayer.prediction;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class BlockStatePredictionHandler implements AutoCloseable {
   private final Long2ObjectOpenHashMap<BlockStatePredictionHandler.ServerVerifiedState> serverVerifiedStates = new Long2ObjectOpenHashMap();
   private int currentSequenceNr;
   private boolean isPredicting;
   private int lastTeleportSequence = -1;

   public void retainKnownServerState(final BlockPos pos, final BlockState state, final LocalPlayer player) {
      this.serverVerifiedStates
         .compute(
            pos.asLong(),
            (key, serverVerifiedState) -> serverVerifiedState != null
               ? serverVerifiedState.setSequence(this.currentSequenceNr)
               : new BlockStatePredictionHandler.ServerVerifiedState(this.currentSequenceNr, state, player.position())
         );
   }

   public boolean updateKnownServerState(final BlockPos pos, final BlockState blockState) {
      BlockStatePredictionHandler.ServerVerifiedState serverVerifiedState = (BlockStatePredictionHandler.ServerVerifiedState)this.serverVerifiedStates
         .get(pos.asLong());
      if (serverVerifiedState == null) {
         return false;
      }

      serverVerifiedState.setBlockState(blockState);
      return true;
   }

   public void endPredictionsUpTo(final int sequence, final ClientLevel clientLevel) {
      ObjectIterator<Entry<BlockStatePredictionHandler.ServerVerifiedState>> stateIterator = this.serverVerifiedStates.long2ObjectEntrySet().iterator();

      while (stateIterator.hasNext()) {
         Entry<BlockStatePredictionHandler.ServerVerifiedState> next = (Entry<BlockStatePredictionHandler.ServerVerifiedState>)stateIterator.next();
         BlockStatePredictionHandler.ServerVerifiedState serverVerifiedState = (BlockStatePredictionHandler.ServerVerifiedState)next.getValue();
         if (serverVerifiedState.sequence <= sequence) {
            BlockPos pos = BlockPos.of(next.getLongKey());
            stateIterator.remove();
            clientLevel.syncBlockState(pos, serverVerifiedState.blockState, this.lastTeleportSequence < sequence ? serverVerifiedState.playerPos : null);
         }
      }
   }

   public BlockStatePredictionHandler startPredicting() {
      this.currentSequenceNr++;
      this.isPredicting = true;
      return this;
   }

   @Override
   public void close() {
      this.isPredicting = false;
   }

   public int currentSequence() {
      return this.currentSequenceNr;
   }

   public void onTeleport() {
      this.lastTeleportSequence = this.currentSequenceNr;
   }

   public boolean isPredicting() {
      return this.isPredicting;
   }

   private static class ServerVerifiedState {
      private final Vec3 playerPos;
      private int sequence;
      private BlockState blockState;

      private ServerVerifiedState(final int sequence, final BlockState blockState, final Vec3 playerPos) {
         this.sequence = sequence;
         this.blockState = blockState;
         this.playerPos = playerPos;
      }

      private BlockStatePredictionHandler.ServerVerifiedState setSequence(final int sequence) {
         this.sequence = sequence;
         return this;
      }

      private void setBlockState(final BlockState blockState) {
         this.blockState = blockState;
      }
   }
}
