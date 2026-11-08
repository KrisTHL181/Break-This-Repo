package rip.ysm.api.attribute;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import org.jetbrains.annotations.Nullable;

public final class ForgeAttributes {

    private ForgeAttributes() {
    }

    @ExpectPlatform
    @Nullable
    public static Attribute blockReach() {
        throw new AssertionError();
    }

    @ExpectPlatform
    @Nullable
    public static Attribute entityReach() {
        throw new AssertionError();
    }

    @ExpectPlatform
    @Nullable
    public static Attribute swimSpeed() {
        throw new AssertionError();
    }

    @ExpectPlatform
    @Nullable
    public static Attribute entityGravity() {
        throw new AssertionError();
    }

    @ExpectPlatform
    @Nullable
    public static Attribute stepHeightAddition() {
        throw new AssertionError();
    }

    @ExpectPlatform
    @Nullable
    public static Attribute nametagDistance() {
        throw new AssertionError();
    }

    public static double getValue(LivingEntity entity, @Nullable Attribute attribute, double defaultValue) {
        if (attribute == null) {
            return defaultValue;
        }
        return entity.getAttributeValue(attribute);
    }
}
