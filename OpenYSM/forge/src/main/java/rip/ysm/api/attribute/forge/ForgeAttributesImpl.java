package rip.ysm.api.attribute.forge;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.common.ForgeMod;

public final class ForgeAttributesImpl {

    private ForgeAttributesImpl() {
    }

    public static Attribute blockReach() {
        return ForgeMod.BLOCK_REACH.get();
    }

    public static Attribute entityReach() {
        return ForgeMod.ENTITY_REACH.get();
    }

    public static Attribute swimSpeed() {
        return ForgeMod.SWIM_SPEED.get();
    }

    public static Attribute entityGravity() {
        return ForgeMod.ENTITY_GRAVITY.get();
    }

    public static Attribute stepHeightAddition() {
        return ForgeMod.STEP_HEIGHT_ADDITION.get();
    }

    public static Attribute nametagDistance() {
        return ForgeMod.NAMETAG_DISTANCE.get();
    }
}
