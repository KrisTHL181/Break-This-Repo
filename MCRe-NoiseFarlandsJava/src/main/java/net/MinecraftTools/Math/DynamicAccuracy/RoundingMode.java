package net.MinecraftTools.Math.DynamicAccuracy;

@SuppressWarnings("deprecation")
public enum RoundingMode {
    UP(BigDecimal.ROUND_UP),

    DOWN(BigDecimal.ROUND_DOWN),

    CEILING(BigDecimal.ROUND_CEILING),

    FLOOR(BigDecimal.ROUND_FLOOR),

    HALF_UP(BigDecimal.ROUND_HALF_UP),

    HALF_DOWN(BigDecimal.ROUND_HALF_DOWN),

    HALF_EVEN(BigDecimal.ROUND_HALF_EVEN),

    UNNECESSARY(BigDecimal.ROUND_UNNECESSARY);

    final int oldMode;

    private RoundingMode(int oldMode) {
        this.oldMode = oldMode;
    }

    public static RoundingMode valueOf(int rm) {
        return switch (rm) {
            case BigDecimal.ROUND_UP -> UP;
            case BigDecimal.ROUND_DOWN -> DOWN;
            case BigDecimal.ROUND_CEILING -> CEILING;
            case BigDecimal.ROUND_FLOOR -> FLOOR;
            case BigDecimal.ROUND_HALF_UP -> HALF_UP;
            case BigDecimal.ROUND_HALF_DOWN -> HALF_DOWN;
            case BigDecimal.ROUND_HALF_EVEN -> HALF_EVEN;
            case BigDecimal.ROUND_UNNECESSARY -> UNNECESSARY;
            default -> throw new IllegalArgumentException("argument out of range");
        };
    }
}
