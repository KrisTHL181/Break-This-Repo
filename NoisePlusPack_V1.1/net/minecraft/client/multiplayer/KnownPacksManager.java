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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class KnownPacksManager {
   private final PackRepository repository = ServerPacksSource.createVanillaTrustedRepository();
   private final Map<KnownPack, String> knownPackToId;

   public KnownPacksManager() {
      this.repository.reload();
      Builder<KnownPack, String> builder = ImmutableMap.builder();
      this.repository.getAvailablePacks().forEach(p_334709_ -> {
         PackLocationInfo packlocationinfo = p_334709_.location();
         packlocationinfo.knownPackInfo().ifPresent(p_333246_ -> builder.put(p_333246_, packlocationinfo.id()));
      });
      this.knownPackToId = builder.build();
   }

   public List<KnownPack> trySelectingPacks(List<KnownPack> p_332560_) {
      List<KnownPack> list = new ArrayList<>(p_332560_.size());
      List<String> list1 = new ArrayList<>(p_332560_.size());

      for (KnownPack knownpack : p_332560_) {
         String s = this.knownPackToId.get(knownpack);
         if (s != null) {
            list1.add(s);
            list.add(knownpack);
         }
      }

      this.repository.setSelected(list1);
      return list;
   }

   public CloseableResourceManager createResourceManager() {
      List<PackResources> list = this.repository.openAllSelected();
      return new MultiPackResourceManager(PackType.SERVER_DATA, list);
   }
}
