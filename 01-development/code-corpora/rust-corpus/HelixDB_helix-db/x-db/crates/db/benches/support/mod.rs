use std::alloc::{GlobalAlloc, Layout, System};
use std::sync::atomic::{AtomicBool, AtomicU64, Ordering};

pub struct TrackingAllocator {
    enabled: AtomicBool,
    allocations: AtomicU64,
    allocated_bytes: AtomicU64,
}

impl TrackingAllocator {
    pub const fn new() -> Self {
        Self {
            enabled: AtomicBool::new(false),
            allocations: AtomicU64::new(0),
            allocated_bytes: AtomicU64::new(0),
        }
    }

    pub fn start(&self) {
        self.allocations.store(0, Ordering::Relaxed);
        self.allocated_bytes.store(0, Ordering::Relaxed);
        self.enabled.store(true, Ordering::Release);
    }

    pub fn finish(&self) -> (u64, u64) {
        self.enabled.store(false, Ordering::Release);
        (
            self.allocations.load(Ordering::Relaxed),
            self.allocated_bytes.load(Ordering::Relaxed),
        )
    }
}

// SAFETY: every allocation and deallocation delegates unchanged layouts and
// pointers to the system allocator. Relaxed counters are observational only.
unsafe impl GlobalAlloc for TrackingAllocator {
    unsafe fn alloc(&self, layout: Layout) -> *mut u8 {
        if self.enabled.load(Ordering::Acquire) {
            self.allocations.fetch_add(1, Ordering::Relaxed);
            self.allocated_bytes
                .fetch_add(layout.size() as u64, Ordering::Relaxed);
        }
        // SAFETY: this method preserves the caller-provided layout contract.
        unsafe { System.alloc(layout) }
    }

    unsafe fn dealloc(&self, pointer: *mut u8, layout: Layout) {
        // SAFETY: the pointer and layout came from the delegated system allocation.
        unsafe { System.dealloc(pointer, layout) }
    }

    unsafe fn alloc_zeroed(&self, layout: Layout) -> *mut u8 {
        if self.enabled.load(Ordering::Acquire) {
            self.allocations.fetch_add(1, Ordering::Relaxed);
            self.allocated_bytes
                .fetch_add(layout.size() as u64, Ordering::Relaxed);
        }
        // SAFETY: this method preserves the caller-provided layout contract.
        unsafe { System.alloc_zeroed(layout) }
    }

    unsafe fn realloc(&self, pointer: *mut u8, layout: Layout, new_size: usize) -> *mut u8 {
        if self.enabled.load(Ordering::Acquire) {
            self.allocations.fetch_add(1, Ordering::Relaxed);
            self.allocated_bytes
                .fetch_add(new_size as u64, Ordering::Relaxed);
        }
        // SAFETY: this method preserves the delegated reallocation contract.
        unsafe { System.realloc(pointer, layout, new_size) }
    }
}
