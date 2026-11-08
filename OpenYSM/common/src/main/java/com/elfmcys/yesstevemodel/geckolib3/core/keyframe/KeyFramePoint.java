package com.elfmcys.yesstevemodel.geckolib3.core.keyframe;

import com.elfmcys.yesstevemodel.geckolib3.core.controller.AnimationControllerContext;
import com.elfmcys.yesstevemodel.geckolib3.core.keyframe.bone.BoneKeyFrame;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.context.AnimationContext;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import org.joml.Vector3f;

public class KeyFramePoint extends AnimationPoint {

    public BoneKeyFrame keyFrame;

    public KeyFramePoint(float currentTick, BoneKeyFrame keyFrame, AnimationControllerContext context) {
        super(currentTick, keyFrame.getTotalTick(), context);
        this.keyFrame = keyFrame;
    }

    public KeyFramePoint reset(float currentTick, BoneKeyFrame keyFrame) {
        this.currentTick = currentTick;
        this.totalTick = keyFrame.getTotalTick();
        this.keyFrame = keyFrame;
        return this;
    }

    @Override
    public Vector3f getLerpPoint(ExpressionEvaluator<AnimationContext<?>> evaluator) {
        setupControllerContext(evaluator);
        if (this.cachedValue == null) {
            this.cachedValue = new Vector3f();
        }
        return this.keyFrame.evaluate(evaluator, getPercentCompleted(), this.cachedValue);
    }
}
