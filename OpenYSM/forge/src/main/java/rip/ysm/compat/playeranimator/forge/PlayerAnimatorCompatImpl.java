package rip.ysm.compat.playeranimator.forge;

import net.minecraft.client.player.AbstractClientPlayer;
import com.elfmcys.yesstevemodel.client.compat.playeranimator.PlayerAnimatorCompat;

public final class PlayerAnimatorCompatImpl {

    private PlayerAnimatorCompatImpl() {
    }

    public static boolean isLoaded() {
        return PlayerAnimatorCompat.isLoaded();
    }

    public static boolean isPlayerAnimated(AbstractClientPlayer abstractClientPlayer) {
        return PlayerAnimatorCompat.isPlayerAnimated(abstractClientPlayer);
    }
}
