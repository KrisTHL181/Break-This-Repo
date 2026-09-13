package net.minecraft.client.renderer.chunk;

import java.util.Objects;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class TranslucencyPointOfView {
    private int x;
    private int y;
    private int z;

    public static TranslucencyPointOfView of(final Vec3 cameraPos, final SectionPos sectionNode) {
        return new TranslucencyPointOfView().set(cameraPos, sectionNode);
    }

    public TranslucencyPointOfView set(final Vec3 cameraPos, final SectionPos sectionPos) {
        this.x = getCoordinate(cameraPos.x(), sectionPos.x());
        this.y = getCoordinate(cameraPos.y(), sectionPos.y());
        this.z = getCoordinate(cameraPos.z(), sectionPos.z());
        return this;
    }

    private static int getCoordinate(final double cameraCoordinate, final int section) {
        int relativeSection = (int)(Mth.lfloor(cameraCoordinate) >> 4) - section;
        return Mth.clamp(relativeSection, -1, 1);
    }

    public boolean isAxisAligned() {
        return this.x == 0 || this.y == 0 || this.z == 0;
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        } else {
            return !(other instanceof TranslucencyPointOfView otherPerspective)
                ? false
                : this.x == otherPerspective.x && this.y == otherPerspective.y && this.z == otherPerspective.z;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.x, this.y, this.z);
    }
}