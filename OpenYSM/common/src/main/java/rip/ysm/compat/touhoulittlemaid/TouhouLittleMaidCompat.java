package rip.ysm.compat.touhoulittlemaid;

import com.elfmcys.yesstevemodel.client.animation.molang.TLMBinding;
import com.elfmcys.yesstevemodel.client.entity.LivingAnimatable;
import com.elfmcys.yesstevemodel.client.model.ModelResourceBundle;
import com.elfmcys.yesstevemodel.client.model.PlayerModelBundle;
import com.elfmcys.yesstevemodel.geckolib3.core.enums.PlayState;
import com.elfmcys.yesstevemodel.geckolib3.core.event.predicate.AnimationEvent;
import com.elfmcys.yesstevemodel.geckolib3.geo.GeoReplacedEntityRenderer;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

public final class TouhouLittleMaidCompat {

    private TouhouLittleMaidCompat() {
    }

    @ExpectPlatform
    public static boolean isLoaded() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean isMaidEntity(Entity entity) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean isMaidRideable(Entity entity) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean isSimplePlanesEntity(Entity entity) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean isImmersiveAircraftEntity(Entity entity) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean isMaidItem(Item item) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static String getMaidEntityId(Entity entity) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean isMaidSitting(LivingEntity livingEntity) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void registerMaidAnimStates(TLMBinding tlmBinding) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static PlayState handleMaidInteraction(AnimationEvent<LivingAnimatable<?>> event, LivingEntity livingEntity, Entity entity) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean isMaidChatAvailable() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void openMaidChat() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static Object buildControllers(PlayerModelBundle modelBundle, ModelResourceBundle resourceBundle) {
        throw new AssertionError();
    }

    @ExpectPlatform
    @Nullable
    public static GeoReplacedEntityRenderer<?, ?> getMaidPreviewRenderer(LivingAnimatable<?> animatable) {
        throw new AssertionError();
    }
}
