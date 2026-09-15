package net.minecraft.client.telemetry;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface TelemetryEventLogger {
   void log(TelemetryEventInstance var1);
}
