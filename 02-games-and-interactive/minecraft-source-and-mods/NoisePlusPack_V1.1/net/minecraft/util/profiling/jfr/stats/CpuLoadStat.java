package net.minecraft.util.profiling.jfr.stats;

import jdk.jfr.consumer.RecordedEvent;

public record CpuLoadStat(double jvm, double userJvm, double system) {
   public static CpuLoadStat from(RecordedEvent p_185623_) {
      return new CpuLoadStat(p_185623_.getFloat("jvmSystem"), p_185623_.getFloat("jvmUser"), p_185623_.getFloat("machineTotal"));
   }
}
