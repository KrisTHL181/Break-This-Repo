package net.minecraft.data.worldgen;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

public class Pools {
    public static final ResourceKey<StructureTemplatePool> EMPTY = createKey("empty");

    public static ResourceKey<StructureTemplatePool> createKey(final Identifier location) {
        return ResourceKey.create(Registries.TEMPLATE_POOL, location);
    }

    public static ResourceKey<StructureTemplatePool> createKey(final String name) {
        return createKey(Identifier.withDefaultNamespace(name));
    }

    public static ResourceKey<StructureTemplatePool> parseKey(final String name) {
        return createKey(Identifier.parse(name));
    }

    public static void register(final BootstrapContext<StructureTemplatePool> context, final String name, final StructureTemplatePool pool) {
        context.register(createKey(name), pool);
    }

    public static void bootstrap(final BootstrapContext<StructureTemplatePool> context) {
        HolderGetter<StructureTemplatePool> pools = context.lookup(Registries.TEMPLATE_POOL);
        Holder<StructureTemplatePool> empty = pools.getOrThrow(EMPTY);
        context.register(EMPTY, new StructureTemplatePool(empty, ImmutableList.of(), StructureTemplatePool.Projection.RIGID));
        BastionPieces.bootstrap(context);
        PillagerOutpostPools.bootstrap(context);
        VillagePools.bootstrap(context);
        AncientCityStructurePieces.bootstrap(context);
        TrailRuinsStructurePools.bootstrap(context);
        TrialChambersStructurePools.bootstrap(context);
    }
}