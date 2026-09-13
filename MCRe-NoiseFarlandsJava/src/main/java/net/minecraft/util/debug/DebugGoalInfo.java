package net.minecraft.util.debug;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record DebugGoalInfo(List<DebugGoalInfo.DebugGoal> goals) {
    public static final StreamCodec<ByteBuf, DebugGoalInfo> STREAM_CODEC = StreamCodec.composite(
        DebugGoalInfo.DebugGoal.STREAM_CODEC.apply(ByteBufCodecs.list()), DebugGoalInfo::goals, DebugGoalInfo::new
    );

    public record DebugGoal(int priority, boolean isRunning, String name) {
        public static final StreamCodec<ByteBuf, DebugGoalInfo.DebugGoal> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            DebugGoalInfo.DebugGoal::priority,
            ByteBufCodecs.BOOL,
            DebugGoalInfo.DebugGoal::isRunning,
            ByteBufCodecs.stringUtf8(255),
            DebugGoalInfo.DebugGoal::name,
            DebugGoalInfo.DebugGoal::new
        );
    }
}