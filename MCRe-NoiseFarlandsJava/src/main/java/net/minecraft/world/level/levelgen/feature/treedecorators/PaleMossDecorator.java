package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.VegetationFeatures;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HangingMossBlock;

public class PaleMossDecorator extends TreeDecorator {
    public static final MapCodec<PaleMossDecorator> CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(
                Codec.floatRange(0.0F, 1.0F).fieldOf("leaves_probability").forGetter(p -> p.leavesProbability),
                Codec.floatRange(0.0F, 1.0F).fieldOf("trunk_probability").forGetter(p -> p.trunkProbability),
                Codec.floatRange(0.0F, 1.0F).fieldOf("ground_probability").forGetter(p -> p.groundProbability)
            )
            .apply(i, PaleMossDecorator::new)
    );
    private final float leavesProbability;
    private final float trunkProbability;
    private final float groundProbability;

    @Override
    protected TreeDecoratorType<?> type() {
        return TreeDecoratorType.PALE_MOSS;
    }

    public PaleMossDecorator(final float leavesProbability, final float trunkProbability, final float groundProbability) {
        this.leavesProbability = leavesProbability;
        this.trunkProbability = trunkProbability;
        this.groundProbability = groundProbability;
    }

    @Override
    public void place(final TreeDecorator.Context context) {
        RandomSource random = context.random();
        WorldGenLevel level = context.level();
        List<BlockPos> logs = Util.shuffledCopy(context.logs(), random);
        if (!logs.isEmpty()) {
            BlockPos origin = Collections.min(logs, Comparator.comparingInt(Vec3i::getY));
            if (random.nextFloat() < this.groundProbability) {
                level.registryAccess()
                    .lookup(Registries.CONFIGURED_FEATURE)
                    .flatMap(registry -> registry.get(VegetationFeatures.PALE_MOSS_PATCH))
                    .ifPresent(mossPatch -> mossPatch.value().place(level, level.getLevel().getChunkSource().getGenerator(), random, origin.above()));
            }

            context.logs().forEach(pos -> {
                if (random.nextFloat() < this.trunkProbability) {
                    BlockPos down = pos.below();
                    if (context.isAir(down)) {
                        addMossHanger(down, context);
                    }
                }
            });
            context.leaves().forEach(pos -> {
                if (random.nextFloat() < this.leavesProbability) {
                    BlockPos down = pos.below();
                    if (context.isAir(down)) {
                        addMossHanger(down, context);
                    }
                }
            });
        }
    }

    private static void addMossHanger(BlockPos pos, final TreeDecorator.Context context) {
        while (context.isAir(pos.below()) && !(context.random().nextFloat() < 0.5)) {
            context.setBlock(pos, Blocks.PALE_HANGING_MOSS.defaultBlockState().setValue(HangingMossBlock.TIP, false));
            pos = pos.below();
        }

        context.setBlock(pos, Blocks.PALE_HANGING_MOSS.defaultBlockState().setValue(HangingMossBlock.TIP, true));
    }
}