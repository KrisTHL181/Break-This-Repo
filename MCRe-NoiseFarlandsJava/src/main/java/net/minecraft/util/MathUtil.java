package net.minecraft.util;

import net.minecraft.world.phys.Vec3;

public class MathUtil {

    // 类似于原版的 Mth.clamp，但是不会限制无穷值
    public static double clamp(double value, double min, double max) {
        if (Double.isInfinite(value)) return value;
        return value < min ? min : Math.min(value, max);
    }

    public static double lerp3(double delta1, double delta2, double delta3, double start1, double end1, double start2, double end2, double start3, double end3, double start4, double end4) {
        return lerp(delta3, lerp2(delta1, delta2, start1, end1, start2, end2), lerp2(delta1, delta2, start3, end3, start4, end4));
    }

    public static double lerp2(double delta1, double delta2, double start1, double end1, double start2, double end2) {
        return lerp(delta2, lerp(delta1, start1, end1), lerp(delta1, start2, end2));
    }

    // 类似于原版的 Mth.lerp，但是添加了对 delta == 0 的特殊处理，使得天空网格能够生成
    public static double lerp(double delta, double start, double end) {
        return delta == 0 ? start : start + delta * (end - start);
    }

    /**
     * 丢失坐标的精度
     * <p> 由于过程中将坐标转换成了浮点数，所以除了“主动精度丢失”外，还会附加上类似基岩版高坐标距离现象的“被动精度丢失”。
     *
     * @param pos  输入坐标
     * @param lose 丢失指数（该指数每增加1，就会多丢失一位；指数为5时，数值的小数部分全部丢失）
     * @return 丢失精度后的坐标。
     */
    public static Vec3 losePrecision(Vec3 pos, int lose) {
        return new Vec3(
                losePrecision((float) pos.x(), lose),
                losePrecision((float) pos.y(), lose),
                losePrecision((float) pos.z(), lose)
        );
    }

    /**
     * 丢失浮点数数值的精度
     *
     * @param original 输入数值
     * @param lose     丢失指数（该指数每增加1，就会多丢失一位；指数为5时，数值的小数部分全部丢失）
     * @return 丢失精度后的坐标
     */
    public static float losePrecision(float original, int lose) {
        lose = 5 - lose;
        return (float) (Math.round(original * Math.pow(2, lose)) / Math.pow(2, lose));
    }
}
