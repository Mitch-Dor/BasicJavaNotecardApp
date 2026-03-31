# --- STEP 1: Base image ---
# Using Maven 3.9 with JDK 17 (matching your Adoptium version)
FROM maven:3.9.4-eclipse-temurin-17

# --- STEP 2: System dependencies ---
RUN apt-get update && apt-get install -y --no-install-recommends \
    git \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# --- STEP 4: Copy the entire codebase ---
COPY . /app

# --- STEP 3b: Pre-download dependencies (Maven) ---
# This ensures the container works offline by caching plugins and dependencies
RUN mvn dependency:resolve -q
RUN mvn test -Dtest=XDUMMY -Dsurefire.failIfNoSpecifiedTests=false -q || true

# --- Pre-edit state capture ---
# DO NOT REMOVE THIS BLOCK.
RUN git config --global user.email "worker@sweagent" && \
    git config --global user.name "worker" && \
    git init && git add -A && git commit -m "pre-edit"

CMD ["/bin/bash"]