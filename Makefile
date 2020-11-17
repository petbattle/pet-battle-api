# Image URL to use all building/pushing image targets
REGISTRY ?= quay.io
REPOSITORY ?= $(REGISTRY)/petbattle/pet-battle-api

IMG := $(REPOSITORY):latest

# Native image compile - FIXME
# Image Scaling (Scalr.java)
# native compile works, but does not run
# broken because Image.io not in graal yet - https://github.com/quarkusio/quarkus/issues/8605
# -P native
# Dockerfile.native

# clean compile
compile:
	mvn clean package -DskipTests

# Podman Login
podman-login:
	@podman login -u $(DOCKER_USER) -p $(DOCKER_PASSWORD) $(REGISTRY)

# Build the oci image no compile
podman-build-nocompile:
	podman build --no-cache . -t ${IMG} -f Dockerfile.jvm

# Build the oci image
podman-build: compile
	podman build . -t ${IMG} -f Dockerfile.jvm

# Push the oci image
podman-push: podman-build
	podman push ${IMG}

# Push the oci image
podman-push-nocompile: podman-build-nocompile
	podman push ${IMG}

# Just Push the oci image
podman-push-nobuild:
	podman push ${IMG}

podman-run:
	podman-compose -f docker-compose.yml up -d

podman-stop:
	podman-compose -f docker-compose.yml down
