package net.minecraft.client.renderer.fog;

import org.joml.Vector4f;

public class FogData {
   public float environmentalStart;
   public float renderDistanceStart;
   public float environmentalEnd;
   public float renderDistanceEnd;
   public float skyEnd;
   public float cloudEnd;
   public Vector4f color = new Vector4f();
}
