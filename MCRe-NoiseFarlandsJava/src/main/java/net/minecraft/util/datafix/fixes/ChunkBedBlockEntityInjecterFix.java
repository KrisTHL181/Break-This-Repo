package net.minecraft.util.datafix.fixes;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.List.ListType;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ChunkBedBlockEntityInjecterFix extends DataFix {
    public ChunkBedBlockEntityInjecterFix(final Schema outputSchema, final boolean changesType) {
        super(outputSchema, changesType);
    }

    @Override
    public TypeRewriteRule makeRule() {
        Type<?> chunkType = this.getOutputSchema().getType(References.CHUNK);
        Type<?> levelType = chunkType.findFieldType("Level");
        if (!(levelType.findFieldType("TileEntities") instanceof ListType<?> tileEntityListType)) {
            throw new IllegalStateException("Tile entity type is not a list type.");
        } else {
            return this.cap(levelType, tileEntityListType);
        }
    }

    private <TE> TypeRewriteRule cap(final Type<?> levelType, final ListType<TE> tileEntityListType) {
        Type<TE> tileEntityType = tileEntityListType.getElement();
        OpticFinder<?> levelF = DSL.fieldFinder("Level", levelType);
        OpticFinder<List<TE>> tileEntitiesF = DSL.fieldFinder("TileEntities", tileEntityListType);
        int bedId = 416;
        return TypeRewriteRule.seq(
            this.fixTypeEverywhere(
                "InjectBedBlockEntityType",
                (com.mojang.datafixers.types.templates.TaggedChoice.TaggedChoiceType<String>)this.getInputSchema().findChoiceType(References.BLOCK_ENTITY),
                (com.mojang.datafixers.types.templates.TaggedChoice.TaggedChoiceType<String>)this.getOutputSchema().findChoiceType(References.BLOCK_ENTITY),
                ops -> v -> v
            ),
            this.fixTypeEverywhereTyped(
                "BedBlockEntityInjecter",
                this.getOutputSchema().getType(References.CHUNK),
                input -> {
                    Typed<?> level = input.getTyped(levelF);
                    Dynamic<?> levelTag = level.get(DSL.remainderFinder());
                    int chunkX = levelTag.get("xPos").asInt(0);
                    int chunkZ = levelTag.get("zPos").asInt(0);
                    List<TE> tileEntities = Lists.newArrayList(level.getOrCreate(tileEntitiesF));

                    for (Dynamic<?> sectionTag : levelTag.get("Sections").asList(Function.identity())) {
                        int pos = sectionTag.get("Y").asInt(0);
                        Streams.mapWithIndex(sectionTag.get("Blocks").asIntStream(), (block, index) -> {
                                if (416 == (block & 0xFF) << 4) {
                                    int p = (int)index;
                                    int xx = p & 15;
                                    int yy = p >> 8 & 15;
                                    int zz = p >> 4 & 15;
                                    Map<Dynamic<?>, Dynamic<?>> bedTag = Maps.newHashMap();
                                    bedTag.put(sectionTag.createString("id"), sectionTag.createString("minecraft:bed"));
                                    bedTag.put(sectionTag.createString("x"), sectionTag.createInt(xx + (chunkX << 4)));
                                    bedTag.put(sectionTag.createString("y"), sectionTag.createInt(yy + (pos << 4)));
                                    bedTag.put(sectionTag.createString("z"), sectionTag.createInt(zz + (chunkZ << 4)));
                                    bedTag.put(sectionTag.createString("color"), sectionTag.createShort((short)14));
                                    return bedTag;
                                } else {
                                    return null;
                                }
                            })
                            .forEachOrdered(
                                bedTag -> {
                                    if (bedTag != null) {
                                        tileEntities.add(
                                            tileEntityType.read(sectionTag.createMap((Map<? extends Dynamic<?>, ? extends Dynamic<?>>)bedTag))
                                                .result()
                                                .orElseThrow(() -> new IllegalStateException("Could not parse newly created bed block entity."))
                                                .getFirst()
                                        );
                                    }
                                }
                            );
                    }

                    return !tileEntities.isEmpty() ? input.set(levelF, level.set(tileEntitiesF, tileEntities)) : input;
                }
            )
        );
    }
}
