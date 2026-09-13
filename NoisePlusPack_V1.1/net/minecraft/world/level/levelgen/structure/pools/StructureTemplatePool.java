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
      p_450039_ -> p_450039_.group(
            Codec.lazyInitialized(CODEC_REFERENCE).fieldOf("fallback").forGetter(StructureTemplatePool::getFallback),
            Codec.mapPair(StructurePoolElement.CODEC.fieldOf("element"), Codec.intRange(1, 150).fieldOf("weight"))
               .codec()
               .listOf()
               .fieldOf("elements")
               .forGetter(p_210579_ -> p_210579_.rawTemplates)
         )
         .apply(p_450039_, StructureTemplatePool::new)
   );
   public static final Codec<Holder<StructureTemplatePool>> CODEC = Util.make(
      RegistryFileCodec.create(Registries.TEMPLATE_POOL, DIRECT_CODEC), CODEC_REFERENCE::setValue
   );
   private final List<Pair<StructurePoolElement, Integer>> rawTemplates;
   private final ObjectArrayList<StructurePoolElement> templates;
   private final Holder<StructureTemplatePool> fallback;
   private int maxSize = Integer.MIN_VALUE;

   public StructureTemplatePool(Holder<StructureTemplatePool> p_255747_, List<Pair<StructurePoolElement, Integer>> p_255919_) {
      this.rawTemplates = p_255919_;
      this.templates = new ObjectArrayList();

      for (Pair<StructurePoolElement, Integer> pair : p_255919_) {
         StructurePoolElement structurepoolelement = (StructurePoolElement)pair.getFirst();

         for (int i = 0; i < pair.getSecond(); i++) {
            this.templates.add(structurepoolelement);
         }
      }

      this.fallback = p_255747_;
   }

   public StructureTemplatePool(
      Holder<StructureTemplatePool> p_255795_,
      List<Pair<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>, Integer>> p_256083_,
      StructureTemplatePool.Projection p_255642_
   ) {
      this.rawTemplates = Lists.newArrayList();
      this.templates = new ObjectArrayList();

      for (Pair<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>, Integer> pair : p_256083_) {
         StructurePoolElement structurepoolelement = (StructurePoolElement)((Function)pair.getFirst()).apply(p_255642_);
         this.rawTemplates.add(Pair.of(structurepoolelement, (Integer)pair.getSecond()));

         for (int i = 0; i < pair.getSecond(); i++) {
            this.templates.add(structurepoolelement);
         }
      }

      this.fallback = p_255795_;
   }

   public int getMaxSize(StructureTemplateManager p_227358_) {
      if (this.maxSize == Integer.MIN_VALUE) {
         this.maxSize = this.templates
            .stream()
            .filter(p_210577_ -> p_210577_ != EmptyPoolElement.INSTANCE)
            .mapToInt(p_227361_ -> p_227361_.getBoundingBox(p_227358_, BlockPos.ZERO, Rotation.NONE).getYSpan())
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

   public StructurePoolElement getRandomTemplate(RandomSource p_227356_) {
      return this.templates.isEmpty() ? EmptyPoolElement.INSTANCE : (StructurePoolElement)this.templates.get(p_227356_.nextInt(this.templates.size()));
   }

   public List<StructurePoolElement> getShuffledTemplates(RandomSource p_227363_) {
      return Util.shuffledCopy(this.templates, p_227363_);
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

      Projection(final String p_210602_, final ImmutableList<StructureProcessor> p_210603_) {
         this.name = p_210602_;
         this.processors = p_210603_;
      }

      public String getName() {
         return this.name;
      }

      public static StructureTemplatePool.Projection byName(String p_210608_) {
         return CODEC.byName(p_210608_);
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
