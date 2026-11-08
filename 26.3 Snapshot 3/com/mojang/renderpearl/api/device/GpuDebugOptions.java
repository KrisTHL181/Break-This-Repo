package com.mojang.renderpearl.api.device;

public record GpuDebugOptions(int logLevel, boolean synchronousLogs, boolean useLabels, boolean useValidationLayers) {
}
