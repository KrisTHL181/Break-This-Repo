package net.minecraft.server;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.Map;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.advancements.TreeNodePosition;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ServerAdvancementManager extends SimpleJsonResourceReloadListener<Advancement> {
   private static final Logger LOGGER = LogUtils.getLogger();
   private Map<Identifier, AdvancementHolder> advancements = Map.of();
   private AdvancementTree tree = new AdvancementTree();
   private final HolderLookup.Provider registries;

   public ServerAdvancementManager(HolderLookup.Provider p_336198_) {
      super(p_336198_, Advancement.CODEC, Registries.ADVANCEMENT);
      this.registries = p_336198_;
   }

   protected void apply(Map<Identifier, Advancement> p_136034_, ResourceManager p_136035_, ProfilerFiller p_136036_) {
      Builder<Identifier, AdvancementHolder> builder = ImmutableMap.builder();
      p_136034_.forEach((p_448845_, p_448846_) -> {
         this.validate(p_448845_, p_448846_);
         builder.put(p_448845_, new AdvancementHolder(p_448845_, p_448846_));
      });
      this.advancements = builder.buildOrThrow();
      AdvancementTree advancementtree = new AdvancementTree();
      advancementtree.addAll(this.advancements.values());

      for (AdvancementNode advancementnode : advancementtree.roots()) {
         if (advancementnode.holder().value().display().isPresent()) {
            TreeNodePosition.run(advancementnode);
         }
      }

      this.tree = advancementtree;
   }

   private void validate(Identifier p_457941_, Advancement p_310937_) {
      ProblemReporter.Collector problemreporter$collector = new ProblemReporter.Collector();
      p_310937_.validate(problemreporter$collector, this.registries);
      if (!problemreporter$collector.isEmpty()) {
         LOGGER.warn("Found validation problems in advancement {}: \n{}", p_457941_, problemreporter$collector.getReport());
      }
   }

   public @Nullable AdvancementHolder get(Identifier p_455112_) {
      return this.advancements.get(p_455112_);
   }

   public AdvancementTree tree() {
      return this.tree;
   }

   public Collection<AdvancementHolder> getAllAdvancements() {
      return this.advancements.values();
   }
}
