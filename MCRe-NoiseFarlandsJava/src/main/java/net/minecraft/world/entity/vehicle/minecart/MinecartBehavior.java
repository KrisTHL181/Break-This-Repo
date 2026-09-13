package net.minecraft.world.entity.vehicle.minecart;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;

public abstract class MinecartBehavior {
    protected final AbstractMinecart minecart;

    protected MinecartBehavior(final AbstractMinecart minecart) {
        this.minecart = minecart;
    }

    public InterpolationHandler getInterpolation() {
        return null;
    }

    public void lerpMotion(final Vec3 movement) {
        this.setDeltaMovement(movement);
    }

    public abstract void tick();

    public Level level() {
        return this.minecart.level();
    }

    public abstract void moveAlongTrack(ServerLevel level);

    public abstract double stepAlongTrack(final BlockPos pos, final RailShape shape, final double movementLeft);

    public abstract boolean pushAndPickupEntities();

    public Vec3 getDeltaMovement() {
        return this.minecart.getDeltaMovement();
    }

    public void setDeltaMovement(final Vec3 deltaMovement) {
        this.minecart.setDeltaMovement(deltaMovement);
    }

    public void setDeltaMovement(final double x, final double y, final double z) {
        this.minecart.setDeltaMovement(x, y, z);
    }

    public Vec3 position() {
        return this.minecart.position();
    }

    public double getX() {
        return this.minecart.getX();
    }

    public double getY() {
        return this.minecart.getY();
    }

    public double getZ() {
        return this.minecart.getZ();
    }

    public void setPos(final Vec3 pos) {
        this.minecart.setPos(pos);
    }

    public void setPos(final double x, final double y, final double z) {
        this.minecart.setPos(x, y, z);
    }

    public float getXRot() {
        return this.minecart.getXRot();
    }

    public void setXRot(final float rot) {
        this.minecart.setXRot(rot);
    }

    public float getYRot() {
        return this.minecart.getYRot();
    }

    public void setYRot(final float rot) {
        this.minecart.setYRot(rot);
    }

    public Direction getMotionDirection() {
        return this.minecart.getDirection();
    }

    public Vec3 getKnownMovement(final Vec3 knownMovement) {
        return knownMovement;
    }

    public abstract double getMaxSpeed(ServerLevel level);

    public abstract double getSlowdownFactor();
}