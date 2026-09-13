package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFileCodec;

public interface StructureProcessorType {
   Codec<StructureProcessor> SINGLE_CODEC = BuiltInRegistries.STRUCTURE_PROCESSOR.byNameCodec().dispatch("processor_type", StructureProcessor::codec, c -> c);
   Codec<StructureProcessorList> LIST_OBJECT_CODEC = SINGLE_CODEC.listOf().xmap(StructureProcessorList::new, StructureProcessorList::list);
   Codec<StructureProcessorList> DIRECT_CODEC = Codec.withAlternative(LIST_OBJECT_CODEC.fieldOf("processors").codec(), LIST_OBJECT_CODEC);
   Codec<Holder<StructureProcessorList>> LIST_CODEC = RegistryFileCodec.create(Registries.PROCESSOR_LIST, DIRECT_CODEC);
}
