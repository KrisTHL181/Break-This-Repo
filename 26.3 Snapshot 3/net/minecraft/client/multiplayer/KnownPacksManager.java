package net.minecraft.client.multiplayer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.MultiPackResourceManager;

public class KnownPacksManager {
   private final PackRepository repository = ServerPacksSource.createVanillaTrustedRepository();
   private final Map<KnownPack, String> knownPackToId;

   public KnownPacksManager() {
      this.repository.reload();
      Builder<KnownPack, String> knownPacks = ImmutableMap.builder();
      this.repository.getAvailablePacks().forEach(pack -> {
         PackLocationInfo location = pack.location();
         location.knownPackInfo().ifPresent(knownPack -> knownPacks.put(knownPack, location.id()));
      });
      this.knownPackToId = knownPacks.build();
   }

   public List<KnownPack> trySelectingPacks(final List<KnownPack> packsToSelect) {
      List<KnownPack> response = new ArrayList<>(packsToSelect.size());
      List<String> selectedPacks = new ArrayList<>(packsToSelect.size());

      for (KnownPack knownPack : packsToSelect) {
         String knownPackId = this.knownPackToId.get(knownPack);
         if (knownPackId != null) {
            selectedPacks.add(knownPackId);
            response.add(knownPack);
         }
      }

      this.repository.setSelected(selectedPacks);
      return response;
   }

   public CloseableResourceManager createResourceManager() {
      List<PackResources> openedPacks = this.repository.openAllSelected();
      return new MultiPackResourceManager(PackType.SERVER_DATA, openedPacks);
   }
}
