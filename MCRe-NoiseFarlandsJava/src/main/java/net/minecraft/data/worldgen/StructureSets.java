package net.minecraft.data.worldgen;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.BuiltinStructureSets;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

public interface StructureSets {
    static void bootstrap(final BootstrapContext<StructureSet> context) {
        HolderGetter<Structure> structures = context.lookup(Registries.STRUCTURE);
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        Holder.Reference<StructureSet> villages = context.register(
            BuiltinStructureSets.VILLAGES,
            new StructureSet(
                List.of(
                    StructureSet.entry(structures.getOrThrow(BuiltinStructures.VILLAGE_PLAINS)),
                    StructureSet.entry(structures.getOrThrow(BuiltinStructures.VILLAGE_DESERT)),
                    StructureSet.entry(structures.getOrThrow(BuiltinStructures.VILLAGE_SAVANNA)),
                    StructureSet.entry(structures.getOrThrow(BuiltinStructures.VILLAGE_SNOWY)),
                    StructureSet.entry(structures.getOrThrow(BuiltinStructures.VILLAGE_TAIGA))
                ),
                new RandomSpreadStructurePlacement(34, 8, RandomSpreadType.LINEAR, 10387312)
            )
        );
        context.register(
            BuiltinStructureSets.DESERT_PYRAMIDS,
            new StructureSet(
                structures.getOrThrow(BuiltinStructures.DESERT_PYRAMID), new RandomSpreadStructurePlacement(32, 8, RandomSpreadType.LINEAR, 14357617)
            )
        );
        context.register(
            BuiltinStructureSets.IGLOOS,
            new StructureSet(structures.getOrThrow(BuiltinStructures.IGLOO), new RandomSpreadStructurePlacement(32, 8, RandomSpreadType.LINEAR, 14357618))
        );
        context.register(
            BuiltinStructureSets.JUNGLE_TEMPLES,
            new StructureSet(
                structures.getOrThrow(BuiltinStructures.JUNGLE_TEMPLE), new RandomSpreadStructurePlacement(32, 8, RandomSpreadType.LINEAR, 14357619)
            )
        );
        context.register(
            BuiltinStructureSets.SWAMP_HUTS,
            new StructureSet(structures.getOrThrow(BuiltinStructures.SWAMP_HUT), new RandomSpreadStructurePlacement(32, 8, RandomSpreadType.LINEAR, 14357620))
        );
        context.register(
            BuiltinStructureSets.PILLAGER_OUTPOSTS,
            new StructureSet(
                structures.getOrThrow(BuiltinStructures.PILLAGER_OUTPOST),
                new RandomSpreadStructurePlacement(
                    Vec3i.ZERO,
                    StructurePlacement.FrequencyReductionMethod.LEGACY_TYPE_1,
                    0.2F,
                    165745296,
                    Optional.of(new StructurePlacement.ExclusionZone(villages, 10)),
                    32,
                    8,
                    RandomSpreadType.LINEAR
                )
            )
        );
        context.register(
            BuiltinStructureSets.ANCIENT_CITIES,
            new StructureSet(
                structures.getOrThrow(BuiltinStructures.ANCIENT_CITY), new RandomSpreadStructurePlacement(24, 8, RandomSpreadType.LINEAR, 20083232)
            )
        );
        context.register(
            BuiltinStructureSets.OCEAN_MONUMENTS,
            new StructureSet(
                structures.getOrThrow(BuiltinStructures.OCEAN_MONUMENT), new RandomSpreadStructurePlacement(32, 5, RandomSpreadType.TRIANGULAR, 10387313)
            )
        );
        context.register(
            BuiltinStructureSets.WOODLAND_MANSIONS,
            new StructureSet(
                structures.getOrThrow(BuiltinStructures.WOODLAND_MANSION), new RandomSpreadStructurePlacement(80, 20, RandomSpreadType.TRIANGULAR, 10387319)
            )
        );
        context.register(
            BuiltinStructureSets.BURIED_TREASURES,
            new StructureSet(
                structures.getOrThrow(BuiltinStructures.BURIED_TREASURE),
                new RandomSpreadStructurePlacement(
                    new Vec3i(9, 0, 9), StructurePlacement.FrequencyReductionMethod.LEGACY_TYPE_2, 0.01F, 0, Optional.empty(), 1, 0, RandomSpreadType.LINEAR
                )
            )
        );
        context.register(
            BuiltinStructureSets.MINESHAFTS,
            new StructureSet(
                List.of(
                    StructureSet.entry(structures.getOrThrow(BuiltinStructures.MINESHAFT)),
                    StructureSet.entry(structures.getOrThrow(BuiltinStructures.MINESHAFT_MESA))
                ),
                new RandomSpreadStructurePlacement(
                    Vec3i.ZERO, StructurePlacement.FrequencyReductionMethod.LEGACY_TYPE_3, 0.004F, 0, Optional.empty(), 1, 0, RandomSpreadType.LINEAR
                )
            )
        );
        context.register(
            BuiltinStructureSets.RUINED_PORTALS,
            new StructureSet(
                List.of(
                    StructureSet.entry(structures.getOrThrow(BuiltinStructures.RUINED_PORTAL_STANDARD)),
                    StructureSet.entry(structures.getOrThrow(BuiltinStructures.RUINED_PORTAL_DESERT)),
                    StructureSet.entry(structures.getOrThrow(BuiltinStructures.RUINED_PORTAL_JUNGLE)),
                    StructureSet.entry(structures.getOrThrow(BuiltinStructures.RUINED_PORTAL_SWAMP)),
                    StructureSet.entry(structures.getOrThrow(BuiltinStructures.RUINED_PORTAL_MOUNTAIN)),
                    StructureSet.entry(structures.getOrThrow(BuiltinStructures.RUINED_PORTAL_OCEAN)),
                    StructureSet.entry(structures.getOrThrow(BuiltinStructures.RUINED_PORTAL_NETHER))
                ),
                new RandomSpreadStructurePlacement(40, 15, RandomSpreadType.LINEAR, 34222645)
            )
        );
        context.register(
            BuiltinStructureSets.SHIPWRECKS,
            new StructureSet(
                List.of(
                    StructureSet.entry(structures.getOrThrow(BuiltinStructures.SHIPWRECK)),
                    StructureSet.entry(structures.getOrThrow(BuiltinStructures.SHIPWRECK_BEACHED))
                ),
                new RandomSpreadStructurePlacement(24, 4, RandomSpreadType.LINEAR, 165745295)
            )
        );
        context.register(
            BuiltinStructureSets.OCEAN_RUINS,
            new StructureSet(
                List.of(
                    StructureSet.entry(structures.getOrThrow(BuiltinStructures.OCEAN_RUIN_COLD)),
                    StructureSet.entry(structures.getOrThrow(BuiltinStructures.OCEAN_RUIN_WARM))
                ),
                new RandomSpreadStructurePlacement(20, 8, RandomSpreadType.LINEAR, 14357621)
            )
        );
        context.register(
            BuiltinStructureSets.NETHER_COMPLEXES,
            new StructureSet(
                List.of(
                    StructureSet.entry(structures.getOrThrow(BuiltinStructures.FORTRESS), 2),
                    StructureSet.entry(structures.getOrThrow(BuiltinStructures.BASTION_REMNANT), 3)
                ),
                new RandomSpreadStructurePlacement(27, 4, RandomSpreadType.LINEAR, 30084232)
            )
        );
        context.register(
            BuiltinStructureSets.NETHER_FOSSILS,
            new StructureSet(
                structures.getOrThrow(BuiltinStructures.NETHER_FOSSIL), new RandomSpreadStructurePlacement(2, 1, RandomSpreadType.LINEAR, 14357921)
            )
        );
        context.register(
            BuiltinStructureSets.END_CITIES,
            new StructureSet(
                structures.getOrThrow(BuiltinStructures.END_CITY), new RandomSpreadStructurePlacement(20, 11, RandomSpreadType.TRIANGULAR, 10387313)
            )
        );
        context.register(
            BuiltinStructureSets.STRONGHOLDS,
            new StructureSet(
                structures.getOrThrow(BuiltinStructures.STRONGHOLD),
                new ConcentricRingsStructurePlacement(32, 3, 128, biomes.getOrThrow(BiomeTags.STRONGHOLD_BIASED_TO))
            )
        );
        context.register(
            BuiltinStructureSets.TRAIL_RUINS,
            new StructureSet(structures.getOrThrow(BuiltinStructures.TRAIL_RUINS), new RandomSpreadStructurePlacement(34, 8, RandomSpreadType.LINEAR, 83469867))
        );
        context.register(
            BuiltinStructureSets.TRIAL_CHAMBERS,
            new StructureSet(
                structures.getOrThrow(BuiltinStructures.TRIAL_CHAMBERS), new RandomSpreadStructurePlacement(34, 12, RandomSpreadType.LINEAR, 94251327)
            )
        );
    }
}