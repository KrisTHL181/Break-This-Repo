use std::thread::JoinHandle;

#[cfg(target_os = "macos")]
pub fn spawn_with_pool<F, T>(f: F) -> JoinHandle<T>
where
    F: FnOnce() -> T + Send + 'static,
    T: Send + 'static,
{
    std::thread::spawn(move || unsafe {
        use cocoa::foundation::NSAutoreleasePool;
        let pool = NSAutoreleasePool::new(cocoa::base::nil);
        let result = f();
        pool.drain();
        result
    })
}

#[cfg(not(target_os = "macos"))]
pub fn spawn_with_pool<F, T>(f: F) -> JoinHandle<T>
where
    F: FnOnce() -> T + Send + 'static,
    T: Send + 'static,
{
    std::thread::spawn(f)
}
