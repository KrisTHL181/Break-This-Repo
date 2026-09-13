package net.minecraft.world.entity.monster.warden;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class WardenSpawnTracker {
    public static final Codec<WardenSpawnTracker> CODEC = RecordCodecBuilder.create(
        i -> i.group(
                ExtraCodecs.optionalAlwaysPresentFieldOf(ExtraCodecs.NON_NEGATIVE_INT, "ticks_since_last_warning", 0).forGetter(o -> o.ticksSinceLastWarning),
                ExtraCodecs.optionalAlwaysPresentFieldOf(ExtraCodecs.NON_NEGATIVE_INT, "warning_level", 0).forGetter(o -> o.warningLevel),
                ExtraCodecs.optionalAlwaysPresentFieldOf(ExtraCodecs.NON_NEGATIVE_INT, "cooldown_ticks", 0).forGetter(o -> o.cooldownTicks)
            )
            .apply(i, WardenSpawnTracker::new)
    );
    public static final int MAX_WARNING_LEVEL = 4;
    private static final double PLAYER_SEARCH_RADIUS = 16.0;
    private static final int WARNING_CHECK_DIAMETER = 48;
    private static final int DECREASE_WARNING_LEVEL_EVERY_INTERVAL = 12000;
    private static final int WARNING_LEVEL_INCREASE_COOLDOWN = 200;
    private int ticksSinceLastWarning;
    private int warningLevel;
    private int cooldownTicks;

    public WardenSpawnTracker(final int ticksSinceLastWarning, final int warningLevel, final int cooldownTicks) {
        this.ticksSinceLastWarning = ticksSinceLastWarning;
        this.warningLevel = warningLevel;
        this.cooldownTicks = cooldownTicks;
    }

    public WardenSpawnTracker() {
        this(0, 0, 0);
    }

    public void tick() {
        if (this.ticksSinceLastWarning >= 12000) {
            this.decreaseWarningLevel();
            this.ticksSinceLastWarning = 0;
        } else {
            this.ticksSinceLastWarning++;
        }

        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
        }
    }

    public void reset() {
        this.ticksSinceLastWarning = 0;
        this.warningLevel = 0;
        this.cooldownTicks = 0;
    }

    public static OptionalInt tryWarn(final ServerLevel level, final BlockPos pos, final ServerPlayer triggerPlayer) {
        if (hasNearbyWarden(level, pos)) {
            return OptionalInt.empty();
        }

        List<ServerPlayer> players = getNearbyPlayers(level, pos);
        if (!players.contains(triggerPlayer)) {
            players.add(triggerPlayer);
        }

        if (players.stream().anyMatch(player -> player.getWardenSpawnTracker().map(WardenSpawnTracker::onCooldown).orElse(false))) {
            return OptionalInt.empty();
        } else {
            Optional<WardenSpawnTracker> highestWarningSpawnTracker = players.stream()
                .flatMap(player -> player.getWardenSpawnTracker().stream())
                .max(Comparator.comparingInt(WardenSpawnTracker::getWarningLevel));
            if (highestWarningSpawnTracker.isPresent()) {
                WardenSpawnTracker spawnTracker = highestWarningSpawnTracker.get();
                spawnTracker.increaseWarningLevel();
                players.forEach(player -> player.getWardenSpawnTracker().ifPresent(otherSpawnTracker -> otherSpawnTracker.copyData(spawnTracker)));
                return OptionalInt.of(spawnTracker.warningLevel);
            } else {
                return OptionalInt.empty();
            }
        }
    }

    private boolean onCooldown() {
        return this.cooldownTicks > 0;
    }

    private static boolean hasNearbyWarden(final ServerLevel level, final BlockPos pos) {
        AABB areaToCheck = AABB.ofSize(Vec3.atCenterOf(pos), 48.0, 48.0, 48.0);
        return !level.getEntitiesOfClass(Warden.class, areaToCheck).isEmpty();
    }

    private static List<ServerPlayer> getNearbyPlayers(final ServerLevel level, final BlockPos pos) {
        Vec3 origin = Vec3.atCenterOf(pos);
        return level.getPlayers(player -> !player.isSpectator() && player.position().closerThan(origin, 16.0) && player.isAlive());
    }

    private void increaseWarningLevel() {
        if (!this.onCooldown()) {
            this.ticksSinceLastWarning = 0;
            this.cooldownTicks = 200;
            this.setWarningLevel(this.getWarningLevel() + 1);
        }
    }

    private void decreaseWarningLevel() {
        this.setWarningLevel(this.getWarningLevel() - 1);
    }

    public void setWarningLevel(final int warningLevel) {
        this.warningLevel = Mth.clamp(warningLevel, 0, 4);
    }

    public int getWarningLevel() {
        return this.warningLevel;
    }

    private void copyData(final WardenSpawnTracker copyFrom) {
        this.warningLevel = copyFrom.warningLevel;
        this.cooldownTicks = copyFrom.cooldownTicks;
        this.ticksSinceLastWarning = copyFrom.ticksSinceLastWarning;
    }
}