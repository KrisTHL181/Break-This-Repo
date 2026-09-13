package net.minecraft.util.datafix.fixes;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.List.ListType;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jspecify.annotations.Nullable;

public class ChunkProtoTickListFix extends DataFix {
    private static final int SECTION_WIDTH = 16;
    private static final ImmutableSet<String> ALWAYS_WATERLOGGED = ImmutableSet.of(
        "minecraft:bubble_column", "minecraft:kelp", "minecraft:kelp_plant", "minecraft:seagrass", "minecraft:tall_seagrass"
    );

    public ChunkProtoTickListFix(final Schema outputSchema) {
        super(outputSchema, false);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        Type<?> chunkType = this.getInputSchema().getType(References.CHUNK);
        OpticFinder<?> levelFinder = chunkType.findField("Level");
        OpticFinder<?> sectionsFinder = levelFinder.type().findField("Sections");
        OpticFinder<?> sectionFinder = ((ListType)sectionsFinder.type()).getElement().finder();
        OpticFinder<?> blockStateContainerFinder = sectionFinder.type().findField("block_states");
        OpticFinder<?> biomeContainerFinder = sectionFinder.type().findField("biomes");
        OpticFinder<?> blockStatePaletteFinder = blockStateContainerFinder.type().findField("palette");
        OpticFinder<?> tileTickFinder = levelFinder.type().findField("TileTicks");
        return this.fixTypeEverywhereTyped(
            "ChunkProtoTickListFix",
            chunkType,
            chunk -> chunk.updateTyped(
                levelFinder,
                level -> {
                    level = level.update(
                        DSL.remainderFinder(),
                        tag -> DataFixUtils.orElse(tag.get("LiquidTicks").result().map(v -> tag.set("fluid_ticks", (Dynamic<?>)v).remove("LiquidTicks")), tag)
                    );
                    Dynamic<?> chunkTag = level.get(DSL.remainderFinder());
                    MutableInt lowestY = new MutableInt();
                    Int2ObjectMap<Supplier<ChunkProtoTickListFix.PoorMansPalettedContainer>> palettedContainers = new Int2ObjectArrayMap<>();
                    level.getOptionalTyped(sectionsFinder)
                        .ifPresent(
                            sections -> sections.getAllTyped(sectionFinder)
                                .forEach(
                                    section -> {
                                        Dynamic<?> sectionRemainder = section.get(DSL.remainderFinder());
                                        int sectionY = sectionRemainder.get("Y").asInt(Integer.MAX_VALUE);
                                        if (sectionY != Integer.MAX_VALUE) {
                                            if (section.getOptionalTyped(biomeContainerFinder).isPresent()) {
                                                lowestY.setValue(Math.min(sectionY, lowestY.intValue()));
                                            }

                                            section.getOptionalTyped(blockStateContainerFinder)
                                                .ifPresent(
                                                    blockContainer -> palettedContainers.put(
                                                        sectionY,
                                                        Suppliers.memoize(
                                                            () -> {
                                                                List<? extends Dynamic<?>> palette = blockContainer.getOptionalTyped(blockStatePaletteFinder)
                                                                    .map(
                                                                        x -> x.write()
                                                                            .result()
                                                                            .map(r -> r.asList(Function.identity()))
                                                                            .orElse(Collections.emptyList())
                                                                    )
                                                                    .orElse(Collections.emptyList());
                                                                long[] data = blockContainer.get(DSL.remainderFinder()).get("data").asLongStream().toArray();
                                                                return new ChunkProtoTickListFix.PoorMansPalettedContainer(palette, data);
                                                            }
                                                        )
                                                    )
                                                );
                                        }
                                    }
                                )
                        );
                    byte sectionMinY = lowestY.byteValue();
                    level = level.update(DSL.remainderFinder(), remainder -> remainder.update("yPos", y -> y.createByte(sectionMinY)));
                    if (!level.getOptionalTyped(tileTickFinder).isPresent() && !chunkTag.get("fluid_ticks").result().isPresent()) {
                        int sectionX = chunkTag.get("xPos").asInt(0);
                        int sectionZ = chunkTag.get("zPos").asInt(0);
                        Dynamic<?> fluidTicks = this.makeTickList(
                            chunkTag, palettedContainers, sectionMinY, sectionX, sectionZ, "LiquidsToBeTicked", ChunkProtoTickListFix::getLiquid
                        );
                        Dynamic<?> blockTicks = this.makeTickList(
                            chunkTag, palettedContainers, sectionMinY, sectionX, sectionZ, "ToBeTicked", ChunkProtoTickListFix::getBlock
                        );
                        Optional<? extends Pair<? extends Typed<?>, ?>> parsedBlockTicks = tileTickFinder.type().readTyped(blockTicks).result();
                        if (parsedBlockTicks.isPresent()) {
                            level = level.set(tileTickFinder, (Typed<?>)parsedBlockTicks.get().getFirst());
                        }

                        return level.update(
                            DSL.remainderFinder(), remainder -> remainder.remove("ToBeTicked").remove("LiquidsToBeTicked").set("fluid_ticks", fluidTicks)
                        );
                    } else {
                        return level;
                    }
                }
            )
        );
    }

    private Dynamic<?> makeTickList(
        final Dynamic<?> tag,
        final Int2ObjectMap<Supplier<ChunkProtoTickListFix.PoorMansPalettedContainer>> palettedContainers,
        final byte sectionMinY,
        final int sectionX,
        final int sectionZ,
        final String protoTickListTag,
        final Function<Dynamic<?>, String> typeGetter
    ) {
        Stream<Dynamic<?>> newTickList = Stream.empty();
        List<? extends Dynamic<?>> ticksPerSection = tag.get(protoTickListTag).asList(Function.identity());

        for (int sectionYIndex = 0; sectionYIndex < ticksPerSection.size(); sectionYIndex++) {
            int sectionY = sectionYIndex + sectionMinY;
            Supplier<ChunkProtoTickListFix.PoorMansPalettedContainer> container = palettedContainers.get(sectionY);
            Stream<? extends Dynamic<?>> newTickListForSection = ticksPerSection.get(sectionYIndex)
                .asStream()
                .mapToInt(pos -> pos.asShort((short)-1))
                .filter(pos -> pos > 0)
                .mapToObj(pos -> this.createTick(tag, container, sectionX, sectionY, sectionZ, pos, typeGetter));
            newTickList = Stream.concat(newTickList, newTickListForSection);
        }

        return tag.createList(newTickList);
    }

    private static String getBlock(final @Nullable Dynamic<?> blockState) {
        return blockState != null ? blockState.get("Name").asString("minecraft:air") : "minecraft:air";
    }

    private static String getLiquid(final @Nullable Dynamic<?> blockState) {
        if (blockState == null) {
            return "minecraft:empty";
        } else {
            String block = blockState.get("Name").asString("");
            if ("minecraft:water".equals(block)) {
                return blockState.get("Properties").get("level").asInt(0) == 0 ? "minecraft:water" : "minecraft:flowing_water";
            } else if ("minecraft:lava".equals(block)) {
                return blockState.get("Properties").get("level").asInt(0) == 0 ? "minecraft:lava" : "minecraft:flowing_lava";
            } else {
                return !ALWAYS_WATERLOGGED.contains(block) && !blockState.get("Properties").get("waterlogged").asBoolean(false)
                    ? "minecraft:empty"
                    : "minecraft:water";
            }
        }
    }

    private Dynamic<?> createTick(
        final Dynamic<?> tag,
        final @Nullable Supplier<ChunkProtoTickListFix.PoorMansPalettedContainer> container,
        final int sectionX,
        final int sectionY,
        final int sectionZ,
        final int pos,
        final Function<Dynamic<?>, String> typeGetter
    ) {
        int relativeX = pos & 15;
        int relativeY = pos >>> 4 & 15;
        int relativeZ = pos >>> 8 & 15;
        String type = typeGetter.apply(container != null ? container.get().get(relativeX, relativeY, relativeZ) : null);
        return tag.createMap(
            ImmutableMap.<Dynamic<?>, Dynamic<?>>builder()
                .put(tag.createString("i"), tag.createString(type))
                .put(tag.createString("x"), tag.createInt(sectionX * 16 + relativeX))
                .put(tag.createString("y"), tag.createInt(sectionY * 16 + relativeY))
                .put(tag.createString("z"), tag.createInt(sectionZ * 16 + relativeZ))
                .put(tag.createString("t"), tag.createInt(0))
                .put(tag.createString("p"), tag.createInt(0))
                .build()
        );
    }

    public static final class PoorMansPalettedContainer {
        private static final long SIZE_BITS = 4L;
        private final List<? extends Dynamic<?>> palette;
        private final long[] data;
        private final int bits;
        private final long mask;
        private final int valuesPerLong;

        public PoorMansPalettedContainer(final List<? extends Dynamic<?>> palette, final long[] data) {
            this.palette = palette;
            this.data = data;
            this.bits = Math.max(4, ChunkHeightAndBiomeFix.ceillog2(palette.size()));
            this.mask = (1L << this.bits) - 1L;
            this.valuesPerLong = (char)(64 / this.bits);
        }

        public @Nullable Dynamic<?> get(final int x, final int y, final int z) {
            int entryCount = this.palette.size();
            if (entryCount < 1) {
                return null;
            } else if (entryCount == 1) {
                return (Dynamic<?>)this.palette.getFirst();
            } else {
                int index = this.getIndex(x, y, z);
                int cellIndex = index / this.valuesPerLong;
                if (cellIndex >= 0 && cellIndex < this.data.length) {
                    long cellValue = this.data[cellIndex];
                    int bitIndex = (index - cellIndex * this.valuesPerLong) * this.bits;
                    int paletteIndex = (int)(cellValue >> bitIndex & this.mask);
                    return (Dynamic<?>)(paletteIndex >= 0 && paletteIndex < entryCount ? this.palette.get(paletteIndex) : null);
                } else {
                    return null;
                }
            }
        }

        private int getIndex(final int x, final int y, final int z) {
            return (y << 4 | z) << 4 | x;
        }

        public List<? extends Dynamic<?>> palette() {
            return this.palette;
        }

        public long[] data() {
            return this.data;
        }
    }
}
