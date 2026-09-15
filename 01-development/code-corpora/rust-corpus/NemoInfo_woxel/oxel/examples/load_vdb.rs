use std::io::BufReader;

extern crate woxel;
use woxel::vdb::VdbReader;

fn main() {
    let f = std::fs::File::open("assets/utahteapot.vdb").unwrap();
    let mut vdb_reader = VdbReader::new(BufReader::new(f)).unwrap();
    let vdb1 = vdb_reader.read_vdb345_grid::<f32>("ls_utahteapot").unwrap();
    // vdb1.compute_sdf();
    let atlas1 = vdb1.atlas();
    let masks1 = vdb1.masks();

    for _ in 0..5 {
        let vdb2 = vdb_reader.read_vdb345_grid::<f32>("ls_utahteapot").unwrap();
        // vdb2.compute_sdf();
        let atlas2 = vdb2.atlas();
        let masks2 = vdb2.masks();

        assert_eq!(vdb1, vdb2);
        assert_eq!(atlas1, atlas2);
        assert_eq!(masks1, masks2);
    }
}
