package com.elfmcys.yesstevemodel.geckolib3.core.keyframe.bone;

import com.elfmcys.yesstevemodel.geckolib3.core.util.MathUtil;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import org.joml.Vector3f;

public class CatmullRomKeyFrame extends BoneKeyFrame {

    private final Vector3v leftPoint;

    private final Vector3v endPoint;

    private final Vector3v rightPoint;

    private final Vector3v postPoint;

    public CatmullRomKeyFrame(float startTick, float totalTick, Vector3v leftPoint, Vector3v current, Vector3v endPoint, Vector3v postRight, Vector3v postPoint) {
        super(startTick, totalTick, current);
        this.leftPoint = leftPoint;
        this.endPoint = endPoint;
        this.rightPoint = postRight;
        this.postPoint = postPoint;
    }

    @Override
    public Vector3f evaluate(ExpressionEvaluator<?> evaluator, float percentCompleted, Vector3f target) {
        if (isBegin(percentCompleted)) {
            return this.beginPoint.eval(evaluator, target);
        }
        if (isEnd(percentCompleted)) {
            return this.postPoint.eval(evaluator, target);
        }
        Vector3f[] scratch = EvaluationScratch.CATMULL.get();
        Vector3f left = this.leftPoint.eval(evaluator, scratch[0]);
        Vector3f begin = this.beginPoint.eval(evaluator, scratch[1]);
        Vector3f end = this.endPoint.eval(evaluator, scratch[2]);
        Vector3f right = this.rightPoint.eval(evaluator, scratch[3]);
        target.set(
                MathUtil.catmullRom(percentCompleted, left.x, begin.x, end.x, right.x),
                MathUtil.catmullRom(percentCompleted, left.y, begin.y, end.y, right.y),
                MathUtil.catmullRom(percentCompleted, left.z, begin.z, end.z, right.z)
        );
        return target;
    }
}
