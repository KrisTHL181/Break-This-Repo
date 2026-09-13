package net.minecraft.server.dialog;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;

public class DialogTypes {
   public static MapCodec<? extends Dialog> bootstrap(Registry<MapCodec<? extends Dialog>> p_410710_) {
      Registry.register(p_410710_, "notice", NoticeDialog.MAP_CODEC);
      Registry.register(p_410710_, "server_links", ServerLinksDialog.MAP_CODEC);
      Registry.register(p_410710_, "dialog_list", DialogListDialog.MAP_CODEC);
      Registry.register(p_410710_, "multi_action", MultiActionDialog.MAP_CODEC);
      return Registry.register(p_410710_, "confirmation", ConfirmationDialog.MAP_CODEC);
   }
}
