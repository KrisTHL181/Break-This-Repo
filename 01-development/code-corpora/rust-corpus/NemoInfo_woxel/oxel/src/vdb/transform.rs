#[derive(Debug)]
pub enum Map {
    UniformScaleMap {
        scale_values: cgmath::Vector3<f64>,
        voxel_size: cgmath::Vector3<f64>,
        scale_values_inverse: cgmath::Vector3<f64>,
        inv_scale_sqr: cgmath::Vector3<f64>,
        inv_twice_scale: cgmath::Vector3<f64>,
    },
    ScaleTranslateMap {
        translation: cgmath::Vector3<f64>,
        scale_values: cgmath::Vector3<f64>,
        voxel_size: cgmath::Vector3<f64>,
        scale_values_inverse: cgmath::Vector3<f64>,
        inv_scale_sqr: cgmath::Vector3<f64>,
        inv_twice_scale: cgmath::Vector3<f64>,
    },
}
