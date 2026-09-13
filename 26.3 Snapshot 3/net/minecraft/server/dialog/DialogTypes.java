package net.minecraft.server.dialog;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;

public class DialogTypes {
   public static MapCodec<? extends Dialog> bootstrap(final Registry<MapCodec<? extends Dialog>> registry) {
      Registry.register(registry, "notice", NoticeDialog.MAP_CODEC);
      Registry.register(registry, "server_links", ServerLinksDialog.MAP_CODEC);
      Registry.register(registry, "dialog_list", DialogListDialog.MAP_CODEC);
      Registry.register(registry, "multi_action", MultiActionDialog.MAP_CODEC);
      return Registry.register(registry, "confirmation", ConfirmationDialog.MAP_CODEC);
   }
}
