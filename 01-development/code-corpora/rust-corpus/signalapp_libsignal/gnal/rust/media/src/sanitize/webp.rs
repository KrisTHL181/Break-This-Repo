//
// Copyright 2023 Signal Messenger, LLC.
// SPDX-License-Identifier: AGPL-3.0-only
//

pub use webpsan::parse::ParseError;

pub fn sanitize<R: std::io::Read + webpsan::Skip>(input: R) -> Result<(), Error> {
    Ok(webpsan::sanitize_with_config(
        input,
        webpsan::Config {
            allow_unknown_chunks: true,
        },
    )?)
}

/// Error type returned by [`sanitize`].
pub type Error = super::error::SanitizerError<ParseError>;

/// A decomposed and stringified [`error_stack::Report<ParseError>`](mediasan_common::Error::Parse).
pub type ParseErrorReport = super::error::ParseErrorReport<ParseError>;
