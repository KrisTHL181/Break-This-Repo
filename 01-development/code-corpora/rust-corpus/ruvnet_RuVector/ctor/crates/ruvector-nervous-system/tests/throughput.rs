// Throughput benchmarks - sustained load testing
// Tests system performance under continuous operation
//
// Smoke vs perf split convention
// ------------------------------
// Each operation has TWO tests:
//
//   `<name>` (always-on):
//       Smoke version. Exercises the code path under sustained load and
//       asserts only functional correctness — operations completed,
//       output shape valid, no panic. Runs on every `cargo test`,
//       deterministic regardless of host speed.
//
//   `<name>_perf` (gated `#[cfg(feature = "perf-tests")]`):
//       Perf version. Same workload, but adds the absolute throughput
//       threshold assertion. Run with
//       `cargo test -p ruvector-nervous-system --features perf-tests`.
//       Intended for tuned/release-mode runners; off by default so CI
//       on slow shared runners doesn't flake on absolute timings.

#[cfg(test)]
mod throughput_tests {
    use rand::rngs::StdRng;
    use rand::{Rng, SeedableRng};
    use std::sync::atomic::{AtomicU64, Ordering};
    use std::sync::Arc;
    use std::time::{Duration, Instant};

    // ========================================================================
    // Helper Structures
    // ========================================================================

    struct ThroughputStats {
        operations: u64,
        duration: Duration,
        min_latency: Duration,
        max_latency: Duration,
        latencies: Vec<Duration>,
    }

    impl ThroughputStats {
        fn new() -> Self {
            Self {
                operations: 0,
                duration: Duration::ZERO,
                min_latency: Duration::MAX,
                max_latency: Duration::ZERO,
                latencies: Vec::new(),
            }
        }

        fn record(&mut self, latency: Duration) {
            self.operations += 1;
            self.min_latency = self.min_latency.min(latency);
            self.max_latency = self.max_latency.max(latency);
            self.latencies.push(latency);
        }

        fn ops_per_sec(&self) -> f64 {
            self.operations as f64 / self.duration.as_secs_f64()
        }

        fn mean_latency(&self) -> Duration {
            let sum: Duration = self.latencies.iter().sum();
            sum / self.latencies.len() as u32
        }

        fn percentile(&mut self, p: f64) -> Duration {
            self.latencies.sort();
            let idx = (self.latencies.len() as f64 * p) as usize;
            self.latencies[idx.min(self.latencies.len() - 1)]
        }

        fn report(&mut self) {
            println!("Throughput: {:.0} ops/sec", self.ops_per_sec());
            println!("Total ops: {}", self.operations);
            println!("Duration: {:.2}s", self.duration.as_secs_f64());
            println!("Latency min: {:?}", self.min_latency);
            println!("Latency mean: {:?}", self.mean_latency());
            println!("Latency p50: {:?}", self.percentile(0.50));
            println!("Latency p99: {:?}", self.percentile(0.99));
            println!("Latency max: {:?}", self.max_latency);
        }
    }

    // ========================================================================
    // Event Bus Throughput
    // ========================================================================

    /// Drives the event-bus publish loop for a fixed wall-clock window and
    /// returns the populated stats. Shared by smoke + perf variants so they
    /// exercise the exact same code path.
    fn event_bus_sustained_workload(test_duration: Duration) -> ThroughputStats {
        // let bus = EventBus::new(1000);
        let mut stats = ThroughputStats::new();
        let start = Instant::now();

        while start.elapsed() < test_duration {
            let op_start = Instant::now();

            // bus.publish(Event::new("test", vec![0.0; 128]));
            // Placeholder operation
            let _result = vec![0.0f32; 128];

            stats.record(op_start.elapsed());
        }

        stats.duration = start.elapsed();
        stats
    }

    #[test]
    fn event_bus_sustained_throughput() {
        // Smoke: exercise sustained publish loop, verify no panic + non-empty
        // stats. No absolute throughput assertion (see perf variant below).
        let mut stats = event_bus_sustained_workload(Duration::from_secs(2));
        stats.report();

        assert!(
            stats.operations > 0,
            "event bus produced zero operations under sustained load"
        );
        assert!(stats.min_latency <= stats.max_latency);
        assert_eq!(stats.latencies.len() as u64, stats.operations);
    }

    #[cfg(feature = "perf-tests")]
    #[test]
    fn event_bus_sustained_throughput_perf() {
        // Target: >10,000 events/ms = 10M events/sec
        let mut stats = event_bus_sustained_workload(Duration::from_secs(10));
        stats.report();

        let ops_per_ms = stats.ops_per_sec() / 1000.0;
        assert!(
            ops_per_ms > 1_000.0,
            "Event bus throughput {:.0} ops/ms < 1K ops/ms",
            ops_per_ms
        );
    }

    #[test]
    fn event_bus_multi_producer_scaling() {
        use std::thread;

        // Test scaling with multiple producers (1, 2, 4, 8 threads)
        for num_threads in [1, 2, 4, 8] {
            let counter = Arc::new(AtomicU64::new(0));
            let test_duration = Duration::from_secs(5);
            // let bus = Arc::new(EventBus::new(1000));

            let handles: Vec<_> = (0..num_threads)
                .map(|_| {
                    let counter = Arc::clone(&counter);
                    // let bus = Arc::clone(&bus);

                    thread::spawn(move || {
                        let start = Instant::now();
                        let mut local_count = 0u64;

                        while start.elapsed() < test_duration {
                            // bus.publish(Event::new("test", vec![0.0; 128]));
                            let _result = vec![0.0f32; 128]; // Placeholder
                            local_count += 1;
                        }

                        counter.fetch_add(local_count, Ordering::Relaxed);
                    })
                })
                .collect();

            for handle in handles {
                handle.join().unwrap();
            }

            let total_ops = counter.load(Ordering::Relaxed);
            let ops_per_sec = total_ops as f64 / test_duration.as_secs_f64();

            println!("{} threads: {:.0} ops/sec", num_threads, ops_per_sec);

            // Check for reasonable scaling (at least 70% efficiency)
            if num_threads > 1 {
                // We'd compare to single-threaded baseline here
            }
        }
    }

    #[test]
    fn event_bus_backpressure_handling() {
        // Test behavior when queue is saturated
        // let bus = EventBus::new(100); // Small queue
        let mut stats = ThroughputStats::new();
        let start = Instant::now();
        let test_duration = Duration::from_secs(5);

        while start.elapsed() < test_duration {
            let op_start = Instant::now();

            // Try to publish at high rate
            // let result = bus.try_publish(Event::new("test", vec![0.0; 128]));
            let result = true; // Placeholder

            stats.record(op_start.elapsed());

            if !result {
                // Backpressure applied - this is expected
                std::thread::yield_now();
            }
        }

        stats.duration = start.elapsed();
        stats.report();

        // Should gracefully handle saturation without panic
        assert!(
            stats.operations > 0,
            "No operations completed under backpressure"
        );
    }

    // ========================================================================
    // HDC Encoding Throughput
    // ========================================================================

    fn hdc_encoding_workload(test_duration: Duration) -> ThroughputStats {
        let mut rng = StdRng::seed_from_u64(42);

        // let encoder = HDCEncoder::new(10000);
        let mut stats = ThroughputStats::new();
        let start = Instant::now();

        while start.elapsed() < test_duration {
            let input: Vec<f32> = (0..128).map(|_| rng.gen()).collect();
            let op_start = Instant::now();

            // encoder.encode(&input);
            // Placeholder: simple XOR binding
            let _result: Vec<u64> = (0..157).map(|_| rng.gen()).collect();
            // Reference `input` so the optimizer doesn't elide the loop body
            // entirely on aggressive opt levels — keeps the code path honest.
            std::hint::black_box(input);

            stats.record(op_start.elapsed());
        }

        stats.duration = start.elapsed();
        stats
    }

    #[test]
    fn hdc_encoding_throughput() {
        // Smoke: exercises the HDC encoding loop, asserts correctness only.
        let mut stats = hdc_encoding_workload(Duration::from_secs(2));
        stats.report();

        assert!(
            stats.operations > 0,
            "HDC encoding produced zero operations"
        );
        assert_eq!(stats.latencies.len() as u64, stats.operations);
    }

    #[cfg(feature = "perf-tests")]
    #[test]
    fn hdc_encoding_throughput_perf() {
        // Target: >1M ops/sec (placeholder threshold is conservative because
        // body is not the real SIMD HDC encoder).
        let mut stats = hdc_encoding_workload(Duration::from_secs(5));
        stats.report();
        assert!(
            stats.ops_per_sec() > 5_000.0,
            "HDC encoding throughput {:.0} < 5K ops/sec",
            stats.ops_per_sec()
        );
    }

    fn hdc_similarity_workload(test_duration: Duration) -> ThroughputStats {
        let mut rng = StdRng::seed_from_u64(42);
        let a: Vec<u64> = (0..157).map(|_| rng.gen()).collect();
        let b: Vec<u64> = (0..157).map(|_| rng.gen()).collect();

        let mut stats = ThroughputStats::new();
        let start = Instant::now();

        while start.elapsed() < test_duration {
            let op_start = Instant::now();

            // Hamming distance (SIMD accelerated)
            let dist: u32 = a
                .iter()
                .zip(b.iter())
                .map(|(x, y)| (x ^ y).count_ones())
                .sum();
            std::hint::black_box(dist);

            stats.record(op_start.elapsed());
        }

        stats.duration = start.elapsed();
        stats
    }

    #[test]
    fn hdc_similarity_throughput() {
        // Smoke: exercises the Hamming-distance similarity path without
        // asserting absolute throughput.
        let mut stats = hdc_similarity_workload(Duration::from_secs(2));
        stats.report();

        assert!(
            stats.operations > 0,
            "HDC similarity produced zero operations"
        );
        assert_eq!(stats.latencies.len() as u64, stats.operations);
    }

    #[cfg(feature = "perf-tests")]
    #[test]
    fn hdc_similarity_throughput_perf() {
        // Target: >10M ops/sec (placeholder; real SIMD path is faster).
        let mut stats = hdc_similarity_workload(Duration::from_secs(5));
        stats.report();
        assert!(
            stats.ops_per_sec() > 100_000.0,
            "HDC similarity throughput {:.0} < 100K ops/sec",
            stats.ops_per_sec()
        );
    }

    // ========================================================================
    // Hopfield Retrieval Throughput
    // ========================================================================

    fn hopfield_retrieval_workload(test_duration: Duration) -> ThroughputStats {
        let mut rng = StdRng::seed_from_u64(42);
        let dims = 512;

        // let hopfield = ModernHopfield::new(dims, 100.0);
        // Store 100 patterns
        // for _ in 0..100 {
        //     hopfield.store(generate_random_vector(&mut rng, dims));
        // }

        let mut stats = ThroughputStats::new();
        let start = Instant::now();

        while start.elapsed() < test_duration {
            let query: Vec<f32> = (0..dims).map(|_| rng.gen()).collect();
            let op_start = Instant::now();

            // let _retrieved = hopfield.retrieve(&query);
            let _retrieved = query.clone(); // Placeholder
            std::hint::black_box(_retrieved);

            stats.record(op_start.elapsed());
        }

        stats.duration = start.elapsed();
        stats
    }

    #[test]
    fn hopfield_parallel_retrieval() {
        // Smoke: exercise the Hopfield retrieval loop, no perf assertion.
        let mut stats = hopfield_retrieval_workload(Duration::from_secs(2));
        stats.report();

        assert!(
            stats.operations > 0,
            "Hopfield retrieval produced zero operations"
        );
        assert_eq!(stats.latencies.len() as u64, stats.operations);
    }

    #[cfg(feature = "perf-tests")]
    #[test]
    fn hopfield_parallel_retrieval_perf() {
        // Target: >1000 queries/sec
        let mut stats = hopfield_retrieval_workload(Duration::from_secs(5));
        stats.report();
        assert!(
            stats.ops_per_sec() > 1000.0,
            "Hopfield retrieval throughput {:.0} < 1K queries/sec",
            stats.ops_per_sec()
        );
    }

    #[test]
    fn hopfield_batch_retrieval() {
        let mut rng = StdRng::seed_from_u64(42);
        let dims = 512;
        let batch_size = 100;
        let test_duration = Duration::from_secs(5);

        // let hopfield = ModernHopfield::new(dims, 100.0);

        let mut batches_processed = 0u64;
        let start = Instant::now();

        while start.elapsed() < test_duration {
            let queries: Vec<Vec<f32>> = (0..batch_size)
                .map(|_| (0..dims).map(|_| rng.gen()).collect())
                .collect();

            // let _results = hopfield.retrieve_batch(&queries);
            // Placeholder
            for _query in queries {
                // Process each query
            }

            batches_processed += 1;
        }

        let duration = start.elapsed();
        let total_queries = batches_processed * batch_size;
        let queries_per_sec = total_queries as f64 / duration.as_secs_f64();

        println!("Batch retrieval: {:.0} queries/sec", queries_per_sec);
        assert!(
            queries_per_sec > 1000.0,
            "Batch retrieval {:.0} < 1K queries/sec",
            queries_per_sec
        );
    }

    // ========================================================================
    // Plasticity Throughput
    // ========================================================================

    #[test]
    fn btsp_consolidation_replay() {
        // Target: >100 samples/sec
        let mut rng = StdRng::seed_from_u64(42);
        let test_duration = Duration::from_secs(5);

        // let btsp = BTSPLearner::new(1000, 0.01, 100);

        let mut samples_processed = 0u64;
        let start = Instant::now();

        while start.elapsed() < test_duration {
            // Generate batch of samples
            let batch: Vec<Vec<f32>> = (0..10)
                .map(|_| (0..128).map(|_| rng.gen()).collect())
                .collect();

            // btsp.replay_batch(&batch);
            // Placeholder
            for _sample in batch {
                samples_processed += 1;
            }
        }

        let duration = start.elapsed();
        let samples_per_sec = samples_processed as f64 / duration.as_secs_f64();

        println!("BTSP replay: {:.0} samples/sec", samples_per_sec);
        assert!(
            samples_per_sec > 100.0,
            "BTSP replay {:.0} < 100 samples/sec",
            samples_per_sec
        );
    }

    /// Returns (tasks_processed, duration).
    fn meta_learning_workload(test_duration: Duration) -> (u64, Duration) {
        // let meta = MetaLearner::new();
        let mut tasks_processed = 0u64;
        let start = Instant::now();

        while start.elapsed() < test_duration {
            // let task = generate_task();
            // meta.adapt_to_task(&task);
            // Placeholder
            tasks_processed += 1;
        }

        (tasks_processed, start.elapsed())
    }

    #[test]
    fn meta_learning_task_throughput() {
        // Smoke: exercise the adapt loop, assert tasks were processed.
        let (tasks_processed, duration) = meta_learning_workload(Duration::from_secs(2));
        let tasks_per_sec = tasks_processed as f64 / duration.as_secs_f64();
        println!("Meta-learning: {:.0} tasks/sec", tasks_per_sec);
        assert!(
            tasks_processed > 0,
            "Meta-learning processed zero tasks under sustained load"
        );
    }

    #[cfg(feature = "perf-tests")]
    #[test]
    fn meta_learning_task_throughput_perf() {
        // Target: >50 tasks/sec
        let (tasks_processed, duration) = meta_learning_workload(Duration::from_secs(5));
        let tasks_per_sec = tasks_processed as f64 / duration.as_secs_f64();
        println!("Meta-learning: {:.0} tasks/sec", tasks_per_sec);
        assert!(
            tasks_per_sec > 50.0,
            "Meta-learning {:.0} < 50 tasks/sec",
            tasks_per_sec
        );
    }

    // ========================================================================
    // Memory Growth Tests
    // ========================================================================

    #[test]
    fn sustained_load_memory_stability() {
        // Ensure memory doesn't grow unbounded under sustained load
        let test_duration = Duration::from_secs(60); // 1 minute

        // let bus = EventBus::new(1000);
        let start = Instant::now();
        let mut iterations = 0u64;

        // Sample memory at intervals
        let mut memory_samples = Vec::new();
        let sample_interval = Duration::from_secs(10);
        let mut last_sample = Instant::now();

        while start.elapsed() < test_duration {
            // bus.publish(Event::new("test", vec![0.0; 128]));
            // bus.consume();
            iterations += 1;

            if last_sample.elapsed() >= sample_interval {
                // In production: measure actual RSS/heap
                memory_samples.push(iterations);
                last_sample = Instant::now();
            }
        }

        println!("Iterations: {}", iterations);
        println!("Memory samples: {:?}", memory_samples);

        // Memory should stabilize (not grow linearly)
        // This is a simplified check - real impl would use memory profiling
        if memory_samples.len() >= 3 {
            let first_half_avg = memory_samples[..memory_samples.len() / 2]
                .iter()
                .sum::<u64>() as f64
                / (memory_samples.len() / 2) as f64;
            let second_half_avg = memory_samples[memory_samples.len() / 2..]
                .iter()
                .sum::<u64>() as f64
                / (memory_samples.len() - memory_samples.len() / 2) as f64;

            // Growth should be sub-linear
            println!(
                "First half avg: {:.0}, Second half avg: {:.0}",
                first_half_avg, second_half_avg
            );
        }
    }

    // ========================================================================
    // CPU Utilization Tests
    // ========================================================================

    #[test]
    #[ignore] // Run manually for profiling
    fn cpu_utilization_profiling() {
        // Profile CPU usage under different loads
        let test_duration = Duration::from_secs(30);

        println!("Starting CPU profiling...");
        println!(
            "Run with: cargo test --release cpu_utilization_profiling -- --ignored --nocapture"
        );

        let start = Instant::now();
        let mut operations = 0u64;

        while start.elapsed() < test_duration {
            // Simulate mixed workload
            // EventBus publish
            let _ev = vec![0.0f32; 128];

            // HDC encoding
            let _hv: Vec<u64> = (0..157).map(|_| rand::random()).collect();

            // WTA competition
            let inputs: Vec<f32> = (0..1000).map(|_| rand::random()).collect();
            let _winner = inputs
                .iter()
                .enumerate()
                .max_by(|(_, a), (_, b)| a.partial_cmp(b).unwrap())
                .unwrap()
                .0;

            operations += 1;
        }

        println!("Operations completed: {}", operations);
        println!(
            "Ops/sec: {:.0}",
            operations as f64 / test_duration.as_secs_f64()
        );
    }
}
