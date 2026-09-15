#![cfg_attr(not(feature = "std"), no_std)]
#![allow(clippy::redundant_static_lifetimes)]
pub mod emoji;
pub mod emoji_presentation;
pub mod emoji_variation;
pub mod nerdfonts;
mod nerdfonts_data;
pub mod white_space;
pub mod widechar_width;
