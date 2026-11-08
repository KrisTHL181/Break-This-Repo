package rip.ysm.compat.touhoulittlemaid.forge;

import com.elfmcys.yesstevemodel.network.message.FeedbackData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.TouhouMaidCompat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public final class TouhouMaidCompatImpl {

    private TouhouMaidCompatImpl() {
    }

    public static boolean isLoaded() {
        return TouhouMaidCompat.isLoaded();
    }

    public static void init() {
        TouhouMaidCompat.init();
    }

    public static boolean isMaidEntity(Entity entity) {
        return TouhouMaidCompat.isMaidEntity(entity);
    }

    public static void handleProjectileOwner(Projectile projectile, Entity entity) {
        TouhouMaidCompat.handleProjectileOwner(projectile, entity);
    }

    public static void registerAnimationRoulette(Entity entity, String str, int i) {
        TouhouMaidCompat.registerAnimationRoulette(entity, str, i);
    }

    public static void applyFeedback(Entity entity, FeedbackData message) {
        TouhouMaidCompat.applyFeedback(entity, message);
    }

    @OnlyIn(Dist.CLIENT)
    public static void playMaidAnimation(Entity entity, String str) {
        TouhouMaidCompat.playMaidAnimation(entity, str);
    }
}
