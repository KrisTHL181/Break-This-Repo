use std::path::PathBuf;

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let mut prost = prost_build::Config::new();
    prost.protoc_executable(protoc_bin_vendored::protoc_bin_path()?);
    tonic_prost_build::configure()
        .bytes(".helixdb.server.v1.QueryJsonRequest.body")
        .bytes(".helixdb.server.v1.QueryJsonResponse.body")
        .compile_with_config(
            prost,
            &[PathBuf::from("proto/helixdb.proto")],
            &[PathBuf::from("proto"), protoc_bin_vendored::include_path()?],
        )?;
    Ok(())
}
