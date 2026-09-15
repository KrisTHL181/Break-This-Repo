//! A font-database to keep track of fonts that we've located

use crate::locator::{FontDataSource, FontOrigin};
use crate::parser::{load_built_in_fonts, parse_and_collect_font_info, ParsedFont};
use anyhow::Context;
use config::{ConfigHandle, FontAttributes};
use rangeset::RangeSet;
use std::collections::{HashMap, HashSet};
use std::path::PathBuf;
use std::sync::{Mutex, OnceLock};

lazy_static::lazy_static! {
    /// Cache for font dir scan results, keyed by config generation plus the
    /// font_dirs list used for the scan. Entries persist for the lifetime of
    /// a config generation so that every FontConfiguration built under the
    /// same config (additional windows, DPI/scale changes) reuses the scan
    /// instead of re-walking the dirs on the main thread. A config reload
    /// bumps the generation, forcing exactly one rescan, which is what keeps
    /// fonts newly added to font_dirs discoverable via reload.
    static ref FONT_DIRS_CACHE: Mutex<Option<FontDirsCacheEntry>> = Mutex::new(None);
}

struct FontDirsCacheEntry {
    generation: usize,
    font_dirs: Vec<PathBuf>,
    font_info: Vec<ParsedFont>,
}

static BUILT_IN_CACHE: OnceLock<Vec<ParsedFont>> = OnceLock::new();

pub struct FontDatabase {
    by_full_name: HashMap<String, Vec<ParsedFont>>,
}

impl FontDatabase {
    pub fn new() -> Self {
        Self {
            by_full_name: HashMap::new(),
        }
    }

    fn load_font_info(&mut self, font_info: Vec<ParsedFont>) {
        for parsed in font_info {
            if let Some(path) = parsed.handle.path_str() {
                self.by_full_name
                    .entry(path.to_string())
                    .or_insert_with(Vec::new)
                    .push(parsed.clone());
            }
            self.by_full_name
                .entry(parsed.names().full_name.clone())
                .or_insert_with(Vec::new)
                .push(parsed);
        }
    }

    /// Scan the given font dirs and return the parsed font info.
    fn scan_font_dirs(font_dirs: &[PathBuf]) -> Vec<ParsedFont> {
        let mut font_info = vec![];
        for path in font_dirs {
            for entry in walkdir::WalkDir::new(path).into_iter() {
                let entry = match entry {
                    Ok(entry) => entry,
                    Err(_) => continue,
                };

                let source = FontDataSource::OnDisk(entry.path().to_path_buf());
                parse_and_collect_font_info(&source, &mut font_info, FontOrigin::FontDirs)
                    .map_err(|err| {
                        log::trace!("failed to read {:?}: {:#}", source, err);
                        err
                    })
                    .ok();
            }
        }
        font_info
    }

    /// Prewarm the font dir scan cache in a background thread.
    /// Safe to call concurrently: the cache is a Mutex<Option<...>>.
    pub fn prewarm_font_dirs(font_dirs: &[PathBuf], generation: usize) {
        let font_info = Self::scan_font_dirs(font_dirs);
        let mut cache = FONT_DIRS_CACHE.lock().unwrap();
        *cache = Some(FontDirsCacheEntry {
            generation,
            font_dirs: font_dirs.to_vec(),
            font_info,
        });
    }

    /// Build up the database from the fonts found in the configured font dirs
    /// and from the built-in selection of fonts
    pub fn with_font_dirs(config: &ConfigHandle) -> anyhow::Result<Self> {
        let generation = config.generation();
        let font_info = {
            let cache = FONT_DIRS_CACHE.lock().unwrap();
            match &*cache {
                Some(entry)
                    if entry.generation == generation && entry.font_dirs == config.font_dirs =>
                {
                    Some(entry.font_info.clone())
                }
                _ => None,
            }
        };

        let font_info = font_info.unwrap_or_else(|| {
            let font_info = Self::scan_font_dirs(&config.font_dirs);
            // Store the fresh scan so the other windows' config_changed
            // calls in this same reload cycle hit the cache instead of
            // each re-walking the dirs.
            let mut cache = FONT_DIRS_CACHE.lock().unwrap();
            *cache = Some(FontDirsCacheEntry {
                generation,
                font_dirs: config.font_dirs.clone(),
                font_info: font_info.clone(),
            });
            font_info
        });

        let mut db = Self::new();
        db.load_font_info(font_info);
        Ok(db)
    }

    pub fn list_available(&self) -> Vec<ParsedFont> {
        let mut fonts = vec![];
        for parsed_list in self.by_full_name.values() {
            for parsed in parsed_list {
                fonts.push(parsed.clone());
            }
        }
        fonts
    }

    pub fn with_built_in() -> anyhow::Result<Self> {
        let font_info = BUILT_IN_CACHE.get_or_init(|| {
            let mut info = vec![];
            if let Err(err) = load_built_in_fonts(&mut info) {
                log::error!("failed to load built-in fonts: {:#}", err);
            }
            info
        });
        let mut db = Self::new();
        db.load_font_info(font_info.clone());
        Ok(db)
    }

    pub fn resolve_multiple(
        &self,
        fonts: &[FontAttributes],
        handles: &mut Vec<ParsedFont>,
        loaded: &mut HashSet<FontAttributes>,
        pixel_size: u16,
    ) {
        for attr in fonts {
            if let Some(handle) = self.resolve(attr, pixel_size) {
                handles.push(handle.clone().synthesize(attr));
                loaded.insert(attr.clone());
            }
        }
    }

    /// Equivalent to FontLocator::locate_fallback_for_codepoints
    pub fn locate_fallback_for_codepoints(
        &self,
        codepoints: &[char],
    ) -> anyhow::Result<Vec<ParsedFont>> {
        let mut wanted_range = RangeSet::new();
        for &c in codepoints {
            wanted_range.add(c as u32);
        }

        let mut matches = vec![];

        for parsed_list in self.by_full_name.values() {
            for parsed in parsed_list {
                if parsed.names().family == "Last Resort High-Efficiency" {
                    continue;
                }
                let covered = parsed
                    .coverage_intersection(&wanted_range)
                    .with_context(|| format!("coverage_interaction for {:?}", parsed))?;
                if !covered.is_empty() {
                    matches.push(parsed.clone());
                }
            }
        }

        Ok(matches)
    }

    pub fn candidates(&self, font_attr: &FontAttributes) -> Vec<&ParsedFont> {
        let mut fonts = vec![];
        for parsed_list in self.by_full_name.values() {
            for parsed in parsed_list {
                if parsed.matches_name(font_attr) {
                    fonts.push(parsed);
                }
            }
        }
        fonts
    }

    pub fn resolve(&self, font_attr: &FontAttributes, pixel_size: u16) -> Option<&ParsedFont> {
        let mut candidates = vec![];
        for parsed_list in self.by_full_name.values() {
            for parsed in parsed_list {
                if parsed.matches_name(font_attr) {
                    candidates.push(parsed);
                }
            }
        }

        if let Some(idx) = ParsedFont::best_matching_index(font_attr, &candidates, pixel_size) {
            return candidates.get(idx).copied();
        }

        None
    }
}
