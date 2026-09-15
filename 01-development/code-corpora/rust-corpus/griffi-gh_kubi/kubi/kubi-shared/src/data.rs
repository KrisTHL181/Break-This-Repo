use std::{
  mem::size_of,
  fs::{File, OpenOptions},
  io::{Read, Seek, SeekFrom, Write},
  path::Path,
  borrow::Cow,
  sync::{Arc, RwLock}
};
use num_enum::TryFromPrimitive;
use serde::{Serialize, Deserialize};
use glam::IVec3;
use hashbrown::HashMap;
use anyhow::Result;
use shipyard::Unique;
use static_assertions::const_assert_eq;
use crate::{
  block::Block,
  chunk::{CHUNK_SIZE, BlockDataRef, BlockData}
};

pub mod io_thread;

const SECTOR_SIZE: usize = CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE * size_of::<Block>();
const RESERVED_SIZE: usize = 1048576; //~1mb (16 sectors assuming 32x32x32 world of 1byte blocks)
const RESERVED_SECTOR_COUNT: usize = RESERVED_SIZE / SECTOR_SIZE;

//magic = "KUBI" + IDENTITY (4 bytes)
const SUBHEADER_SIZE: usize = 8;
const SUBHEADER_MAGIC: [u8; 4] = *b"KUBI";
const SUBHEADER_IDENTITY: u32 = 1;

// #[repr(transparent)]
// struct IVec3Hash(IVec3);
#[derive(Serialize, Deserialize)]
pub struct WorldSaveDataHeader {
  pub name: Cow<'static, str>,
  pub seed: u64,
  sector_count: u32,
  chunk_map: HashMap<IVec3, u32>,
}

impl Default for WorldSaveDataHeader {
  fn default() -> Self {
    Self {
      name: "World".into(),
      seed: 0,
      sector_count: RESERVED_SECTOR_COUNT as u32,
      chunk_map: HashMap::new()
    }
  }
}

pub type SharedHeader = Arc<RwLock<WorldSaveDataHeader>>;

#[derive(Unique)]
pub struct WorldSaveFile {
  pub file: File,
  pub header: SharedHeader,
}

impl WorldSaveFile {
  pub fn new(file: File) -> Self {
    WorldSaveFile {
      file,
      header: Arc::new(RwLock::new(WorldSaveDataHeader::default())),
    }
  }

  fn read_header(&mut self) -> Result<()> {
    self.file.rewind()?;

    let mut subheader = [0u8; SUBHEADER_SIZE];
    self.file.read_exact(&mut subheader)?;
    if subheader[0..4] != SUBHEADER_MAGIC {
      return Err(anyhow::anyhow!("invalid file header"));
    }
    if subheader[4..8] != SUBHEADER_IDENTITY.to_be_bytes() {
      return Err(anyhow::anyhow!("this save file cannot be loaded by this version of the game"));
    }

    let limit = (RESERVED_SIZE - SUBHEADER_SIZE) as u64;
    *self.header.write().unwrap() = bincode::deserialize_from((&self.file).take(limit))?;

    Ok(())
  }

  fn write_header(&mut self) -> Result<()> {
    self.file.rewind()?;
    self.file.write_all(&SUBHEADER_MAGIC)?;
    self.file.write_all(&SUBHEADER_IDENTITY.to_be_bytes())?;
    //XXX: this can cause the header to destroy chunk data (if it's WAY too long)
    //     read has checks against this, but write doesn't
    //     1mb is pretty generous tho, so it's not a *big* deal
    bincode::serialize_into(&self.file, &*self.header.read().unwrap())?;
    Ok(())
  }

  pub fn initialize(&mut self) -> Result<()> {
    self.write_header()?;
    Ok(())
  }

  pub fn load_data(&mut self) -> Result<()> {
    self.read_header()?;
    Ok(())
  }

  // fn allocate_sector(&mut self) -> u32 {
  //   let mut lock = self.header.write().unwrap();
  //   let value = lock.sector_count + 1;
  //   lock.sector_count += 1;
  //   value
  // }

  pub fn save_chunk(&mut self, position: IVec3, data: &BlockDataRef) -> Result<()> {
    let mut header_lock = self.header.write().unwrap();

    let mut header_modified = false;
    let sector = header_lock.chunk_map.get(&position).copied().unwrap_or_else(|| {
      header_modified = true;
      let sector = header_lock.sector_count;
      header_lock.sector_count += 1;
      header_lock.chunk_map.insert(position, sector);
      sector
      // self.allocate_sector()
    });

    drop(header_lock);

    let offset = sector as u64 * SECTOR_SIZE as u64;

    const_assert_eq!(size_of::<Block>(), 1);
    let data: &[u8; SECTOR_SIZE] = unsafe { std::mem::transmute(data) };

    self.file.seek(SeekFrom::Start(offset))?;
    self.file.write_all(data)?;

    if header_modified {
      self.write_header()?;
    }
    self.file.sync_data()?;
    Ok(())
  }

  ///TODO partial chunk commit (No need to write whole 32kb for a single block change!)
  pub fn chunk_set_block() {
    todo!()
  }

  pub fn chunk_exists(&self, position: IVec3) -> bool {
    self.header.read().unwrap().chunk_map.contains_key(&position)
  }

  pub fn load_chunk(&mut self, position: IVec3) -> Result<Option<BlockData>> {
    let Some(&sector) = self.header.read().unwrap().chunk_map.get(&position) else {
      return Ok(None);
    };

    let mut buffer = Box::new([0u8; CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE * size_of::<Block>()]);
    let offset = sector as u64 * SECTOR_SIZE as u64;

    self.file.seek(SeekFrom::Start(offset))?;
    self.file.read_exact(&mut buffer[..])?;

    //should be safe under these conditions:
    //Block is a single byte
    //All block data bytes are in valid range
    const_assert_eq!(size_of::<Block>(), 1);
    for &byte in &buffer[..] {
      let block = Block::try_from_primitive(byte);
      match block {
        //Sanity check, not actually required: (should NEVER happen)
        Ok(block) => debug_assert_eq!(byte, block as u8),
        Err(_) => anyhow::bail!("invalid block data"),
      }
    }
    let data: BlockData = unsafe { std::mem::transmute(buffer) };

    Ok(Some(data))
  }

  pub fn get_shared_header(&self) -> SharedHeader {
    Arc::clone(&self.header)
  }
}

/// Utility function to open a local save file, creating it if it doesn't exist
pub fn open_local_save_file(path: &Path) -> Result<WorldSaveFile> {
  let mut save_file = WorldSaveFile::new({
    OpenOptions::new()
      .read(true)
      .write(true)
      .create(true)
      .truncate(false)
      .open(path)?
  });
  if save_file.file.metadata().unwrap().len() == 0 {
    save_file.initialize()?;
  } else {
    save_file.load_data()?;
  }
  Ok(save_file)
}

