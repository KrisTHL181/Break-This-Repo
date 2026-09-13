package net.minecraft.world.level.levelgen.structure.pools;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.GravityProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.apache.commons.lang3.mutable.MutableObject;

public class StructureTemplatePool {
   private static final int SIZE_UNSET = Integer.MIN_VALUE;
   private static final MutableObject<Codec<Holder<StructureTemplatePool>>> CODEC_REFERENCE = new MutableObject();
   public static final Codec<StructureTemplatePool> DIRECT_CODEC = RecordCodecBuilder.create(
      i -> i.group(
            Codec.lazyInitialized(CODEC_REFERENCE).fieldOf("fallback").forGetter(StructureTemplatePool::getFallback),
            Codec.mapPair(StructurePoolElement.CODEC.fieldOf("element"), Codec.intRange(1, 150).fieldOf("weight"))
               .codec()
               .listOf()
               .fieldOf("elements")
               .forGetter(p -> p.rawTemplates)
         )
         .apply(i, StructureTemplatePool::new)
   );
   public static final Codec<Holder<StructureTemplatePool>> CODEC = Util.make(
      RegistryFileCodec.create(Registries.TEMPLATE_POOL, DIRECT_CODEC), CODEC_REFERENCE::setValue
   );
   private final List<Pair<StructurePoolElement, Integer>> rawTemplates;
   private final ObjectArrayList<StructurePoolElement> templates;
   private final Holder<StructureTemplatePool> fallback;
   private int maxSize = Integer.MIN_VALUE;

   public StructureTemplatePool(final Holder<StructureTemplatePool> fallback, final List<Pair<StructurePoolElement, Integer>> templates) {
      this.rawTemplates = templates;
      this.templates = new ObjectArrayList();

      for (Pair<StructurePoolElement, Integer> templateDef : templates) {
         StructurePoolElement element = (StructurePoolElement)templateDef.getFirst();

         for (int i = 0; i < templateDef.getSecond(); i++) {
            this.templates.add(element);
         }
      }

      this.fallback = fallback;
   }

   public StructureTemplatePool(
      final Holder<StructureTemplatePool> fallback,
      final List<Pair<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>, Integer>> templates,
      final StructureTemplatePool.Projection projection
   ) {
      this.rawTemplates = Lists.newArrayList();
      this.templates = new ObjectArrayList();

      for (Pair<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>, Integer> templateDef : templates) {
         StructurePoolElement element = (StructurePoolElement)((Function)templateDef.getFirst()).apply(projection);
         this.rawTemplates.add(Pair.of(element, (Integer)templateDef.getSecond()));

         for (int i = 0; i < templateDef.getSecond(); i++) {
            this.templates.add(element);
         }
      }

      this.fallback = fallback;
   }

   public int getMaxSize(final StructureTemplateManager manager) {
      if (this.maxSize == Integer.MIN_VALUE) {
         this.maxSize = this.templates
            .stream()
            .filter(t -> t != EmptyPoolElement.INSTANCE)
            .mapToInt(t -> t.getBoundingBox(manager, BlockPos.ZERO, Rotation.NONE).getYSpan())
            .max()
            .orElse(0);
      }

      return this.maxSize;
   }

   @VisibleForTesting
   public List<Pair<StructurePoolElement, Integer>> getTemplates() {
      return this.rawTemplates;
   }

   public Holder<StructureTemplatePool> getFallback() {
      return this.fallback;
   }

   public StructurePoolElement getRandomTemplate(final RandomSource random) {
      return this.templates.isEmpty() ? EmptyPoolElement.INSTANCE : (StructurePoolElement)this.templates.get(random.nextInt(this.templates.size()));
   }

   public List<StructurePoolElement> getShuffledTemplates(final RandomSource random) {
      return Util.shuffledCopy(this.templates, random);
   }

   public int size() {
      return this.templates.size();
   }

   public enum Projection implements StringRepresentable {
      TERRAIN_MATCHING("terrain_matching", ImmutableList.of(new GravityProcessor(Heightmap.Types.WORLD_SURFACE_WG, -1))),
      RIGID("rigid", ImmutableList.of());

      public static final StringRepresentable.EnumCodec<StructureTemplatePool.Projection> CODEC = StringRepresentable.fromEnum(
         StructureTemplatePool.Projection::values
      );
      private final String name;
      private final ImmutableList<StructureProcessor> processors;

      Projection(final String name, final ImmutableList<StructureProcessor> processors) {
         this.name = name;
         this.processors = processors;
      }

      public String getName() {
         return this.name;
      }

      public static StructureTemplatePool.Projection byName(final String name) {
         return CODEC.byName(name);
      }

      public ImmutableList<StructureProcessor> getProcessors() {
         return this.processors;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }
   }
}
