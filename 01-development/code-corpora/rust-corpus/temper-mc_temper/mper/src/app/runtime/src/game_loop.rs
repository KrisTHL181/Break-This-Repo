//! Main game loop module.
//!
//! This module contains the core server loop that:
//! - Initializes the ECS (Entity Component System) world
//! - Sets up networking (TCP connection acceptor)
//! - Runs timed schedules (tick, world sync, keepalive, etc.)
//! - Handles graceful shutdown

use crate::blocklist::blocklist;
use crate::errors::BinaryError;
use crate::tui;
use bevy_ecs::prelude::World;
use bevy_ecs::schedule::Schedule;
use crossbeam_channel::Sender;
use std::sync::Arc;
use std::sync::atomic::Ordering::Relaxed;
use std::time::{Duration, Instant};
use temper_game_systems::{LanPinger, register_schedules};
use temper_messages::register_messages;
use temper_net_runtime::connection::{NewConnection, handle_connection};
use temper_net_runtime::server::create_server_listener;
use temper_performance::tick::TickData;
use temper_protocol::{PacketSender, create_packet_senders};
use temper_resources::register_resources;
use temper_scheduler::Scheduler;
use temper_state::{GlobalState, GlobalStateResource};
use temper_utils::formatting::format_duration;
use tokio::io::AsyncWriteExt;
use tracing::{Instrument, debug, error, info, info_span, trace, warn};

/// Main entry point for the server game loop.
///
/// This function:
/// 1. Initializes the Bevy ECS world and registers all systems/resources
/// 2. Starts the TCP connection acceptor on a separate thread
/// 3. Runs the main scheduler loop that executes timed schedules (tick, sync, etc.)
/// 4. Handles graceful shutdown when the server is stopped
pub fn start_game_loop(global_state: GlobalState, no_tui: bool) -> Result<(), BinaryError> {
    // =========================================================================
    // PHASE 1: ECS World Setup
    // =========================================================================

    // Create the Bevy ECS world - this holds all entities, components, and resources
    let mut ecs_world = World::new();

    // Schedule that runs cleanup systems when the server shuts down
    let mut shutdown_schedule = Schedule::default();

    // =========================================================================
    // PHASE 2: Channel Setup for Inter-Thread Communication
    // =========================================================================

    // Packet sender for outgoing network packets (shared across connection handlers)
    let sender_struct = Arc::new(create_packet_senders(&mut ecs_world));

    // Channel for new player connections (TCP acceptor -> main loop)
    let (new_conn_send, new_conn_recv) = crossbeam_channel::unbounded();

    // Shutdown coordination channels:
    // - shutdown_send/recv: Main loop tells TCP acceptor to stop
    // - shutdown_response: TCP acceptor confirms it has stopped
    let (shutdown_send, shutdown_recv) = tokio::sync::oneshot::channel();
    let (shutdown_response_send, shutdown_response_recv) = crossbeam_channel::unbounded();

    let (server_command_tx, server_command_rx) = crossbeam_channel::unbounded::<String>();
    if !no_tui {
        tui::run_tui(global_state.clone(), server_command_tx);
    }

    // =========================================================================
    // PHASE 3: Register ECS Systems and Resources
    // =========================================================================

    // Initialize default server commands (e.g., /stop, /help, etc.)
    temper_default_commands::init();

    // Wrap global state for ECS resource access
    let global_state_res = GlobalStateResource(global_state.clone());

    // Register event messages the ECS will handle
    register_messages(&mut ecs_world);

    // Register shared resources (connection receiver, global state, etc.)
    register_resources(
        &mut ecs_world,
        new_conn_recv,
        global_state_res,
        server_command_rx,
    );

    // Build the timed scheduler with all periodic schedules and shutdown systems.
    let mut timed = Scheduler::new();
    register_schedules(&mut timed, &mut shutdown_schedule, global_state.clone());

    // =========================================================================
    // PHASE 4: Start Network Thread
    // =========================================================================

    // Spawn the TCP connection acceptor on a dedicated thread with its own Tokio runtime
    tcp_conn_acceptor(
        global_state.clone(),
        sender_struct,
        Arc::new(new_conn_send),
        shutdown_recv,
        shutdown_response_send,
    )?;

    info!(
        "Server is ready in {}",
        format_duration(global_state.start_time.elapsed())
    );

    // =========================================================================
    // PHASE 5: Main Scheduler Loop
    // =========================================================================

    // Maximum number of schedules to run in a single iteration before yielding.
    // This prevents starvation if we fall behind (e.g., after a lag spike).
    const MAX_GLOBAL_CATCH_UP: usize = 64;

    let tick_zero = Instant::now();

    // Main loop - runs until shutdown flag is set (e.g., via Ctrl+C or /stop command)
    while !global_state.shut_down.load(Relaxed) {
        let tick_start = Instant::now();
        let mut ran_any = false;
        let mut ran_count = 0;

        // Inner loop: Run all schedules that are currently due
        loop {
            // Prevent running too many schedules in one go (catch-up limit)
            if ran_count >= MAX_GLOBAL_CATCH_UP {
                break;
            }

            let now = Instant::now();

            // Peek at the next schedule that's due to run
            let Some((idx, due)) = timed.peek_next_due() else {
                // No schedules registered, wait a bit
                // which is unexpected, because we should have at least the tick schedule
                warn!("No schedules registered (this is a bug)");
                std::thread::sleep(Duration::from_millis(1));
                continue;
            };

            // If the next schedule isn't due yet, exit inner loop
            if due > now {
                break;
            }

            // Pop the schedule from the priority queue
            let (popped_idx, _popped_due) = timed
                .pop_next_due()
                .expect("scheduler heap changed unexpectedly");
            debug_assert_eq!(popped_idx, idx);

            // Get schedule metadata for logging
            let name = timed.schedules[idx].name.clone();
            let period = timed.schedules[idx].period;

            // Execute the schedule and measure how long it took
            let start = Instant::now();
            timed.schedules[idx].schedule.run(&mut ecs_world);
            let elapsed = start.elapsed();

            // Log warning if schedule took longer than its allocated time budget
            if elapsed > period {
                warn!(
                    "Schedule '{}' overran: took {:?}, budget {:?}",
                    name, elapsed, period
                );
            } else {
                trace!(
                    "Schedule '{}' ran in {:?} (budget {:?})",
                    name, elapsed, period
                );
            }

            // Reschedule for next run (updates next_due time based on MissedTickBehavior)
            timed.after_run(idx);

            ran_any = true;
            ran_count += 1;
        }

        let tick_duration = tick_start.elapsed();

        // If no schedules were ready, sleep until the next one is due
        // If schedules were ran, store tick data.
        if !ran_any {
            timed.park_until_next_due();
        } else {
            let tick_data = TickData {
                start_ns: tick_zero.elapsed().as_nanos(),
                duration_ns: tick_duration.as_nanos(),
                entity_count: 0,
                ran_count,
            };

            let mut performance = global_state
                .performance
                .lock()
                .expect("Failed to lock performance resource");
            performance.tps.record_tick(tick_data);
        }
    }

    // =========================================================================
    // PHASE 6: Graceful Shutdown
    // =========================================================================

    // Signal the TCP acceptor thread to stop accepting new connections
    trace!("Sending shutdown signal to TCP connection acceptor");
    let _ = shutdown_send.send(());

    // Run shutdown systems (save world, disconnect players, cleanup)
    shutdown_schedule.run(&mut ecs_world);

    // Wait for TCP acceptor to confirm it has shut down cleanly
    trace!("Waiting for TCP connection acceptor to shut down");
    let _ = shutdown_response_recv.recv();

    Ok(())
}

/// Spawns the LAN broadcast pinger task.
///
/// This broadcasts the server's presence on the local network using UDP multicast
/// to Mojang's LAN discovery address (224.0.2.60:4445). Minecraft clients scanning
/// for LAN games will pick up these broadcasts.
///
/// The 1.5 second interval is a balance between:
/// - Fast enough for clients to discover the server quickly
/// - Slow enough to not spam the network with unnecessary traffic
async fn spawn_lan_pinger(state: GlobalState) {
    let Ok(mut pinger) = LanPinger::new().await else {
        error!("Failed creating LAN pinger");
        return;
    };

    while !state.shut_down.load(Relaxed) {
        pinger.send(&state.config).await;
        tokio::time::sleep(Duration::from_millis(1500)).await;
    }
}

/// Spawns a dedicated thread for accepting TCP connections.
///
/// This function creates a new OS thread with its own Tokio async runtime that:
/// 1. Starts a LAN pinger to broadcast the server on local network
/// 2. Listens for incoming TCP connections on the configured port
/// 3. Spawns a handler task for each new connection
/// 4. Responds to shutdown signals for graceful termination
///
/// # Arguments
/// * `state` - Global server state (shared across all connections)
/// * `packet_sender` - Channel for sending outgoing packets
/// * `sender` - Channel to notify main loop of new connections
/// * `shutdown_notify` - Receives signal when server is shutting down
/// * `shutdown_response` - Sends confirmation when this thread has stopped
///
/// # Why a separate thread?
/// The network acceptor runs on its own thread with a dedicated Tokio runtime
/// to isolate async I/O from the main game loop. This prevents network lag
/// from affecting game tick timing and vice versa.
fn tcp_conn_acceptor(
    state: GlobalState,
    packet_sender: Arc<PacketSender>,
    sender: Arc<Sender<NewConnection>>,
    mut shutdown_notify: tokio::sync::oneshot::Receiver<()>,
    shutdown_response: Sender<()>,
) -> Result<(), BinaryError> {
    let named_thread = std::thread::Builder::new().name("TokioAsyncThread".to_string());
    named_thread.spawn(move || {
        // Catch panics to ensure graceful shutdown even if something goes wrong
        // We catch it so we can shut down the entire server instead of leaving it open with a crashed network loop
        let caught_panic = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
            // Create a dedicated single-threaded Tokio runtime for networking
            let async_runtime = tokio::runtime::Builder::new_current_thread()
                .enable_all()
                .thread_name("Tokio-Async-Network")
                .build()?;

            // Spawn LAN broadcast pinger (for local network server discovery)
            async_runtime.spawn(spawn_lan_pinger(state.clone()));

            if state.config.block_scanner_ips {
                async_runtime.spawn(blocklist(state.clone()));
            }

            // Main connection accept loop
            async_runtime.block_on({
                let state = Arc::clone(&state);
                async move {
                    // Create the TCP listener on the configured address/port
                    let Ok(listener) = create_server_listener(&state.config).await else {
                        error!("Failed to create TCP listener");
                        return Err::<(), BinaryError>(BinaryError::Custom(
                            "Failed to create TCP listener".to_string(),
                        ));
                    };

                    // Accept connections until shutdown is signaled
                    while !state.shut_down.load(Relaxed) {
                        // Use tokio::select! to handle both new connections AND shutdown signal
                        tokio::select! {
                            // Branch 1: New TCP connection incoming
                            accept_result = listener.accept() => {
                                match accept_result {
                                    Ok((mut stream, _)) => {
                                        let addy = stream.peer_addr()?;
                                        debug!("Got TCP connection from {}", addy);
                                        if state.blocked_ips.contains(&addy.ip().to_string()) {
                                            debug!("Rejected connection from blocked IP: {}", addy);
                                            stream.write_all("Lol nah".as_bytes()).await.ok();
                                            stream.shutdown().await.ok();
                                            continue;
                                        }

                                        // Spawn a task to handle this connection asynchronously
                                        tokio::spawn({
                                            let state = Arc::clone(&state);
                                            let packet_sender = Arc::clone(&packet_sender);
                                            let sender = Arc::clone(&sender);
                                            async move {
                                                // handle_connection manages the full lifecycle:
                                                // handshake -> login -> play -> disconnect
                                                _ = handle_connection(state, stream, packet_sender, sender)
                                                    .instrument(info_span!("conn", %addy).or_current())
                                                    .await;
                                            }
                                        });
                                    }
                                    Err(e) => {
                                        error!("Failed to accept TCP connection: {:?}", e);
                                    }
                                }
                            }
                            // Branch 2: Shutdown signal received
                            _ = &mut shutdown_notify => {
                                trace!("Shutdown signal received on notify channel");
                                break;
                            }
                        }
                    }
                    Ok(())
                }
            })?;

            trace!("Shutting down TCP connection acceptor");

            // Notify main loop that we've finished shutting down
            shutdown_response.send(()).expect("Failed to send shutdown response");
            Ok::<(), BinaryError>(())
        }));

        // Handle panic case - ensure server shuts down cleanly
        if let Err(e) = caught_panic {
            error!("TCP connection acceptor thread panicked: {:?}", e);
            // Set shutdown flag so the main loop knows something went wrong
            state
                .shut_down
                .store(true, Relaxed);
            return Err::<(), BinaryError>(BinaryError::Custom(
                "TCP connection acceptor thread panicked".to_string(),
            ));
        }
        Err(BinaryError::Custom(
            "TCP connection acceptor thread panicked".to_string(),
        ))
    })?;
    Ok(())
}
