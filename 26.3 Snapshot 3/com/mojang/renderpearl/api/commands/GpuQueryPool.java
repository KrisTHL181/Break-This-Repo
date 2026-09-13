package com.mojang.renderpearl.api.commands;

import java.util.OptionalLong;

public interface GpuQueryPool extends AutoCloseable {
   int size();

   OptionalLong getValue(int index);

   OptionalLong[] getValues(int index, int count);

   @Override
   void close();
}
