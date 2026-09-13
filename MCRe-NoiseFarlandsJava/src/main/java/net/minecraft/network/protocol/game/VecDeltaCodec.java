package net.minecraft.network.protocol.game;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.world.phys.Vec3;

public class VecDeltaCodec {
    private static final double TRUNCATION_STEPS = 4096.0;
    private Vec3 base = Vec3.ZERO;

    @VisibleForTesting
    static long encode(final double input) {
        return Math.round(input * 4096.0);
    }

    @VisibleForTesting
    static double decode(final long v) {
        return v / 4096.0;
    }

    public Vec3 decode(final long xa, final long ya, final long za) {
        if (xa == 0L && ya == 0L && za == 0L) {
            return this.base;
        }

        double x = xa == 0L ? this.base.x : decode(encode(this.base.x) + xa);
        double y = ya == 0L ? this.base.y : decode(encode(this.base.y) + ya);
        double z = za == 0L ? this.base.z : decode(encode(this.base.z) + za);
        return new Vec3(x, y, z);
    }

    public long encodeX(final Vec3 pos) {
        return encode(pos.x) - encode(this.base.x);
    }

    public long encodeY(final Vec3 pos) {
        return encode(pos.y) - encode(this.base.y);
    }

    public long encodeZ(final Vec3 pos) {
        return encode(pos.z) - encode(this.base.z);
    }

    public Vec3 delta(final Vec3 pos) {
        return pos.subtract(this.base);
    }

    public void setBase(final Vec3 base) {
        this.base = base;
    }

    public Vec3 getBase() {
        return this.base;
    }
}