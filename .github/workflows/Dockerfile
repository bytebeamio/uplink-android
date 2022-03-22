
FROM bitriseio/android-ndk:latest

SHELL ["/bin/bash", "-c"]

# Update deps
RUN apt-get update
RUN apt-get install -y \
    build-essential \
    curl \
    git

# Link min sdk version Android tools
ARG API=21
ENV API ${API}
ENV NDK_HOME_BIN ${ANDROID_NDK_HOME}/toolchains/llvm/prebuilt/linux-x86_64/bin
RUN ln -s ${NDK_HOME_BIN}/aarch64-linux-android${API}-clang ${NDK_HOME_BIN}/aarch64-linux-android-clang
RUN ln -s ${NDK_HOME_BIN}/armv7a-linux-androideabi${API}-clang ${NDK_HOME_BIN}/armv7a-linux-androideabi-clang
RUN ln -s ${NDK_HOME_BIN}/i686-linux-android${API}-clang ${NDK_HOME_BIN}/i686-linux-android-clang
RUN ln -s ${NDK_HOME_BIN}/x86_64-linux-android${API}-clang ${NDK_HOME_BIN}/x86_64-linux-android-clang

# Setup user
RUN useradd -ms /bin/bash rust
USER rust
ENV HOME /home/rust
ENV USER rust
ENV SHELL /bin/bash
WORKDIR /home/rust
SHELL ["/bin/bash", "-c"]

# Install Rust
RUN curl https://sh.rustup.rs -sSf | \
    sh -s -- --default-toolchain stable -y

# Setup additional Rust/Android env
ENV PATH=${HOME}/.cargo/bin:${PATH}
RUN echo 'source $HOME/.cargo/env' >> ${HOME}/.bashrc
RUN echo 'NDK_HOME=$ANDROID_NDK_HOME' >> ${HOME}/.bashrc
RUN echo 'CC=$NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/clang' >> ${HOME}/.bashrc
RUN echo 'AR=$NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-ar' >> ${HOME}/.bashrc
RUN echo 'PATH=$PATH:$NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin' >> ${HOME}/.bashrc

RUN source ~/.bashrc

# Install Android targets
RUN rustup target add armv7-linux-androideabi
RUN rustup target add aarch64-linux-android
RUN rustup target add i686-linux-android
RUN rustup target add x86_64-linux-android

RUN mkdir ${HOME}/application
WORKDIR ${HOME}/application
