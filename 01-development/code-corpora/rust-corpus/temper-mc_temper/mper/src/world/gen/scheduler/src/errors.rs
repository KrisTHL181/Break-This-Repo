use thiserror::Error;

use crate::{JobKey, RequestId};

#[derive(Debug, Error)]
pub enum SchedulerError {
    #[error("unknown generation stage for job {0:?}")]
    UnknownStage(JobKey),
    #[error("unknown scheduler job {0:?}")]
    UnknownJob(JobKey),
    #[error("unknown scheduler request {0:?}")]
    UnknownRequest(RequestId),
}
