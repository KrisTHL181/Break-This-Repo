package net.minecraft.world.level.levelgen;

import net.MinecraftTools.Math.DynamicAccuracy.BigDecimal;
import net.MinecraftTools.Math.DynamicAccuracy.MathContext;
import net.minecraft.core.Direction;

/**
 * 🔧 MCRe NoiseFarlands —— 世界生成器偏移/缩放工具类
 *
 * <p>移植自 UltimateScaler (MIT, inf32768) 的 mixin 偏移逻辑，
 * 用我们的自研 BigDecimal (DynamicAccuracy 库) 实现"无大小限制"——
 * 避免之前用 Float256 时超大 scale/shift 转 int/long 触发 Int256 溢出崩溃
 * （{@code ArithmeticException: Int256 out of long range}）。
 *
 * <p>核心公式（一维）：{@code newPos = pos * scale + shift}
 *
 * <p>接入点：DensityFunctions.Noise/ShiftedNoise/Shift/ShiftA/ShiftB/YClampedGradient.compute，
 * BlendedNoise.compute，NoiseBasedChunkGenerator.createFluidPicker。
 *
 * <p>缓存策略：开世界时（WorldMainSettingScreen.onDone）调用 {@link #refresh} 一次性解析为 BigDecimal，
 * 运行期直接读内存数组，避免每个方块解析字符串。
 *
 * <p>本类位于 shared 包（levelgen），不依赖 client 包，避免循环依赖。
 */
public final class WorldReposition {
    /** 缩放因子（X, Y, Z），默认全 1（无缩放） */
    private static final BigDecimal[] SCALE = {BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE};
    /** 偏移量（X, Y, Z），默认全 0（无偏移） */
    private static final BigDecimal[] SHIFT = {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
    /** YClampedGradient 独立开关——默认 false（保持原版 Y 轴海拔梯度，避免边境之地消失） */
    private static volatile boolean yClampedGradientOffsetEnabled = false;

    private WorldReposition() {
    }

    // ═════════════════ 同步入口 ═════════════════

    /**
     * 🔧 从 {@link RepositionConfig} 刷新内存缓存（开世界时调用一次）。
     * 调用方负责把 String 解析为 BigDecimal（用 {@link #parseOrFallback}），本类不接触字符串。
     */
    public static void refresh(final RepositionConfig config) {
        SCALE[0] = config.scaleX();
        SCALE[1] = config.scaleY();
        SCALE[2] = config.scaleZ();
        SHIFT[0] = config.shiftX();
        SHIFT[1] = config.shiftY();
        SHIFT[2] = config.shiftZ();
        yClampedGradientOffsetEnabled = config.yClampedGradientOffset();
    }

    // ═════════════════ 一维变换（无损 BigDecimal） ═════════════════

    /** 一维变换 → BigDecimal（无损，公式：{@code newPos = pos * scale + shift}） */
    public static BigDecimal reposition(final BigDecimal pos, final Direction.Axis axis) {
        final int i = axis.ordinal();
        return pos.multiply(SCALE[i]).add(SHIFT[i]);
    }

    // ═════════════════ 一维变换（带 saturate 防护的 double 输出） ═════════════════

    /**
     * 一维变换 → double。BigDecimal 超 ±Double.MAX_VALUE 时 saturate 到 ±MAX_VALUE，
     * 避免 Infinity/NaN 传到 Minecraft 内部触发 NaN 链式崩溃。
     */
    public static double reposition(final double pos, final Direction.Axis axis) {
        return toDoubleSaturated(BigDecimal.valueOf(pos).multiply(SCALE[axis.ordinal()]).add(SHIFT[axis.ordinal()]));
    }

    /** 一维变换 → double（int 输入） */
    public static double reposition(final int pos, final Direction.Axis axis) {
        return toDoubleSaturated(BigDecimal.valueOf(pos).multiply(SCALE[axis.ordinal()]).add(SHIFT[axis.ordinal()]));
    }

    /** 一维变换 → double（long 输入） */
    public static double reposition(final long pos, final Direction.Axis axis) {
        return toDoubleSaturated(BigDecimal.valueOf(pos).multiply(SCALE[axis.ordinal()]).add(SHIFT[axis.ordinal()]));
    }

    // ═════════════════ 逆运算（用于 createFluidPicker 把世界 Y 还原到玩家 Y） ═════════════════

    /**
     * 🔧 Y 轴逆运算：把世界生成器输出的 Y 还原到玩家世界 Y。
     * 公式：{@code playerY = (worldY - shift) / scale}
     * 仅当 enabledYClampedGradientOffset=true 时调用（开关关闭时上层不走此函数）。
     * 用 DECIMAL64 (16 位精度) 做除法避免非终止小数抛 ArithmeticException。
     */
    public static int inverseY(final int worldY) {
        final BigDecimal playerY = BigDecimal.valueOf(worldY)
                .subtract(SHIFT[1])
                .divide(SCALE[1], MathContext.DECIMAL64);
        return playerY.intValue();  // 截断
    }

    // ═════════════════ 开关访问 ═════════════════

    /**
     * YClampedGradient 是否启用偏移——控制 Y 轴 base stone 海拔梯度。
     * 启用后 Y 轴不会出现任何边境之地，作为可选项让用户自己权衡。
     */
    public static boolean isYClampedGradientOffsetEnabled() {
        return yClampedGradientOffsetEnabled;
    }

    /**
     * 🔧 MCRe：读指定轴的缩放因子（用于逆运算，如 {@link #inverseY} 把世界 Y 还原到玩家 Y）。
     */
    public static BigDecimal getScale(final Direction.Axis axis) {
        return SCALE[axis.ordinal()];
    }

    /**
     * 🔧 MCRe：读指定轴的偏移量（同上）。
     */
    public static BigDecimal getShift(final Direction.Axis axis) {
        return SHIFT[axis.ordinal()];
    }

    // ═════════════════ 解析 helper（公开，调用方复用） ═════════════════

    /**
     * 把字符串解析为 BigDecimal（自研 DynamicAccuracy 库，原生支持 e/E、负号、小数），失败回退到 fallback。
     */
    public static BigDecimal parseOrFallback(final String s, final BigDecimal fallback) {
        if (s == null || s.trim().isEmpty()) {
            return fallback;
        }
        try {
            return new BigDecimal(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // ═════════════════ 配置快照 record ═════════════════

    /**
     * 🔧 MCRe：偏移/缩放配置快照——开世界时一次解析，运行期不再触碰字符串。
     * 位于 shared 包（levelgen），不引用 client 包，避免循环依赖。
     */
    public record RepositionConfig(
            BigDecimal scaleX, BigDecimal scaleY, BigDecimal scaleZ,
            BigDecimal shiftX, BigDecimal shiftY, BigDecimal shiftZ,
            boolean yClampedGradientOffset
    ) {
        /** 默认无变换配置（scale=1, shift=0, yGradient=false） */
        public static final RepositionConfig DISABLED = new RepositionConfig(
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                false
        );
    }

    // ═════════════════ 内部 saturate helper ═════════════════

    /**
     * 🔧 把 BigDecimal 转 double，超出 ±Double.MAX_VALUE 时 saturate 到 ±MAX_VALUE。
     * 避免 Minecraft 内部收到 Infinity/NaN 引发连锁崩溃。
     */
    private static double toDoubleSaturated(final BigDecimal value) {
        if (value.signum() > 0 && value.compareTo(BigDecimal.valueOf(Double.MAX_VALUE)) > 0) {
            return Double.MAX_VALUE;
        }
        if (value.signum() < 0 && value.compareTo(BigDecimal.valueOf(-Double.MAX_VALUE)) < 0) {
            return -Double.MAX_VALUE;
        }
        return value.doubleValue();
    }
}