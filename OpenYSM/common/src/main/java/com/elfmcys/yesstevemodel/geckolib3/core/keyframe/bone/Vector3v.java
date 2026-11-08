package com.elfmcys.yesstevemodel.geckolib3.core.keyframe.bone;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.FloatValue;
import com.elfmcys.yesstevemodel.molang.runtime.ExpressionEvaluator;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

public class Vector3v {
    private static final ThreadLocal<Map<ConstantKey, Vector3v>> CONSTANT_POOL = new ThreadLocal<>();

    private final float x;
    private final float y;
    private final float z;
    private final IValue[] expressions;

    public Vector3v(IValue x, IValue y, IValue z) {
        this(0.0f, 0.0f, 0.0f, x, y, z);
    }

    public Vector3v(float x, float y, float z) {
        this(x, y, z, null, null, null);
    }

    public static void beginConstantPooling() {
        CONSTANT_POOL.set(new HashMap<>());
    }

    public static void endConstantPooling() {
        CONSTANT_POOL.remove();
    }

    public static Vector3v constant(float x, float y, float z) {
        Map<ConstantKey, Vector3v> pool = CONSTANT_POOL.get();
        if (pool == null) return new Vector3v(x, y, z);
        ConstantKey key = new ConstantKey(Float.floatToIntBits(x), Float.floatToIntBits(y), Float.floatToIntBits(z));
        return pool.computeIfAbsent(key, ignored -> new Vector3v(x, y, z));
    }

    public Vector3v(float x, float y, float z, IValue xExpression, IValue yExpression, IValue zExpression) {
        this.x = xExpression instanceof FloatValue value ? value.value() : x;
        this.y = yExpression instanceof FloatValue value ? value.value() : y;
        this.z = zExpression instanceof FloatValue value ? value.value() : z;

        IValue resolvedX = xExpression instanceof FloatValue ? null : xExpression;
        IValue resolvedY = yExpression instanceof FloatValue ? null : yExpression;
        IValue resolvedZ = zExpression instanceof FloatValue ? null : zExpression;
        this.expressions = resolvedX == null && resolvedY == null && resolvedZ == null ? null : new IValue[]{resolvedX, resolvedY, resolvedZ};

    }

    public Vector3f eval(ExpressionEvaluator<?> evaluator) {
        return eval(evaluator, new Vector3f());
    }

    public Vector3f eval(ExpressionEvaluator<?> evaluator, Vector3f target) {
        if (expressions == null) {
            return target.set(x, y, z);
        }
        return target.set(
                expressions[0] == null ? x : expressions[0].evalAsFloat(evaluator),
                expressions[1] == null ? y : expressions[1].evalAsFloat(evaluator),
                expressions[2] == null ? z : expressions[2].evalAsFloat(evaluator)
        );
    }

    private record ConstantKey(int x, int y, int z) {
    }
}
