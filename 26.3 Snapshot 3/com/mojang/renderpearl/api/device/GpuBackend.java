package com.mojang.renderpearl.api.device;

import com.mojang.blaze3d.GLFWErrorCapture;

public interface GpuBackend {
   String getName();

   void setWindowHints();

   void handleWindowCreationErrors(final GLFWErrorCapture.Error error) throws BackendCreationException;

   GpuDevice createDevice(long window, GpuDebugOptions debugOptions) throws BackendCreationException;
}
