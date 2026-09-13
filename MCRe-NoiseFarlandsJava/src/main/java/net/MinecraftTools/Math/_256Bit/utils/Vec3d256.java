package net.MinecraftTools.Math._256Bit.utils;

import net.minecraft.core.Direction;
import net.MinecraftTools.Math._256Bit.Float256;

/**
 * Vec3d256 — 256-bit 高精度三维向量（Float256 分量）
 *
 * <p>用于边境之地探索 / Perlin 噪声 / 高精度坐标运算。所有运算走 Float256
 * （176-bit 尾数 + GRS 舍入），距离平方在超大坐标下不丢失精度。
 *
 * <p>与 {@link Vec3} 互转：Vec3#to256() / Vec3#from256()
 *
 * <p>INF32768 / MCRe NoiseFarlands 项目
 */
public final class Vec3d256 {
    public static final Vec3d256 ZERO = new Vec3d256(Float256.ZERO, Float256.ZERO, Float256.ZERO);
    public static final Vec3d256 X_AXIS = new Vec3d256(Float256.ONE, Float256.ZERO, Float256.ZERO);
    public static final Vec3d256 Y_AXIS = new Vec3d256(Float256.ZERO, Float256.ONE, Float256.ZERO);
    public static final Vec3d256 Z_AXIS = new Vec3d256(Float256.ZERO, Float256.ZERO, Float256.ONE);

    public final Float256 x;
    public final Float256 y;
    public final Float256 z;

    private Vec3d256(final Float256 x, final Float256 y, final Float256 z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    // ═══════════ 工厂 ═══════════

    public static Vec3d256 of(final Float256 x, final Float256 y, final Float256 z) {
        return new Vec3d256(x, y, z);
    }

    public static Vec3d256 of(final double x, final double y, final double z) {
        return new Vec3d256(Float256.of(x), Float256.of(y), Float256.of(z));
    }

    /** 从 double 向量（53-bit 尾数无损） */
    public static Vec3d256 fromVec3(final Vec3 vec) {
        return new Vec3d256(Float256.of(vec.x), Float256.of(vec.y), Float256.of(vec.z));
    }

    /** 从 Int256 坐标（精确整数） */
    public static Vec3d256 ofInt(final net.MinecraftTools.Math._256Bit.Int256 x, final net.MinecraftTools.Math._256Bit.Int256 y, final net.MinecraftTools.Math._256Bit.Int256 z) {
        return new Vec3d256(Float256.of(x), Float256.of(y), Float256.of(z));
    }

    // ═══════════ 分量访问 ═══════════

    public Float256 get(final Direction.Axis axis) {
        return axis == Direction.Axis.X ? this.x : (axis == Direction.Axis.Y ? this.y : this.z);
    }

    public Vec3d256 with(final Direction.Axis axis, final Float256 value) {
        Float256 nx = axis == Direction.Axis.X ? value : this.x;
        Float256 ny = axis == Direction.Axis.Y ? value : this.y;
        Float256 nz = axis == Direction.Axis.Z ? value : this.z;
        return new Vec3d256(nx, ny, nz);
    }

    public boolean isFinite() {
        return this.x.isFinite() && this.y.isFinite() && this.z.isFinite();
    }

    public boolean isZero() {
        return this.x.isZero() && this.y.isZero() && this.z.isZero();
    }

    // ═══════════ 向量运算 ═══════════

    public Vec3d256 add(final Vec3d256 o) {
        return new Vec3d256(this.x.add(o.x), this.y.add(o.y), this.z.add(o.z));
    }

    public Vec3d256 subtract(final Vec3d256 o) {
        return new Vec3d256(this.x.subtract(o.x), this.y.subtract(o.y), this.z.subtract(o.z));
    }

    public Vec3d256 scale(final Float256 factor) {
        return new Vec3d256(this.x.multiply(factor), this.y.multiply(factor), this.z.multiply(factor));
    }

    public Vec3d256 scale(final double factor) {
        return this.scale(Float256.of(factor));
    }

    public Vec3d256 multiply(final Vec3d256 o) {
        return new Vec3d256(this.x.multiply(o.x), this.y.multiply(o.y), this.z.multiply(o.z));
    }

    public Vec3d256 reverse() {
        return this.scale(Float256.MINUS_ONE);
    }

    /** 高精度点积 */
    public Float256 dot(final Vec3d256 o) {
        return this.x.multiply(o.x).add(this.y.multiply(o.y)).add(this.z.multiply(o.z));
    }

    /** 高精度叉积 */
    public Vec3d256 cross(final Vec3d256 o) {
        return new Vec3d256(
            this.y.multiply(o.z).subtract(this.z.multiply(o.y)),
            this.z.multiply(o.x).subtract(this.x.multiply(o.z)),
            this.x.multiply(o.y).subtract(this.y.multiply(o.x))
        );
    }

    // ═══════════ 距离 / 长度 ═══════════

    public Float256 lengthSqr() {
        return Mth.lengthSquared(this.x, this.y, this.z);
    }

    public Float256 length() {
        return Mth.sqrt(this.lengthSqr());
    }

    public Float256 horizontalDistanceSqr() {
        return Mth.lengthSquared(this.x, this.z);
    }

    public Float256 horizontalDistance() {
        return Mth.sqrt(this.horizontalDistanceSqr());
    }

    public Float256 distanceToSqr(final Vec3d256 o) {
        return Mth.lengthSquared(o.x.subtract(this.x), o.y.subtract(this.y), o.z.subtract(this.z));
    }

    public Float256 distanceTo(final Vec3d256 o) {
        return Mth.sqrt(this.distanceToSqr(o));
    }

    /** 单位向量（176-bit 精度） */
    public Vec3d256 normalize() {
        Float256 len = this.length();
        if (len.isZero()) return ZERO;
        Float256 inv = Float256.ONE.divide(len);
        return new Vec3d256(this.x.multiply(inv), this.y.multiply(inv), this.z.multiply(inv));
    }

    /** 线性插值：this + alpha × (to - this) */
    public Vec3d256 lerp(final Vec3d256 to, final Float256 alpha) {
        return new Vec3d256(
            Mth.lerp(alpha, this.x, to.x),
            Mth.lerp(alpha, this.y, to.y),
            Mth.lerp(alpha, this.z, to.z)
        );
    }

    public Vec3d256 abs() {
        return new Vec3d256(this.x.abs(), this.y.abs(), this.z.abs());
    }

    public Float256 minComponent() {
        return Mth.min(this.x, Mth.min(this.y, this.z));
    }

    public Float256 maxComponent() {
        return Mth.max(this.x, Mth.max(this.y, this.z));
    }

    // ═══════════ 转换 ═══════════

    /** 转 double 向量（Float256 → double，近似） */
    public Vec3 toVec3() {
        return new Vec3(this.x.doubleValue(), this.y.doubleValue(), this.z.doubleValue());
    }

    @Override
    public String toString() {
        return "(" + this.x + ", " + this.y + ", " + this.z + ")";
    }

    /** 精确十进制展开（完整 176-bit 尾数，超长） */
    public String toExactString() {
        return "(" + this.x.toExactString() + ", " + this.y.toExactString() + ", " + this.z.toExactString() + ")";
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof Vec3d256 other)) return false;
        return this.x.equals(other.x) && this.y.equals(other.y) && this.z.equals(other.z);
    }

    @Override
    public int hashCode() {
        int result = this.x.hashCode();
        result = 31 * result + this.y.hashCode();
        return 31 * result + this.z.hashCode();
    }

    // ═══════════════════════ 测试 ═══════════════════════

    public static void main(String[] args) {
        System.out.println("=== Vec3d256 测试 ===");
        Vec3d256 a = Vec3d256.of(12550821.5, 64.0, -12550821.25);
        Vec3d256 b = Vec3d256.of(12550822.0, 64.0, -12550820.0);
        System.out.println("a = " + a);
        System.out.println("a.toExactString() = " + a.toExactString());
        System.out.println("b - a = " + b.subtract(a));
        System.out.println("distance = " + a.distanceTo(b));
        System.out.println("distanceSqr = " + a.distanceToSqr(b));
        System.out.println("length = " + a.length());
        System.out.println("normalize = " + a.normalize());
        System.out.println("dot = " + a.dot(b));
        System.out.println("cross = " + a.cross(b));
        System.out.println("lerp(0.5) = " + a.lerp(b, Float256.of(0.5)));
        System.out.println("toVec3 = " + a.toVec3());
        System.out.println("fromVec3 = " + Vec3d256.fromVec3(new Vec3(1.0, 2.0, 3.0)));
    }
}
