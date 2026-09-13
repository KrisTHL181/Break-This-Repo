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

   private void remove(final AdvancementNode node) {
      for (AdvancementNode child : node.children()) {
         this.remove(child);
      }

      LOGGER.info("Forgot about advancement {}", node.holder());
      this.nodes.remove(node.holder().id());
      if (node.parent() == null) {
         this.roots.remove(node);
         if (this.listener != null) {
            this.listener.onRemoveAdvancementRoot(node);
         }
      } else {
         this.tasks.remove(node);
         if (this.listener != null) {
            this.listener.onRemoveAdvancementTask(node);
         }
      }
   }

   public void remove(final Set<Identifier> ids) {
      for (Identifier id : ids) {
         AdvancementNode advancement = this.nodes.get(id);
         if (advancement == null) {
            LOGGER.warn("Told to remove advancement {} but I don't know what that is", id);
         } else {
            this.remove(advancement);
         }
      }
   }

   public void addAll(final Collection<AdvancementHolder> advancements) {
      List<AdvancementHolder> advancementsToAdd = new ArrayList<>(advancements);

      while (!advancementsToAdd.isEmpty()) {
         if (!advancementsToAdd.removeIf(this::tryInsert)) {
            LOGGER.error("Couldn't load advancements: {}", advancementsToAdd);
            break;
         }
      }

      LOGGER.info("Loaded {} advancements", this.nodes.size());
   }

   private boolean tryInsert(final AdvancementHolder holder) {
      Optional<Identifier> parentId = holder.value().parent();
      AdvancementNode parentNode = parentId.map(this.nodes::get).orElse(null);
      if (parentNode == null && parentId.isPresent()) {
         return false;
      }

      AdvancementNode node = new AdvancementNode(holder, parentNode);
      if (parentNode != null) {
         parentNode.addChild(node);
      }

      this.nodes.put(holder.id(), node);
      if (parentNode == null) {
         this.roots.add(node);
         if (this.listener != null) {
            this.listener.onAddAdvancementRoot(node);
         }
      } else {
         this.tasks.add(node);
         if (this.listener != null) {
            this.listener.onAddAdvancementTask(node);
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

   public @Nullable AdvancementNode get(final Identifier id) {
      return this.nodes.get(id);
   }

   public @Nullable AdvancementNode get(final AdvancementHolder advancement) {
      return this.nodes.get(advancement.id());
   }

   public void setListener(final AdvancementTree.@Nullable Listener listener) {
      this.listener = listener;
      if (listener != null) {
         for (AdvancementNode root : this.roots) {
            listener.onAddAdvancementRoot(root);
         }

         for (AdvancementNode task : this.tasks) {
            listener.onAddAdvancementTask(task);
         }
      }
   }

   public interface Listener {
      void onAddAdvancementRoot(AdvancementNode root);

      void onRemoveAdvancementRoot(AdvancementNode root);

      void onAddAdvancementTask(AdvancementNode task);

      void onRemoveAdvancementTask(AdvancementNode task);

      void onAdvancementsCleared();
   }
}
