package com.elfmcys.yesstevemodel.geckolib3.core.keyframe;

import com.elfmcys.yesstevemodel.geckolib3.core.controller.AnimationControllerContext;
import com.elfmcys.yesstevemodel.geckolib3.core.util.MathUtil;
import com.elfmcys.yesstevemodel.geckolib3.core.keyframe.bone.TransitionKeyFrame;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.AnimationContext;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import org.joml.Vector3f;

public class TransitionPoint extends AnimationPoint {
    private static final ThreadLocal<Vector3f> RAW_VALUE = ThreadLocal.withInitial(Vector3f::new);

    public final float lerpFactor;

    public final Vector3f offsetPoint;

    public final TransitionKeyFrame dstKeyframe;

    public TransitionPoint(float currentTick, float lerpFactor, float totalTick, Vector3f offsetPoint, TransitionKeyFrame dstKeyframe, AnimationControllerContext context) {
        super(currentTick, totalTick, context);
        this.lerpFactor = lerpFactor;
        this.offsetPoint = offsetPoint;
        this.dstKeyframe = dstKeyframe;
    }

    @Override
    public Vector3f getLerpPoint(ExpressionEvaluator<AnimationContext<?>> evaluator) {
        setupControllerContext(evaluator);
        if (this.cachedValue == null) {
            this.cachedValue = new Vector3f();
        }
        this.dstKeyframe.evaluate(evaluator, this.cachedValue);
        MathUtil.lerpValues(this.lerpFactor, this.offsetPoint, this.cachedValue, this.cachedValue);
        return this.cachedValue;
    }

    public Vector3f evaluateRaw(ExpressionEvaluator<AnimationContext<?>> evaluator) {
        setupControllerContext(evaluator);
        return this.dstKeyframe.evaluate(evaluator, RAW_VALUE.get());
    }

    public Vector3f getOffsetPoint() {
        return this.offsetPoint;
    }

    public float getLerpFactor() {
        return this.lerpFactor;
    }
}
