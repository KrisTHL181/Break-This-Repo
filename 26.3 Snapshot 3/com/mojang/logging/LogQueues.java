package com.mojang.logging;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.jspecify.annotations.Nullable;

public class LogQueues {
   private static final Map<String, BlockingQueue<String>> QUEUES = new HashMap<>();
   private static final ReentrantReadWriteLock QUEUE_LOCK = new ReentrantReadWriteLock();

   public static BlockingQueue<String> getOrCreateQueue(String target) {
      try {
         QUEUE_LOCK.readLock().lock();
         BlockingQueue<String> queue = QUEUES.get(target);
         if (queue != null) {
            return queue;
         }
      } finally {
         QUEUE_LOCK.readLock().unlock();
      }

      try {
         QUEUE_LOCK.writeLock().lock();
         return QUEUES.computeIfAbsent(target, k -> new LinkedBlockingQueue<>());
      } finally {
         QUEUE_LOCK.writeLock().unlock();
      }
   }

   public static @Nullable String getNextLogEvent(String queueName) {
      QUEUE_LOCK.readLock().lock();
      BlockingQueue<String> queue = QUEUES.get(queueName);
      QUEUE_LOCK.readLock().unlock();
      if (queue != null) {
         try {
            return queue.take();
         } catch (InterruptedException var3) {
         }
      }

      return null;
   }
}
