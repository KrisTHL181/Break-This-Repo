package net.minecraft.world.level.levelgen.carver;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class CarverDebugSettings {
    public static final CarverDebugSettings DEFAULT = new CarverDebugSettings(
        false,
        Blocks.ACACIA_BUTTON.defaultBlockState(),
        Blocks.CANDLE.defaultBlockState(),
        Blocks.STAINED_GLASS.orange().defaultBlockState(),
        Blocks.GLASS.defaultBlockState()
    );
    public static final Codec<CarverDebugSettings> CODEC = RecordCodecBuilder.create(
        i -> i.group(
                Codec.BOOL.optionalFieldOf("debug_mode", false).forGetter(CarverDebugSettings::isDebugMode),
                BlockState.CODEC.optionalFieldOf("air_state", DEFAULT.getAirState()).forGetter(CarverDebugSettings::getAirState),
                BlockState.CODEC.optionalFieldOf("water_state", DEFAULT.getAirState()).forGetter(CarverDebugSettings::getWaterState),
                BlockState.CODEC.optionalFieldOf("lava_state", DEFAULT.getAirState()).forGetter(CarverDebugSettings::getLavaState),
                BlockState.CODEC.optionalFieldOf("barrier_state", DEFAULT.getAirState()).forGetter(CarverDebugSettings::getBarrierState)
            )
            .apply(i, CarverDebugSettings::new)
    );
    private final boolean debugMode;
    private final BlockState airState;
    private final BlockState waterState;
    private final BlockState lavaState;
    private final BlockState barrierState;

    public static CarverDebugSettings of(
        final boolean enabled, final BlockState airState, final BlockState waterState, final BlockState lavaState, final BlockState barrierState
    ) {
        return new CarverDebugSettings(enabled, airState, waterState, lavaState, barrierState);
    }

    public static CarverDebugSettings of(final BlockState airState, final BlockState waterState, final BlockState lavaState, final BlockState barrierState) {
        return new CarverDebugSettings(false, airState, waterState, lavaState, barrierState);
    }

    public static CarverDebugSettings of(final boolean debugMode, final BlockState airState) {
        return new CarverDebugSettings(debugMode, airState, DEFAULT.getWaterState(), DEFAULT.getLavaState(), DEFAULT.getBarrierState());
    }

    private CarverDebugSettings(
        final boolean debugMode, final BlockState airState, final BlockState waterState, final BlockState lavaState, final BlockState barrierState
    ) {
        this.debugMode = debugMode;
        this.airState = airState;
        this.waterState = waterState;
        this.lavaState = lavaState;
        this.barrierState = barrierState;
    }

    public boolean isDebugMode() {
        return this.debugMode;
    }

    public BlockState getAirState() {
        return this.airState;
    }

    public BlockState getWaterState() {
        return this.waterState;
    }

    public BlockState getLavaState() {
        return this.lavaState;
    }

    public BlockState getBarrierState() {
        return this.barrierState;
    }
}