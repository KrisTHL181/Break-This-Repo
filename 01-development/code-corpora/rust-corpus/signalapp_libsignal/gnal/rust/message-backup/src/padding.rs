// Copyright (C) 2026 Signal Messenger, LLC.
// SPDX-License-Identifier: AGPL-3.0-only

//! Sizing decisions for a compressed backup stream.
//!
//! Two numbers used by the client that is writing a backup:
//!
//! - [`flush_interval`] — how many uncompressed bytes to write before ending the current DEFLATE
//!   block.
//! - [`padding_size`] — how many zero bytes to append to the finished stream.
//!
//! The client owns the compressor and does the flushing itself; this module does the arithmetic so
//! that every platform makes the same choices.

use rand::{CryptoRng, Rng};
use rand_distr::StandardNormal;

/// Channel input/output size ratio
pub const SENSITIVITY: f64 = 4.0;

/// Output bytes one `Z_FULL_FLUSH` costs, approximate.
pub const FLUSH_COST_BYTES: f64 = 1024.0;

/// Padding is drawn from a normal centered this many standard deviations above zero, so that a draw
/// that would be negative is rare.
pub const TRUNCATION_SIGMAS: f64 = 3.0;

/// Padding noise is this many times the largest length difference an attacker can induce.
pub const NOISE_SCALE: f64 = 2.0;

/// Floor on the flush interval, so that small backups do not end blocks absurdly often.
pub const MIN_INTERVAL_BYTES: u64 = 8 * 1024;

/// Ceiling on the flush interval, so it does not grow without limit on very large backups.
pub const MAX_INTERVAL_BYTES: u64 = 512 * 1024;

/// Smallest backup we will produce, in bytes.
///
/// The randomized backup size is rounded up to at least this value. This is post-processing of the
/// randomized result, so it cannot reveal any additional information; see [`padding_size`].
pub const MINIMUM_BACKUP_BYTES: u64 = 64 * 1024;

/// Standard deviation of the padding for a backup whose largest DEFLATE block was
/// `max_interval_bytes` uncompressed bytes.
pub fn sigma(max_interval_bytes: u64) -> f64 {
    let block = max_interval_bytes.max(MIN_INTERVAL_BYTES) as f64;
    NOISE_SCALE * block / SENSITIVITY
}

/// The interval that costs least over a backup of `total` uncompressed bytes.
fn optimal_interval(total: f64) -> f64 {
    (SENSITIVITY * FLUSH_COST_BYTES * total / (TRUNCATION_SIGMAS * NOISE_SCALE)).sqrt()
}

/// Uncompressed bytes to write before ending the current DEFLATE block.
///
/// `uncompressed_len` counts uncompressed bytes written since the compression region began.
///
/// `estimated_total_uncompressed_len` is the estimated total **uncompressed** length of the backup,
/// or `None`. The uncompressed length of the previous backup is a good estimate. The estimate does
/// not need to be provided, does not need to be accurate, and has no effect on security - it just
/// allows more efficient compression.
pub fn flush_interval(uncompressed_len: u64, estimated_total_uncompressed_len: Option<u64>) -> u64 {
    // The interval depends on how long the backup will be, which is not known while it is being
    // written. So assume a length and use the interval that suits it:
    //
    //   - Given an estimate, believe it. Once the backup passes it we have learned only that it
    //     was low, and the cheapest assumption then is that the backup ends here.
    //   - Given no estimate, assume the backup is half written. That is the assumption with the
    //     best worst case: about 41% over what a writer who knew the length would pay, which no
    //     other rule improves on.
    let assumed = match estimated_total_uncompressed_len {
        Some(total) if total > 0 => total.max(uncompressed_len) as f64,
        _ => 2.0 * uncompressed_len as f64,
    };

    #[expect(clippy::cast_possible_truncation)]
    {
        // Saturating: the interval is positive and finite, and the ceiling clamps it anyway.
        (optimal_interval(assumed) as u64).clamp(MIN_INTERVAL_BYTES, MAX_INTERVAL_BYTES)
    }
}

/// Number of zero bytes to append to a finished backup stream.
///
/// `max_interval_bytes` must be the largest DEFLATE block the writer **actually produced** in the
/// chat item region.
///
/// If the sampled padding would leave the backup smaller than [`MINIMUM_BACKUP_BYTES`], the padding
/// is increased to reach that size. This is equivalent to taking the maximum after randomization,
/// so it is privacy-preserving post-processing.
pub fn padding_size<R: Rng + CryptoRng + ?Sized>(
    max_interval_bytes: u64,
    compressed_len: u64,
    rng: &mut R,
) -> u64 {
    let sigma = sigma(max_interval_bytes);
    let mean = TRUNCATION_SIGMAS * sigma;

    // Rejection sampling keeps the padding positive. A normal draw is unbounded
    // below, and this relies on Rust's float-to-integer cast saturating: it
    // sends every negative draw to zero (since Rust 1.45,
    // https://doc.rust-lang.org/reference/expressions/operator-expr.html#r-expr.as.numeric.float-as-int),
    // which the check below then rejects. A draw lands below one with
    // probability Phi(-TRUNCATION_SIGMAS), about 0.13%, so one backup in 740
    // costs a second iteration and one in 550,000 costs a third.
    let draw = loop {
        let z: f64 = rng.sample(StandardNormal);
        let draw = mean + sigma * z;
        #[expect(clippy::cast_possible_truncation)]
        let draw = draw as u64;
        if draw >= 1 {
            break draw;
        }
    };

    draw.max(MINIMUM_BACKUP_BYTES.saturating_sub(compressed_len))
}

#[cfg(test)]
mod test {
    use rand::SeedableRng as _;
    use rand::rngs::StdRng;

    use super::*;

    /// If this fails, the fix is not to update the numbers.
    #[test]
    fn flush_interval_values() {
        // No estimate: the interval grows with position. `x` is bytes into the chat item region.
        assert_eq!(flush_interval(0, None), MIN_INTERVAL_BYTES);
        assert_eq!(flush_interval(1, None), MIN_INTERVAL_BYTES);
        assert_eq!(flush_interval(65_536, None), 9_459);
        assert_eq!(flush_interval(1_048_576, None), 37_837);
        assert_eq!(flush_interval(7_700_000, None), 102_533);
        assert_eq!(flush_interval(16_777_216, None), 151_348);
        assert_eq!(flush_interval(200_000_000, None), 522_557);

        // With an estimate: one interval, held.
        assert_eq!(flush_interval(0, Some(1_048_576)), 26_754);
        assert_eq!(flush_interval(0, Some(16_777_216)), 107_019);
        assert_eq!(flush_interval(0, Some(200_000_000)), 369_504);
    }

    #[test]
    fn flush_interval_is_monotone_and_clamped() {
        let mut previous = 0;
        for x in (0..64 * 1024 * 1024).step_by(97_003) {
            let interval = flush_interval(x, None);
            assert!(interval >= previous, "not monotone at {x}");
            assert!((MIN_INTERVAL_BYTES..=MAX_INTERVAL_BYTES).contains(&interval));
            previous = interval;
        }
    }

    #[test]
    fn estimate_holds_a_fixed_interval_until_the_backup_crosses_it() {
        let total = 1_048_576;
        let estimate = Some(total);

        // Fixed while the backup is smaller than the estimate.
        assert_eq!(flush_interval(0, estimate), 26_754);
        assert_eq!(flush_interval(total / 2, estimate), 26_754);
        assert_eq!(flush_interval(total - 1, estimate), 26_754);

        // Past it, the interval is the one that suits a backup ending here.
        for x in [total, 4 * total, 64 * total] {
            assert_eq!(flush_interval(x, estimate), flush_interval(0, Some(x)));
        }
    }

    /// Not knowing the length costs a factor of sqrt(2) in interval, and the two branches have to
    /// agree on that or one of them is using the wrong constant.
    #[test]
    fn the_unknown_length_interval_is_sqrt_2_larger() {
        for total in [1 << 20, 1 << 22, 1 << 24] {
            let known = flush_interval(0, Some(total)) as f64;
            let unknown = flush_interval(total, None) as f64;
            assert!(
                (unknown / known - std::f64::consts::SQRT_2).abs() < 0.001,
                "{unknown} / {known}"
            );
        }
    }

    #[test]
    fn an_estimate_of_zero_is_no_estimate() {
        // The bridge passes 0 for "not known"; the guard has to treat it as absent rather than
        // pinning the interval at the floor.
        assert_eq!(
            flush_interval(1_048_576, Some(0)),
            flush_interval(1_048_576, None)
        );
    }

    #[test]
    fn sigma_values() {
        // Half the largest block, floored.
        assert_eq!(sigma(0), 4_096.0);
        assert_eq!(sigma(MIN_INTERVAL_BYTES), 4_096.0);
        assert_eq!(sigma(151_348), 75_674.0);
        assert_eq!(sigma(MAX_INTERVAL_BYTES), 262_144.0);
    }

    #[test]
    fn padding_is_never_zero() {
        let mut rng = StdRng::seed_from_u64(0x5150);
        // A block at the floor is the case with the smallest padding, so it is where a draw of zero
        // is most likely.
        for _ in 0..100_000 {
            assert!(padding_size(MIN_INTERVAL_BYTES, 10 * 1024 * 1024, &mut rng) >= 1);
        }
    }

    #[test]
    fn padding_distribution_matches_sigma() {
        let mut rng = StdRng::seed_from_u64(0xf1a5);
        let block = 37_837;
        let sigma = sigma(block);

        // Large enough that the minimum backup size never binds.
        let compressed_len = 10 * 1024 * 1024;
        let count = 100_000;
        let draws: Vec<f64> = (0..count)
            .map(|_| padding_size(block, compressed_len, &mut rng) as f64)
            .collect();

        let mean = draws.iter().sum::<f64>() / count as f64;
        let variance =
            draws.iter().map(|d| (d - mean) * (d - mean)).sum::<f64>() / (count - 1) as f64;

        let expected_mean = TRUNCATION_SIGMAS * sigma;
        assert!(
            (mean - expected_mean).abs() < 0.01 * expected_mean,
            "mean {mean} against {expected_mean}"
        );
        assert!(
            (variance.sqrt() - sigma).abs() < 0.02 * sigma,
            "sigma {} against {sigma}",
            variance.sqrt()
        );
    }

    #[test]
    fn padding_spreads_sizes() {
        let mut rng = StdRng::seed_from_u64(0xba5e);
        let compressed_len = 10 * 1024 * 1024;
        let sizes: std::collections::HashSet<u64> = (0..25)
            .map(|_| compressed_len + padding_size(37_837, compressed_len, &mut rng))
            .collect();
        assert!(sizes.len() > 20, "only {} distinct sizes", sizes.len());
        // In particular, never the unpadded length itself.
        assert!(!sizes.contains(&compressed_len));
    }

    #[test]
    fn minimum_backup_size_is_enforced() {
        let mut rng = StdRng::seed_from_u64(0xd15c);
        for compressed_len in [0, 1_000, MINIMUM_BACKUP_BYTES / 2, MINIMUM_BACKUP_BYTES - 1] {
            for _ in 0..1_000 {
                let padded =
                    compressed_len + padding_size(MIN_INTERVAL_BYTES, compressed_len, &mut rng);
                assert!(padded >= MINIMUM_BACKUP_BYTES);
            }
        }
    }
}
