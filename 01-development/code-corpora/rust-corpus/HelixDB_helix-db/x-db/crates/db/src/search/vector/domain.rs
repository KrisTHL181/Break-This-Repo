//! Metric-bound vector validation before active HNSW access.
//!
//! Persisted vectors and request inputs remain in their deployed `f32`
//! representation. This module binds those values to one validated metric and
//! dimension, rejecting values that cannot preserve finite current-generation
//! scores before any graph, cache, or lifecycle mutation occurs.

use std::borrow::Cow;

use super::unaligned_vector::{UnalignedVector, UnalignedVectorCodec};
use super::{VectorDimension, VectorDistanceMetric};

/// Inclusive component magnitude limit for a bounded active metric.
#[derive(Debug, Clone, Copy, PartialEq)]
pub(crate) struct VectorComponentLimit {
    metric: VectorDistanceMetric,
    dimension: VectorDimension,
    inclusive_maximum: f32,
}

impl VectorComponentLimit {
    /// Derive the metric limit using checked `f64` arithmetic.
    ///
    /// Cosine has no component magnitude limit and returns `None`.
    pub(crate) fn try_new(
        metric: VectorDistanceMetric,
        dimension: VectorDimension,
    ) -> Result<Option<Self>, VectorMagnitudeDomainError> {
        let factor = match metric {
            VectorDistanceMetric::Cosine => return Ok(None),
            VectorDistanceMetric::Euclidean => 8_u64,
            VectorDistanceMetric::Manhattan => 4_u64,
        };
        let dimension_u64 = u64::try_from(dimension.get()).map_err(|_| {
            VectorMagnitudeDomainError::DimensionArithmeticOverflow {
                dimension: dimension.get(),
            }
        })?;
        let divisor = dimension_u64.checked_mul(factor).ok_or(
            VectorMagnitudeDomainError::DimensionArithmeticOverflow {
                dimension: dimension.get(),
            },
        )?;
        let exact = match metric {
            VectorDistanceMetric::Euclidean => (f64::from(f32::MAX) / divisor as f64).sqrt(),
            VectorDistanceMetric::Manhattan => f64::from(f32::MAX) / divisor as f64,
            VectorDistanceMetric::Cosine => unreachable!("cosine returned above"),
        };
        if !exact.is_finite() || exact <= 0.0 {
            return Err(VectorMagnitudeDomainError::InvalidComputedLimit {
                metric,
                dimension: dimension.get(),
            });
        }
        let rounded = exact as f32;
        let inclusive_maximum = if f64::from(rounded) > exact {
            f32::from_bits(rounded.to_bits().checked_sub(1).ok_or(
                VectorMagnitudeDomainError::InvalidComputedLimit {
                    metric,
                    dimension: dimension.get(),
                },
            )?)
        } else {
            rounded
        };
        if !inclusive_maximum.is_finite() || inclusive_maximum <= 0.0 {
            return Err(VectorMagnitudeDomainError::InvalidComputedLimit {
                metric,
                dimension: dimension.get(),
            });
        }
        Ok(Some(Self {
            metric,
            dimension,
            inclusive_maximum,
        }))
    }

    /// Return the metric owning this limit.
    pub(crate) const fn metric(self) -> VectorDistanceMetric {
        self.metric
    }

    /// Return the dimension used to derive this limit.
    pub(crate) const fn dimension(self) -> VectorDimension {
        self.dimension
    }

    /// Return the accepted inclusive component magnitude.
    pub(crate) const fn inclusive_maximum(self) -> f32 {
        self.inclusive_maximum
    }
}

/// A vector proven valid for one active metric and dimension.
///
/// Fields are private so active HNSW code cannot construct this proof without
/// running the complete validation contract.
pub(crate) struct ValidatedMetricVector<'a, C>
where
    C: UnalignedVectorCodec,
{
    values: Cow<'a, UnalignedVector<C>>,
    metric: VectorDistanceMetric,
    dimension: VectorDimension,
}

impl<'a, C> ValidatedMetricVector<'a, C>
where
    C: UnalignedVectorCodec,
{
    /// Validate one decoded or borrowed vector under authoritative semantics.
    pub(crate) fn try_new(
        values: Cow<'a, UnalignedVector<C>>,
        metric: VectorDistanceMetric,
        dimension: VectorDimension,
    ) -> Result<Self, VectorValidationError> {
        if values.len() != dimension.get() {
            return Err(VectorValidationError::DimensionMismatch {
                expected: dimension.get(),
                actual: values.len(),
            });
        }
        for (index, component) in values.iter().enumerate() {
            if !component.is_finite() {
                return Err(VectorValidationError::NonFiniteComponent { index });
            }
        }
        if metric == VectorDistanceMetric::Cosine && values.is_zero() {
            return Err(VectorValidationError::ZeroNormCosineVector);
        }
        let limit = VectorComponentLimit::try_new(metric, dimension)?;
        if let Some(limit) = limit {
            for (component_index, component) in values.iter().enumerate() {
                let observed_magnitude = component.abs();
                if observed_magnitude > limit.inclusive_maximum() {
                    return Err(VectorValidationError::ComponentMagnitudeExceeded {
                        metric: limit.metric(),
                        dimension: limit.dimension().get(),
                        component_index,
                        observed_magnitude,
                        inclusive_maximum: limit.inclusive_maximum(),
                    });
                }
            }
        }
        Ok(Self {
            values,
            metric,
            dimension,
        })
    }

    /// Borrow the validated physical vector.
    pub(crate) fn values(&self) -> &UnalignedVector<C> {
        debug_assert_eq!(self.values.len(), self.dimension.get());
        debug_assert!(
            VectorComponentLimit::try_new(self.metric, self.dimension).is_ok(),
            "validated vector retains a derivable metric domain"
        );
        &self.values
    }

    /// Consume the proof and return the validated physical vector.
    pub(crate) fn into_values(self) -> Cow<'a, UnalignedVector<C>> {
        self.values
    }

    /// Return the bound metric for contract assertions.
    #[cfg(test)]
    pub(crate) const fn metric(&self) -> VectorDistanceMetric {
        self.metric
    }

    /// Return the bound dimension for contract assertions.
    #[cfg(test)]
    pub(crate) const fn dimension(&self) -> VectorDimension {
        self.dimension
    }
}

impl<'a> ValidatedMetricVector<'a, f32> {
    /// Validate a borrowed aligned request vector without copying.
    pub(crate) fn try_from_slice(
        values: &'a [f32],
        metric: VectorDistanceMetric,
        dimension: VectorDimension,
    ) -> Result<Self, VectorValidationError> {
        Self::try_new(UnalignedVector::from_slice(values), metric, dimension)
    }

    /// Validate and own a vector for domain contract assertions.
    #[cfg(test)]
    pub(crate) fn try_from_vec(
        values: Vec<f32>,
        metric: VectorDistanceMetric,
        dimension: VectorDimension,
    ) -> Result<ValidatedMetricVector<'static, f32>, VectorValidationError> {
        ValidatedMetricVector::try_new(UnalignedVector::from_vec(values), metric, dimension)
    }
}

/// Failure to derive a finite positive component limit.
#[derive(Debug, Clone, Copy, PartialEq, Eq, thiserror::Error)]
pub(crate) enum VectorMagnitudeDomainError {
    /// Integer arithmetic could not represent the dimension divisor.
    #[error("vector magnitude limit arithmetic overflowed for dimension {dimension}")]
    DimensionArithmeticOverflow {
        /// Dimension whose checked arithmetic failed.
        dimension: usize,
    },
    /// The formula did not produce a positive finite representable limit.
    #[error("invalid {metric:?} vector magnitude limit for dimension {dimension}")]
    InvalidComputedLimit {
        /// Metric whose formula failed.
        metric: VectorDistanceMetric,
        /// Dimension used by the formula.
        dimension: usize,
    },
}

/// Complete metric-vector boundary validation failure.
#[derive(Debug, Clone, PartialEq, thiserror::Error)]
pub(crate) enum VectorValidationError {
    /// The vector length did not match authoritative metadata.
    #[error("vector dimension mismatch: expected {expected}, got {actual}")]
    DimensionMismatch {
        /// Authoritative component count.
        expected: usize,
        /// Observed component count.
        actual: usize,
    },
    /// One component was NaN or infinite.
    #[error("vector component {index} is not finite")]
    NonFiniteComponent {
        /// Zero-based component offset.
        index: usize,
    },
    /// Cosine distance received a true zero vector.
    #[error("cosine vector norm must be non-zero")]
    ZeroNormCosineVector,
    /// A finite component exceeded the inclusive metric/dimension limit.
    #[error(
        "{metric:?} vector dimension {dimension} component {component_index} magnitude {observed_magnitude} exceeds inclusive maximum {inclusive_maximum}"
    )]
    ComponentMagnitudeExceeded {
        /// Bound distance metric.
        metric: VectorDistanceMetric,
        /// Authoritative component count.
        dimension: usize,
        /// Zero-based component offset.
        component_index: usize,
        /// Absolute observed component value.
        observed_magnitude: f32,
        /// Inclusive accepted maximum.
        inclusive_maximum: f32,
    },
    /// The limit formula could not be evaluated safely.
    #[error(transparent)]
    MagnitudeDomain(#[from] VectorMagnitudeDomainError),
}

#[cfg(test)]
mod tests {
    use super::*;

    fn exact_limit(metric: VectorDistanceMetric, dimension: usize) -> f64 {
        match metric {
            VectorDistanceMetric::Euclidean => {
                (f64::from(f32::MAX) / (8.0 * dimension as f64)).sqrt()
            }
            VectorDistanceMetric::Manhattan => f64::from(f32::MAX) / (4.0 * dimension as f64),
            VectorDistanceMetric::Cosine => unreachable!("cosine has no magnitude limit"),
        }
    }

    fn next_up(value: f32) -> f32 {
        f32::from_bits(value.to_bits() + 1)
    }

    #[test]
    fn limits_round_down_for_every_boundary_dimension() {
        for dimension in [1_usize, 15, 16, 17, 31, 32, 33, 1536, u32::MAX as usize] {
            let dimension = VectorDimension::try_new(dimension).unwrap();
            for metric in [
                VectorDistanceMetric::Euclidean,
                VectorDistanceMetric::Manhattan,
            ] {
                let limit = VectorComponentLimit::try_new(metric, dimension)
                    .unwrap()
                    .unwrap();
                let exact = exact_limit(metric, dimension.get());
                assert_eq!(limit.metric(), metric);
                assert_eq!(limit.dimension(), dimension);
                assert!(limit.inclusive_maximum().is_finite());
                assert!(limit.inclusive_maximum() > 0.0);
                assert!(f64::from(limit.inclusive_maximum()) <= exact);
                assert!(f64::from(next_up(limit.inclusive_maximum())) > exact);
            }
        }
        assert_eq!(
            VectorComponentLimit::try_new(
                VectorDistanceMetric::Cosine,
                VectorDimension::try_new(3).unwrap()
            )
            .unwrap(),
            None
        );
    }

    #[test]
    fn exact_limits_are_accepted_and_adjacent_values_are_rejected() {
        for dimension in [1_usize, 15, 16, 17, 31, 32, 33, 1536] {
            let dimension = VectorDimension::try_new(dimension).unwrap();
            for metric in [
                VectorDistanceMetric::Euclidean,
                VectorDistanceMetric::Manhattan,
            ] {
                let limit = VectorComponentLimit::try_new(metric, dimension)
                    .unwrap()
                    .unwrap()
                    .inclusive_maximum();
                ValidatedMetricVector::try_from_vec(
                    vec![limit; dimension.get()],
                    metric,
                    dimension,
                )
                .unwrap();
                assert!(matches!(
                    ValidatedMetricVector::try_from_vec(
                        vec![next_up(limit); dimension.get()],
                        metric,
                        dimension
                    ),
                    Err(VectorValidationError::ComponentMagnitudeExceeded {
                        component_index: 0,
                        observed_magnitude,
                        inclusive_maximum,
                        ..
                    }) if observed_magnitude == next_up(limit) && inclusive_maximum == limit
                ));
            }
        }
    }

    #[test]
    fn validation_order_and_cosine_domain_are_explicit() {
        let dimension = VectorDimension::try_new(2).unwrap();
        assert!(matches!(
            ValidatedMetricVector::try_from_slice(
                &[0.0],
                VectorDistanceMetric::Euclidean,
                dimension
            ),
            Err(VectorValidationError::DimensionMismatch { .. })
        ));
        assert!(matches!(
            ValidatedMetricVector::try_from_slice(
                &[f32::NAN, 0.0],
                VectorDistanceMetric::Cosine,
                dimension
            ),
            Err(VectorValidationError::NonFiniteComponent { index: 0 })
        ));
        assert!(matches!(
            ValidatedMetricVector::try_from_slice(
                &[0.0, -0.0],
                VectorDistanceMetric::Cosine,
                dimension
            ),
            Err(VectorValidationError::ZeroNormCosineVector)
        ));
        let validated = ValidatedMetricVector::try_from_slice(
            &[f32::MAX, f32::MAX],
            VectorDistanceMetric::Cosine,
            dimension,
        )
        .unwrap();
        assert_eq!(validated.metric(), VectorDistanceMetric::Cosine);
        assert_eq!(validated.dimension(), dimension);
    }
}
