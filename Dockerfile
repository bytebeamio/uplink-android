FROM mingc/android-build-box:latest

RUN curl --retry 3 -sSfL https://sh.rustup.rs -o rustup-init.sh
RUN sh rustup-init.sh -y
RUN rm rustup-init.sh

ENV PATH="/root/.cargo/bin:$PATH"

RUN cargo install cargo-ndk
