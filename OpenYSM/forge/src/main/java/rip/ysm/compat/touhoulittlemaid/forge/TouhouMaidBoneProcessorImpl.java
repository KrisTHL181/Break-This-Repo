package rip.ysm.compat.touhoulittlemaid.forge;

import com.elfmcys.yesstevemodel.geckolib3.geo.animated.AnimatedGeoBone;
import com.elfmcys.yesstevemodel.geckolib3.geo.animated.AnimatedGeoModel;
import com.elfmcys.yesstevemodel.client.compat.touhoulittlemaid.TouhouMaidBoneProcessor;

public final class TouhouMaidBoneProcessorImpl {

    private TouhouMaidBoneProcessorImpl() {
    }

    public static Object createLocationBone(AnimatedGeoBone bone) {
        return TouhouMaidBoneProcessor.createLocationBone(bone);
    }

    public static Object createLocationModel(AnimatedGeoModel model) {
        return TouhouMaidBoneProcessor.createLocationModel(model);
    }
}
