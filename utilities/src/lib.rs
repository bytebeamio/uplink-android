use std::fmt::{Display, Formatter};
use std::io::Write;

pub struct LazyFile {
    path: String
}

impl LazyFile {
    pub fn new(path: &str) -> LazyFile {
        LazyFile { path: path.to_string() }
    }

    pub fn exists(&self) -> bool {
        std::path::Path::new(self.path.as_str()).exists()
    }

    pub fn is_file(&self) -> bool {
        std::path::Path::new(self.path.as_str()).is_file()
    }

    pub fn size(&self) -> u64 {
        std::fs::metadata(self.path.as_str()).unwrap().len()
    }

    pub fn name(&self) -> &str {
        self.path.as_str()
    }

    pub fn create(&self) {
            std::fs::File::create(self.path.as_str()).unwrap();
    }

    pub fn delete(&self) {
        if self.exists() {
            std::fs::remove_file(self.path.as_str()).unwrap();
        }
    }

    pub fn append_line(&self, line: &str) {
        if !self.exists() {
            self.create();
        }
        let mut fd = std::fs::OpenOptions::new().append(true).open(self.path.as_str()).unwrap();
        fd.write(line.as_bytes()).unwrap();
        fd.write(&['\n' as u8]).unwrap();
    }
}

impl Display for LazyFile {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        write!(f, "{}", self.path)
    }
}