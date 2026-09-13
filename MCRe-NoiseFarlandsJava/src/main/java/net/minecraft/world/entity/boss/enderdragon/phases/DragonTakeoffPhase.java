package net.minecraft.world.entity.boss.enderdragon.phases;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.EndPodiumFeature;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class DragonTakeoffPhase extends AbstractDragonPhaseInstance {
    private boolean firstTick;
    private @Nullable Path currentPath;
    private @Nullable Vec3 targetLocation;

    public DragonTakeoffPhase(final EnderDragon dragon) {
        super(dragon);
    }

    @Override
    public void doServerTick(final ServerLevel level) {
        if (!this.firstTick && this.currentPath != null) {
            BlockPos egg = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EndPodiumFeature.getLocation(this.dragon.getFightOrigin()));
            if (!egg.closerToCenterThan(this.dragon.position(), 10.0)) {
                this.dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
            }
        } else {
            this.firstTick = false;
            this.findNewTarget();
        }
    }

    @Override
    public void begin() {
        this.firstTick = true;
        this.currentPath = null;
        this.targetLocation = null;
    }

    private void findNewTarget() {
        int currentNodeIndex = this.dragon.findClosestNode();
        Vec3 lookVector = this.dragon.getHeadLookVector(1.0F);
        int targetNodeIndex = this.dragon.findClosestNode(-lookVector.x * 40.0, 105.0, -lookVector.z * 40.0);
        if (this.dragon.getDragonFight() != null && this.dragon.getDragonFight().aliveCrystals() > 0) {
            targetNodeIndex %= 12;
            if (targetNodeIndex < 0) {
                targetNodeIndex += 12;
            }
        } else {
            targetNodeIndex -= 12;
            targetNodeIndex &= 7;
            targetNodeIndex += 12;
        }

        this.currentPath = this.dragon.findPath(currentNodeIndex, targetNodeIndex, null);
        this.navigateToNextPathNode();
    }

    private void navigateToNextPathNode() {
        if (this.currentPath != null) {
            this.currentPath.advance();
            if (!this.currentPath.isDone()) {
                Vec3i current = this.currentPath.getNextNodePos();
                this.currentPath.advance();

                double yTarget;
                do {
                    yTarget = current.getY() + this.dragon.getRandom().nextFloat() * 20.0F;
                } while (yTarget < current.getY());

                this.targetLocation = new Vec3(current.getX(), yTarget, current.getZ());
            }
        }
    }

    @Override
    public @Nullable Vec3 getFlyTargetLocation() {
        return this.targetLocation;
    }

    @Override
    public EnderDragonPhase<DragonTakeoffPhase> getPhase() {
        return EnderDragonPhase.TAKEOFF;
    }
}