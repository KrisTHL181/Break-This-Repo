package net.minecraft.client.multiplayer;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.telemetry.WorldSessionTelemetryManager;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ClientAdvancements {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Minecraft minecraft;
   private final WorldSessionTelemetryManager telemetryManager;
   private final AdvancementTree tree = new AdvancementTree();
   private final Map<AdvancementHolder, AdvancementProgress> progress = new Object2ObjectOpenHashMap();
   private ClientAdvancements.@Nullable Listener listener;
   private @Nullable AdvancementHolder selectedTab;

   public ClientAdvancements(Minecraft p_286782_, WorldSessionTelemetryManager p_286391_) {
      this.minecraft = p_286782_;
      this.telemetryManager = p_286391_;
   }

   public void update(ClientboundUpdateAdvancementsPacket p_104400_) {
      if (p_104400_.shouldReset()) {
         this.tree.clear();
         this.progress.clear();
      }

      this.tree.remove(p_104400_.getRemoved());
      this.tree.addAll(p_104400_.getAdded());

      for (Entry<Identifier, AdvancementProgress> entry : p_104400_.getProgress().entrySet()) {
         AdvancementNode advancementnode = this.tree.get(entry.getKey());
         if (advancementnode != null) {
            AdvancementProgress advancementprogress = entry.getValue();
            advancementprogress.update(advancementnode.advancement().requirements());
            this.progress.put(advancementnode.holder(), advancementprogress);
            if (this.listener != null) {
               this.listener.onUpdateAdvancementProgress(advancementnode, advancementprogress);
            }

            if (!p_104400_.shouldReset() && advancementprogress.isDone()) {
               if (this.minecraft.level != null) {
                  this.telemetryManager.onAdvancementDone(this.minecraft.level, advancementnode.holder());
               }

               Optional<DisplayInfo> optional = advancementnode.advancement().display();
               if (p_104400_.shouldShowAdvancements() && optional.isPresent() && optional.get().shouldShowToast()) {
                  this.minecraft.getToastManager().addToast(new AdvancementToast(advancementnode.holder()));
               }
            }
         } else {
            LOGGER.warn("Server informed client about progress for unknown advancement {}", entry.getKey());
         }
      }
   }

   public AdvancementTree getTree() {
      return this.tree;
   }

   public void setSelectedTab(@Nullable AdvancementHolder p_298261_, boolean p_104403_) {
      ClientPacketListener clientpacketlistener = this.minecraft.getConnection();
      if (clientpacketlistener != null && p_298261_ != null && p_104403_) {
         clientpacketlistener.send(ServerboundSeenAdvancementsPacket.openedTab(p_298261_));
      }

      if (this.selectedTab != p_298261_) {
         this.selectedTab = p_298261_;
         if (this.listener != null) {
            this.listener.onSelectedTabChanged(p_298261_);
         }
      }
   }

   public void setListener(ClientAdvancements.@Nullable Listener p_104398_) {
      this.listener = p_104398_;
      this.tree.setListener(p_104398_);
      if (p_104398_ != null) {
         this.progress.forEach((p_297914_, p_300709_) -> {
            AdvancementNode advancementnode = this.tree.get(p_297914_);
            if (advancementnode != null) {
               p_104398_.onUpdateAdvancementProgress(advancementnode, p_300709_);
            }
         });
         p_104398_.onSelectedTabChanged(this.selectedTab);
      }
   }

   public @Nullable AdvancementHolder get(Identifier p_454866_) {
      AdvancementNode advancementnode = this.tree.get(p_454866_);
      return advancementnode != null ? advancementnode.holder() : null;
   }

   @OnlyIn(Dist.CLIENT)
   public interface Listener extends AdvancementTree.Listener {
      void onUpdateAdvancementProgress(AdvancementNode var1, AdvancementProgress var2);

      void onSelectedTabChanged(@Nullable AdvancementHolder var1);
   }
}
