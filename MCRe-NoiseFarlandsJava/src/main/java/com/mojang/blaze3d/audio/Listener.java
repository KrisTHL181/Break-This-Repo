package com.mojang.blaze3d.audio;

import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.openal.AL10;

@OnlyIn(Dist.CLIENT)
public class Listener {
    private ListenerTransform transform = ListenerTransform.INITIAL;

    public void setTransform(final ListenerTransform transform) {
        // 强制将监听器位置固定在原点，避免大坐标下浮点溢出
        ListenerTransform fixedTransform = new ListenerTransform(Vec3.ZERO, transform.forward(), transform.up());
        this.transform = fixedTransform;
        Vec3 position = fixedTransform.position();
        Vec3 forward = fixedTransform.forward();
        Vec3 up = fixedTransform.up();
        AL10.alListener3f(4100, (float)position.x, (float)position.y, (float)position.z);
        AL10.alListenerfv(4111, new float[]{(float)forward.x, (float)forward.y, (float)forward.z, (float)up.x(), (float)up.y(), (float)up.z()});
    }

    public void reset() {
        this.setTransform(ListenerTransform.INITIAL);
    }

    public ListenerTransform getTransform() {
        return this.transform;
    }
}