package net.minecraft.client.gui.components.debug;

import java.util.List;
import java.util.Locale;
import net.MinecraftTools.Math.DynamicAccuracy.BigDecimal;
import net.MinecraftTools.Math.DynamicAccuracy.BigInteger;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.WorldReposition;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.MinecraftTools.Math._256Bit.Float256;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class DebugEntryPosition implements DebugScreenEntry {
    public static final Identifier GROUP = Identifier.withDefaultNamespace("position");

    /** 精确显示小数部分的最大位数（超出截断 + 省略号） */
    private static final int MAX_FRAC_DIGITS = 14;

    /** double → Float256 → 精确十进制（完整 52-bit 尾数展开，限长显示） */
    private static String fmtExact(final double value) {
        String s = Float256.of(value).toExactString();
        int dot = s.indexOf('.');
        if (dot < 0) return s;
        int frac = s.length() - dot - 1;
        if (frac <= MAX_FRAC_DIGITS) return s;
        return s.substring(0, dot + MAX_FRAC_DIGITS + 1) + "…";
    }

    /**
     * 🔧 MCRe：BigInteger 整数显示（Terrain XYZ 用，无小数点、无精度损失，直接 toString）。
     * 跟 XYZ/XYZ(Camera) 的 fmtExact 输出格式对齐（不省略、无限位数）。
     */
    private static String fmtBigInt(final BigInteger value) {
        return value.toString();
    }

    /**
     * 🔧 MCRe：计算 Terrain XYZ（玩家坐标经 WorldReposition 偏移缩放 → BigDecimal → BigInteger 截断）。
     * <p><b>精度关键：</b>玩家坐标是 double，必须走 IEEE 754 位精确转换（Float256.of）而非 Double.toString，
     * 否则 Double.toString 的科学记数法截断到 17 位有效数字会导致大坐标丢失精度（如 9223372036854776000 而非 9223372036854775808）。
     * <p>无大小限制：scale/shift 即使是 1e49 也能精确算出 BigInteger 整数地形坐标。
     * <p>无精度损失：Float256.of(double) 保留所有 64-bit IEEE 754 信息 → toBigDecimal() 精确 → reposition BigDecimal 精确 → toBigInteger 截断。
     */
    private static BigInteger[] computeTerrainXYZ(final double playerX, final double playerY, final double playerZ) {
        return new BigInteger[] {
            WorldReposition.reposition(Float256.of(playerX).toBigDecimal(), Direction.Axis.X).toBigInteger(),
            WorldReposition.reposition(Float256.of(playerY).toBigDecimal(), Direction.Axis.Y).toBigInteger(),
            WorldReposition.reposition(Float256.of(playerZ).toBigDecimal(), Direction.Axis.Z).toBigInteger()
        };
    }

    @Override
    public void display(
        final DebugScreenDisplayer displayer,
        final @Nullable Level serverOrClientLevel,
        final @Nullable LevelChunk clientChunk,
        final @Nullable LevelChunk serverChunk
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity entity = minecraft.getCameraEntity();
        if (entity != null) {
            BlockPos feetPos = minecraft.getCameraEntity().blockPosition();
            ChunkPos chunkPos = ChunkPos.containing(feetPos);
            Direction direction = entity.getDirection();

            // ===== 正确的摄像机获取方式 =====
            Camera camera = minecraft.gameRenderer.mainCamera();
            double camX = camera.position().x;
            double camY = camera.position().y;
            double camZ = camera.position().z;

            String faceString = switch (direction) {
                case NORTH -> "Towards negative Z";
                case SOUTH -> "Towards positive Z";
                case WEST -> "Towards negative X";
                case EAST -> "Towards positive X";
                default -> "Invalid";
            };
            java.util.Set<ChunkPos> chunks = serverOrClientLevel instanceof ServerLevel serverLevel ? serverLevel.getForceLoadedChunks() : java.util.Set.of();

            // ===== 🔧 MCRe：计算 Terrain XYZ（玩家坐标经 WorldReposition 偏移缩放）=====
            final BigInteger[] terrainXYZ = computeTerrainXYZ(entity.getX(), entity.getY(), entity.getZ());

            // ===== 精度计算（基于 Terrain XYZ 的 BigInteger 值，反映玩家坐标在「地形生成器世界」里的实际精度）=====
            final BigInteger maxAbs = terrainXYZ[0].abs().max(terrainXYZ[1].abs()).max(terrainXYZ[2].abs());
            final int bitLen = maxAbs.bitLength();  // 最高位 1 的位置（等价于 64 - Long.numberOfLeadingZeros）
            final double doublePrecision = Math.pow(2.0, (double)(bitLen - 53));
            final double floatPrecision = Math.pow(2.0, (double)(bitLen - 24));
            final String precisionString = "Current precision: §" + getColorCodeFromPrecision(doublePrecision) + doublePrecision
                                   + "§r (float: §" + getColorCodeFromPrecision(floatPrecision) + floatPrecision + "§r)";

            displayer.addToGroup(
                GROUP,
                List.of(
                    // ===== 256-bit 精确坐标（完整展开 double 的 52-bit 尾数） =====
                    "XYZ: " + fmtExact(entity.getX()) + " / " + fmtExact(entity.getY()) + " / " + fmtExact(entity.getZ()),
                    // ===== 256-bit 精确摄像机坐标 =====
                    "XYZ(Camera): " + fmtExact(camX) + " / " + fmtExact(camY) + " / " + fmtExact(camZ),
                    // ===== 🔧 MCRe：地形生成器坐标（玩家坐标经偏移缩放，无精度损失 BigInteger）=====
                    "Terrain XYZ(BigInteger): " + fmtBigInt(terrainXYZ[0]) + " / " + fmtBigInt(terrainXYZ[1]) + " / " + fmtBigInt(terrainXYZ[2]),
                    String.format(Locale.ROOT, "Block: %d %d %d", feetPos.getX(), feetPos.getY(), feetPos.getZ()),
                    String.format(
                        Locale.ROOT,
                        "Chunk: %d %d %d [%d %d in r.%d.%d.mca]",
                        (int)chunkPos.x(),
                        SectionPos.blockToSectionCoord(feetPos.getY()),
                        (int)chunkPos.z(),
                        (int)chunkPos.getRegionLocalX(),
                        (int)chunkPos.getRegionLocalZ(),
                        (int)chunkPos.getRegionX(),
                        (int)chunkPos.getRegionZ()
                    ),
                    precisionString,
                    String.format(
                        Locale.ROOT,
                        "Facing: %s (%s) (%.1f / %.1f)",
                        direction,
                        faceString,
                        Mth.wrapDegrees(entity.getYRot()),
                        Mth.wrapDegrees(entity.getXRot())
                    ),
                    minecraft.level.dimension().identifier() + " FC: " + chunks.size()
                )
            );
        }
    }

    private static char getColorCodeFromPrecision(double precision) {
        if (precision <= 0.03125) {
            return 'a';
        } else if (precision > 0.25) {
            return 'c';
        } else {
            return 'e';
        }
    }
}