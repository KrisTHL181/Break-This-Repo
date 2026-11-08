package com.elfmcys.yesstevemodel.geckolib3.core.keyframe.bone;

import org.joml.Vector3f;

final class EvaluationScratch {
    static final ThreadLocal<Vector3f> SECOND = ThreadLocal.withInitial(Vector3f::new);
    static final ThreadLocal<Vector3f[]> CATMULL = ThreadLocal.withInitial(() -> new Vector3f[]{new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()});

    private EvaluationScratch() {
    }
}
