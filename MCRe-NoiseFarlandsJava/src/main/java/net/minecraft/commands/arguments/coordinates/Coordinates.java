package net.minecraft.commands.arguments.coordinates;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec2;
import java.util.Locale;
import net.minecraft.world.phys.Vec3;

public interface Coordinates {
    Vec3 getPosition(CommandSourceStack sender);

    Vec2 getRotation(CommandSourceStack sender);

    default BlockPos getBlockPos(final CommandSourceStack sender) {
        return BlockPos.containing(this.getPosition(sender));
    }

    boolean isXRelative();

    boolean isYRelative();

    boolean isZRelative();

    // 在 Coordinates 或 Vec3Argument 中
    private static double parseCoordinate(String input) {
        return switch (input.toLowerCase(Locale.ROOT)) {
            case "infinity", "+infinity" -> Double.POSITIVE_INFINITY;
            case "-infinity" -> Double.NEGATIVE_INFINITY;
            case "nan" -> Double.NaN;
            default -> Double.parseDouble(input);
        };
    }
}