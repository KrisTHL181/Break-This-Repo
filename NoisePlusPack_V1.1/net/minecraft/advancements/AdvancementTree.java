package net.minecraft.advancements;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class AdvancementTree {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Map<Identifier, AdvancementNode> nodes = new Object2ObjectOpenHashMap();
   private final Set<AdvancementNode> roots = new ObjectLinkedOpenHashSet();
   private final Set<AdvancementNode> tasks = new ObjectLinkedOpenHashSet();
   private AdvancementTree.@Nullable Listener listener;

   private void remove(AdvancementNode p_299357_) {
      for (AdvancementNode advancementnode : p_299357_.children()) {
         this.remove(advancementnode);
      }

      LOGGER.info("Forgot about advancement {}", p_299357_.holder());
      this.nodes.remove(p_299357_.holder().id());
      if (p_299357_.parent() == null) {
         this.roots.remove(p_299357_);
         if (this.listener != null) {
            this.listener.onRemoveAdvancementRoot(p_299357_);
         }
      } else {
         this.tasks.remove(p_299357_);
         if (this.listener != null) {
            this.listener.onRemoveAdvancementTask(p_299357_);
         }
      }
   }

   public void remove(Set<Identifier> p_297924_) {
      for (Identifier identifier : p_297924_) {
         AdvancementNode advancementnode = this.nodes.get(identifier);
         if (advancementnode == null) {
            LOGGER.warn("Told to remove advancement {} but I don't know what that is", identifier);
         } else {
            this.remove(advancementnode);
         }
      }
   }

   public void addAll(Collection<AdvancementHolder> p_299574_) {
      List<AdvancementHolder> list = new ArrayList<>(p_299574_);

      while (!list.isEmpty()) {
         if (!list.removeIf(this::tryInsert)) {
            LOGGER.error("Couldn't load advancements: {}", list);
            break;
         }
      }

      LOGGER.info("Loaded {} advancements", this.nodes.size());
   }

   private boolean tryInsert(AdvancementHolder p_300067_) {
      Optional<Identifier> optional = p_300067_.value().parent();
      AdvancementNode advancementnode = optional.map(this.nodes::get).orElse(null);
      if (advancementnode == null && optional.isPresent()) {
         return false;
      }

      AdvancementNode advancementnode1 = new AdvancementNode(p_300067_, advancementnode);
      if (advancementnode != null) {
         advancementnode.addChild(advancementnode1);
      }

      this.nodes.put(p_300067_.id(), advancementnode1);
      if (advancementnode == null) {
         this.roots.add(advancementnode1);
         if (this.listener != null) {
            this.listener.onAddAdvancementRoot(advancementnode1);
         }
      } else {
         this.tasks.add(advancementnode1);
         if (this.listener != null) {
            this.listener.onAddAdvancementTask(advancementnode1);
         }
      }

      return true;
   }

   public void clear() {
      this.nodes.clear();
      this.roots.clear();
      this.tasks.clear();
      if (this.listener != null) {
         this.listener.onAdvancementsCleared();
      }
   }

   public Iterable<AdvancementNode> roots() {
      return this.roots;
   }

   public Collection<AdvancementNode> nodes() {
      return this.nodes.values();
   }

   public @Nullable AdvancementNode get(Identifier p_460529_) {
      return this.nodes.get(p_460529_);
   }

   public @Nullable AdvancementNode get(AdvancementHolder p_299974_) {
      return this.nodes.get(p_299974_.id());
   }

   public void setListener(AdvancementTree.@Nullable Listener p_299884_) {
      this.listener = p_299884_;
      if (p_299884_ != null) {
         for (AdvancementNode advancementnode : this.roots) {
            p_299884_.onAddAdvancementRoot(advancementnode);
         }

         for (AdvancementNode advancementnode1 : this.tasks) {
            p_299884_.onAddAdvancementTask(advancementnode1);
         }
      }
   }

   public interface Listener {
      void onAddAdvancementRoot(AdvancementNode var1);

      void onRemoveAdvancementRoot(AdvancementNode var1);

      void onAddAdvancementTask(AdvancementNode var1);

      void onRemoveAdvancementTask(AdvancementNode var1);

      void onAdvancementsCleared();
   }
}
