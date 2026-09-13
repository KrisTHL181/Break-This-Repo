package net.minecraft.world.level.pathfinder;

import java.util.Arrays;

public class BinaryHeap {
   private Node[] heap = new Node[128];
   private int size;

   public Node insert(final Node node) {
      if (node.heapIdx >= 0) {
         throw new IllegalStateException("OW KNOWS!");
      }

      if (this.size == this.heap.length) {
         Node[] newHeap = new Node[this.size << 1];
         System.arraycopy(this.heap, 0, newHeap, 0, this.size);
         this.heap = newHeap;
      }

      this.heap[this.size] = node;
      node.heapIdx = this.size;
      this.upHeap(this.size++);
      return node;
   }

   public void clear() {
      this.size = 0;
   }

   public Node peek() {
      return this.heap[0];
   }

   public Node pop() {
      Node popped = this.heap[0];
      this.heap[0] = this.heap[--this.size];
      this.heap[this.size] = null;
      if (this.size > 0) {
         this.downHeap(0);
      }

      popped.heapIdx = -1;
      return popped;
   }

   public void remove(final Node node) {
      this.heap[node.heapIdx] = this.heap[--this.size];
      this.heap[this.size] = null;
      if (this.size > node.heapIdx) {
         if (this.heap[node.heapIdx].f < node.f) {
            this.upHeap(node.heapIdx);
         } else {
            this.downHeap(node.heapIdx);
         }
      }

      node.heapIdx = -1;
   }

   public void changeCost(final Node node, final float newCost) {
      float oldCost = node.f;
      node.f = newCost;
      if (newCost < oldCost) {
         this.upHeap(node.heapIdx);
      } else {
         this.downHeap(node.heapIdx);
      }
   }

   public int size() {
      return this.size;
   }

   private void upHeap(int idx) {
      Node node = this.heap[idx];
      float cost = node.f;

      while (idx > 0) {
         int parentIdx = idx - 1 >> 1;
         Node parent = this.heap[parentIdx];
         if (!(cost < parent.f)) {
            break;
         }

         this.heap[idx] = parent;
         parent.heapIdx = idx;
         idx = parentIdx;
      }

      this.heap[idx] = node;
      node.heapIdx = idx;
   }

   private void downHeap(int idx) {
      Node node = this.heap[idx];
      float cost = node.f;

      while (true) {
         int leftIdx = 1 + (idx << 1);
         int rightIdx = leftIdx + 1;
         if (leftIdx >= this.size) {
            break;
         }

         Node leftNode = this.heap[leftIdx];
         float leftCost = leftNode.f;
         Node rightNode;
         float rightCost;
         if (rightIdx >= this.size) {
            rightNode = null;
            rightCost = Float.POSITIVE_INFINITY;
         } else {
            rightNode = this.heap[rightIdx];
            rightCost = rightNode.f;
         }

         if (leftCost < rightCost) {
            if (!(leftCost < cost)) {
               break;
            }

            this.heap[idx] = leftNode;
            leftNode.heapIdx = idx;
            idx = leftIdx;
         } else {
            if (!(rightCost < cost)) {
               break;
            }

            this.heap[idx] = rightNode;
            rightNode.heapIdx = idx;
            idx = rightIdx;
         }
      }

      this.heap[idx] = node;
      node.heapIdx = idx;
   }

   public boolean isEmpty() {
      return this.size == 0;
   }

   public Node[] getHeap() {
      return Arrays.copyOf(this.heap, this.size);
   }
}
