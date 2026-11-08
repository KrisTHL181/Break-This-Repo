package com.elfmcys.yesstevemodel.geckolib3.core.keyframe.bone;

import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.IValue;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.value.RotationValue;

@SuppressWarnings("FieldMayBeFinal,unused")
public class RawBoneKeyFrame {

    public double startTick;

    public EasingType easingType;

    public double preX;
    public IValue preXValue;
    public double preY;
    public IValue preYValue;
    public double preZ;
    public IValue preZValue;

    public double postX;
    public IValue postXValue;
    public double postY;
    public IValue postYValue;
    public double postZ;
    public IValue postZValue;

    public boolean contiguous = true;

    public Vector3v preValue;
    public Vector3v postValue;

    private Vector3v createVector(boolean post, boolean isRotation) {
        float x = (float) (post ? postX : preX);
        float y = (float) (post ? postY : preY);
        float z = (float) (post ? postZ : preZ);
        IValue xValue = post ? postXValue : preXValue;
        IValue yValue = post ? postYValue : preYValue;
        IValue zValue = post ? postZValue : preZValue;

        if (isRotation) {
            x = RotationValue.convert(x, true);
            y = RotationValue.convert(y, true);
            z = RotationValue.convert(z, false);
            if (xValue != null) xValue = new RotationValue(xValue, true);
            if (yValue != null) yValue = new RotationValue(yValue, true);
            if (zValue != null) zValue = new RotationValue(zValue, false);
        }

        if (xValue == null && yValue == null && zValue == null) {
            return Vector3v.constant(x, y, z);
        }
        return new Vector3v(x, y, z, xValue, yValue, zValue);
    }

    public void init(boolean isRotation) {
        if (this.preValue != null) {
            return;
        }
        this.preValue = createVector(false, isRotation);
        if (this.contiguous) {
            this.postValue = this.preValue;
        } else {
            this.postValue = createVector(true, isRotation);
        }

        if (easingType == null) easingType = EasingType.LINEAR;
    }

    public float startTick() {
        return (float) this.startTick;
    }

    public EasingType easingType() {
        return this.easingType;
    }

    public Vector3v preValue() {
        return this.preValue;
    }

    public Vector3v postValue() {
        return this.postValue;
    }
}
