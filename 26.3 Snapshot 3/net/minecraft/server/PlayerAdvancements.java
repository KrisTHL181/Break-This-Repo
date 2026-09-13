package net.minecraft.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.CriterionTrigger;
import net.minecraft.network.protocol.game.ClientboundSelectAdvancementsTabPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.advancements.AdvancementVisibilityEvaluator;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.FileUtil;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.gamerules.GameRules;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class PlayerAdvancements {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private final PlayerList playerList;
   private final Path playerSavePath;
   private AdvancementTree tree;
   private final Map<AdvancementHolder, AdvancementProgress> progress = new LinkedHashMap<>();
   private final Set<AdvancementHolder> visible = new HashSet<>();
   private final Set<AdvancementHolder> progressChanged = new HashSet<>();
   private final Set<AdvancementNode> rootsToUpdate = new HashSet<>();
   private ServerPlayer player;
   private @Nullable AdvancementHolder lastSelectedTab;
   private boolean isFirstPacket = true;
   private final Codec<PlayerAdvancements.Data> codec;
   private final Map<CriterionTrigger<?>, Map<PlayerAdvancements.TriggerInstanceKey, ? extends CriterionTriggerInstance>> activeTriggers = new IdentityHashMap<>();

   public PlayerAdvancements(
      final DataFixer dataFixer, final PlayerList playerList, final ServerAdvancementManager manager, final Path playerSavePath, final ServerPlayer player
   ) {
      this.playerList = playerList;
      this.playerSavePath = playerSavePath;
      this.player = player;
      this.tree = manager.tree();
      int defaultVersion = 1343;
      this.codec = DataFixTypes.ADVANCEMENTS.wrapCodec(PlayerAdvancements.Data.CODEC, dataFixer, 1343);
      this.load(manager);
   }

   public void setPlayer(final ServerPlayer player) {
      this.player = player;
   }

   public void clearTriggers() {
      this.activeTriggers.clear();
   }

   public void reload(final ServerAdvancementManager manager) {
      this.clearTriggers();
      this.progress.clear();
      this.visible.clear();
      this.rootsToUpdate.clear();
      this.progressChanged.clear();
      this.isFirstPacket = true;
      this.lastSelectedTab = null;
      this.tree = manager.tree();
      this.load(manager);
   }

   private void registerListeners(final ServerAdvancementManager manager) {
      for (AdvancementHolder advancement : manager.getAllAdvancements()) {
         this.registerListeners(advancement);
      }
   }

   private void checkForAutomaticTriggers(final ServerAdvancementManager manager) {
      for (AdvancementHolder holder : manager.getAllAdvancements()) {
         Advancement advancement = holder.value();
         if (advancement.criteria().isEmpty()) {
            this.award(holder, "");
            advancement.rewards().grant(this.player);
         }
      }
   }

   private void load(final ServerAdvancementManager manager) {
      if (Files.isRegularFile(this.playerSavePath)) {
         try (Reader reader = Files.newBufferedReader(this.playerSavePath, StandardCharsets.UTF_8)) {
            JsonElement json = StrictJsonParser.parse(reader);
            PlayerAdvancements.Data data = (PlayerAdvancements.Data)this.codec.parse(JsonOps.INSTANCE, json).getOrThrow(JsonParseException::new);
            this.applyFrom(manager, data);
         } catch (IOException | JsonIOException e) {
            LOGGER.error("Couldn't access player advancements in {}", this.playerSavePath, e);
         } catch (JsonParseException e) {
            LOGGER.error("Couldn't parse player advancements in {}", this.playerSavePath, e);
         }
      }

      this.checkForAutomaticTriggers(manager);
      this.registerListeners(manager);
   }

   public void save() {
      JsonElement json = (JsonElement)this.codec.encodeStart(JsonOps.INSTANCE, this.asData()).getOrThrow();

      try {
         FileUtil.createDirectoriesSafe(this.playerSavePath.getParent());

         try (Writer outputWriter = Files.newBufferedWriter(this.playerSavePath, StandardCharsets.UTF_8)) {
            GSON.toJson(json, GSON.newJsonWriter(outputWriter));
         }
      } catch (IOException | JsonIOException e) {
         LOGGER.error("Couldn't save player advancements to {}", this.playerSavePath, e);
      }
   }

   private void applyFrom(final ServerAdvancementManager manager, final PlayerAdvancements.Data data) {
      data.forEach((id, progress) -> {
         AdvancementHolder advancement = manager.get(id);
         if (advancement == null) {
            LOGGER.warn("Ignored advancement '{}' in progress file {} - it doesn't exist anymore?", id, this.playerSavePath);
         } else {
            this.startProgress(advancement, progress);
            this.progressChanged.add(advancement);
            this.markForVisibilityUpdate(advancement);
         }
      });
   }

   private PlayerAdvancements.Data asData() {
      Map<Identifier, AdvancementProgress> map = new LinkedHashMap<>();
      this.progress.forEach((advancement, progress) -> {
         if (progress.hasProgress()) {
            map.put(advancement.id(), progress);
         }
      });
      return new PlayerAdvancements.Data(map);
   }

   public boolean award(final AdvancementHolder holder, final String criterion) {
      boolean result = false;
      AdvancementProgress progress = this.getOrStartProgress(holder);
      boolean wasDone = progress.isDone();
      if (progress.grantProgress(criterion)) {
         this.unregisterListeners(holder);
         this.progressChanged.add(holder);
         result = true;
         if (!wasDone && progress.isDone()) {
            holder.value().rewards().grant(this.player);
            holder.value().display().ifPresent(display -> {
               if (display.shouldAnnounceChat() && this.player.level().getGameRules().get(GameRules.SHOW_ADVANCEMENT_MESSAGES)) {
                  this.playerList.broadcastSystemMessage(display.getType().createAnnouncement(holder, this.player), false);
               }
            });
         }
      }

      if (!wasDone && progress.isDone()) {
         this.markForVisibilityUpdate(holder);
      }

      return result;
   }

   public boolean revoke(final AdvancementHolder advancement, final String criterion) {
      boolean result = false;
      AdvancementProgress progress = this.getOrStartProgress(advancement);
      boolean wasDone = progress.isDone();
      if (progress.revokeProgress(criterion)) {
         this.registerListeners(advancement);
         this.progressChanged.add(advancement);
         result = true;
      }

      if (wasDone && !progress.isDone()) {
         this.markForVisibilityUpdate(advancement);
      }

      return result;
   }

   private void markForVisibilityUpdate(final AdvancementHolder advancement) {
      AdvancementNode node = this.tree.get(advancement);
      if (node != null) {
         this.rootsToUpdate.add(node.root());
      }
   }

   private void registerListeners(final AdvancementHolder holder) {
      AdvancementProgress advancementProgress = this.getOrStartProgress(holder);
      if (!advancementProgress.isDone()) {
         for (Entry<String, Criterion<?>> entry : holder.value().criteria().entrySet()) {
            CriterionProgress criterionProgress = advancementProgress.getCriterion(entry.getKey());
            if (criterionProgress != null && !criterionProgress.isDone()) {
               this.addListener(entry.getValue(), new PlayerAdvancements.TriggerInstanceKey(holder, entry.getKey()));
            }
         }
      }
   }

   private void unregisterListeners(final AdvancementHolder holder) {
      AdvancementProgress advancementProgress = this.getOrStartProgress(holder);

      for (Entry<String, Criterion<?>> entry : holder.value().criteria().entrySet()) {
         CriterionProgress criterionProgress = advancementProgress.getCriterion(entry.getKey());
         if (criterionProgress != null && (criterionProgress.isDone() || advancementProgress.isDone())) {
            this.removeListener(entry.getValue().trigger(), new PlayerAdvancements.TriggerInstanceKey(holder, entry.getKey()));
         }
      }
   }

   public void flushDirty(final ServerPlayer player, final boolean showAdvancements) {
      if (this.isFirstPacket || !this.rootsToUpdate.isEmpty() || !this.progressChanged.isEmpty()) {
         Map<Identifier, AdvancementProgress> progress = new HashMap<>();
         Set<AdvancementHolder> added = new HashSet<>();
         Set<Identifier> removed = new HashSet<>();

         for (AdvancementNode root : this.rootsToUpdate) {
            this.updateTreeVisibility(root, added, removed);
         }

         this.rootsToUpdate.clear();

         for (AdvancementHolder holder : this.progressChanged) {
            if (this.visible.contains(holder)) {
               progress.put(holder.id(), this.progress.get(holder));
            }
         }

         this.progressChanged.clear();
         if (!progress.isEmpty() || !added.isEmpty() || !removed.isEmpty()) {
            player.connection.send(new ClientboundUpdateAdvancementsPacket(this.isFirstPacket, added, removed, progress, showAdvancements));
         }
      }

      this.isFirstPacket = false;
   }

   public void setSelectedTab(final @Nullable AdvancementHolder holder) {
      AdvancementHolder old = this.lastSelectedTab;
      if (holder != null && holder.value().isRoot() && holder.value().display().isPresent()) {
         this.lastSelectedTab = holder;
      } else {
         this.lastSelectedTab = null;
      }

      if (old != this.lastSelectedTab) {
         this.player.connection.send(new ClientboundSelectAdvancementsTabPacket(this.lastSelectedTab == null ? null : this.lastSelectedTab.id()));
      }
   }

   public AdvancementProgress getOrStartProgress(final AdvancementHolder advancement) {
      AdvancementProgress progress = this.progress.get(advancement);
      if (progress == null) {
         progress = new AdvancementProgress();
         this.startProgress(advancement, progress);
      }

      return progress;
   }

   private void startProgress(final AdvancementHolder holder, final AdvancementProgress progress) {
      progress.update(holder.value().requirements());
      this.progress.put(holder, progress);
   }

   private void updateTreeVisibility(final AdvancementNode root, final Set<AdvancementHolder> added, final Set<Identifier> removed) {
      AdvancementVisibilityEvaluator.evaluateVisibility(root, node -> this.getOrStartProgress(node.holder()).isDone(), (node, shouldBeVisible) -> {
         AdvancementHolder advancement = node.holder();
         if (shouldBeVisible) {
            if (this.visible.add(advancement)) {
               added.add(advancement);
               if (this.progress.containsKey(advancement)) {
                  this.progressChanged.add(advancement);
               }
            }
         } else if (this.visible.remove(advancement)) {
            removed.add(advancement.id());
         }
      });
   }

   private <T extends CriterionTriggerInstance> Map<PlayerAdvancements.TriggerInstanceKey, T> getOrCreateTriggerMapForType(final CriterionTrigger<T> type) {
      return (Map<PlayerAdvancements.TriggerInstanceKey, T>)this.activeTriggers.computeIfAbsent(type, var0 -> new HashMap<>());
   }

   private <T extends CriterionTriggerInstance> void addListener(final Criterion<T> typeAndInstance, final PlayerAdvancements.TriggerInstanceKey criterion) {
      this.getOrCreateTriggerMapForType(typeAndInstance.trigger()).put(criterion, typeAndInstance.triggerInstance());
   }

   public <T extends CriterionTriggerInstance> @Nullable Map<PlayerAdvancements.TriggerInstanceKey, T> getTriggerMapForType(final CriterionTrigger<T> type) {
      return (Map<PlayerAdvancements.TriggerInstanceKey, T>)this.activeTriggers.get(type);
   }

   private <T extends CriterionTriggerInstance> void removeListener(final CriterionTrigger<T> type, final PlayerAdvancements.TriggerInstanceKey criterion) {
      Map<PlayerAdvancements.TriggerInstanceKey, T> map = this.getTriggerMapForType(type);
      if (map != null) {
         map.remove(criterion);
         if (map.isEmpty()) {
            this.activeTriggers.remove(type);
         }
      }
   }

   private record Data(Map<Identifier, AdvancementProgress> map) {
      public static final Codec<PlayerAdvancements.Data> CODEC = Codec.unboundedMap(Identifier.CODEC, AdvancementProgress.CODEC)
         .xmap(PlayerAdvancements.Data::new, PlayerAdvancements.Data::map);

      public void forEach(final BiConsumer<Identifier, AdvancementProgress> consumer) {
         this.map.entrySet().stream().sorted(Entry.comparingByValue()).forEach(entry -> consumer.accept(entry.getKey(), entry.getValue()));
      }
   }

   public record TriggerInstanceKey(AdvancementHolder advancement, String criterion) {
   }
}
