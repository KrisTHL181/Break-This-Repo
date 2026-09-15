package net.minecraft.world.level.block.state.properties;

import net.minecraft.util.StringRepresentable;

public enum DoorHingeSide implements StringRepresentable {
    LEFT,
    RIGHT;

    @Override
    public String toString() {
        return this.getSerializedName();
    }

    @Override
    public String getSerializedName() {
        return this == LEFT ? "left" : "right";
    }
}