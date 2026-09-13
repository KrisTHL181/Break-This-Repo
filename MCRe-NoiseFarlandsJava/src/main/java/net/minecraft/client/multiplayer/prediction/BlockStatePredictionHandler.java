package net.minecraft.client.multiplayer.prediction;




import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlockStatePredictionHandler implements AutoCloseable {
    private final Map<BlockPos, BlockStatePredictionHandler.ServerVerifiedState> serverVerifiedStates = new HashMap<>();
    private int currentSequenceNr;
    private boolean isPredicting;
    private int lastTeleportSequence = -1;

    public void retainKnownServerState(final BlockPos pos, final BlockState state, final LocalPlayer player) {
        this.serverVerifiedStates
            .compute(
                pos,
                (key, serverVerifiedState) -> serverVerifiedState != null
                    ? serverVerifiedState.setSequence(this.currentSequenceNr)
                    : new BlockStatePredictionHandler.ServerVerifiedState(this.currentSequenceNr, state, player.position())
            );
    }

    public boolean updateKnownServerState(final BlockPos pos, final BlockState blockState) {
        BlockStatePredictionHandler.ServerVerifiedState serverVerifiedState = this.serverVerifiedStates.get(pos);
        if (serverVerifiedState == null) {
            return false;
        }

        serverVerifiedState.setBlockState(blockState);
        return true;
    }

    public void endPredictionsUpTo(final int sequence, final ClientLevel clientLevel) {
        var stateIterator = this.serverVerifiedStates.entrySet().iterator();

        while (stateIterator.hasNext()) {
            var next = stateIterator.next();
            BlockStatePredictionHandler.ServerVerifiedState serverVerifiedState = next.getValue();
            if (serverVerifiedState.sequence <= sequence) {
                BlockPos pos = next.getKey();
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

    @OnlyIn(Dist.CLIENT)
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