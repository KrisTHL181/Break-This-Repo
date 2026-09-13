package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public record GeodeBlockSettings(
   BlockStateProvider fillingProvider,
   BlockStateProvider innerLayerProvider,
   BlockStateProvider alternateInnerLayerProvider,
   BlockStateProvider middleLayerProvider,
   BlockStateProvider outerLayerProvider,
   List<BlockState> innerPlacements,
   HolderSet<Block> cannotReplace,
   HolderSet<Block> invalidBlocks
) {
   public static final Codec<GeodeBlockSettings> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            BlockStateProvider.CODEC.fieldOf("filling_provider").forGetter(GeodeBlockSettings::fillingProvider),
            BlockStateProvider.CODEC.fieldOf("inner_layer_provider").forGetter(GeodeBlockSettings::innerLayerProvider),
            BlockStateProvider.CODEC.fieldOf("alternate_inner_layer_provider").forGetter(GeodeBlockSettings::alternateInnerLayerProvider),
            BlockStateProvider.CODEC.fieldOf("middle_layer_provider").forGetter(GeodeBlockSettings::middleLayerProvider),
            BlockStateProvider.CODEC.fieldOf("outer_layer_provider").forGetter(GeodeBlockSettings::outerLayerProvider),
            ExtraCodecs.nonEmptyList(BlockState.CODEC.listOf()).fieldOf("inner_placements").forGetter(GeodeBlockSettings::innerPlacements),
            RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("cannot_replace").forGetter(GeodeBlockSettings::cannotReplace),
            RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("invalid_blocks").forGetter(GeodeBlockSettings::invalidBlocks)
         )
         .apply(i, GeodeBlockSettings::new)
   );
}
