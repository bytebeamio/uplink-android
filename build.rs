use flapigen::{JavaConfig, LanguageConfig};
use std::{env, path::Path};

fn main() {
    let out_dir = env::var("OUT_DIR").unwrap();
    let in_src = Path::new("src").join("java_glue.rs.in");
    let out_src = Path::new(&out_dir).join("java_glue.rs");

    let flap_gen = flapigen::Generator::new(
        LanguageConfig::JavaConfig(
            JavaConfig::new(
                Path::new("app")
                    .join("src")
                    .join("main")
                    .join("java")
                    .join("io")
                    .join("bytebeam")
                    .join("console"),
                "io.bytebeam.console".into(),
            )
        )
    ).rustfmt_bindings(true);

    flap_gen.expand("java bindings", &in_src, &out_src);
    println!("cargo:rerun-if-changed={}", in_src.display());
}
