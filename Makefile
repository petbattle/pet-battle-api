# Image URL to use all building/pushing image targets
REGISTRY ?= quay.io
REPOSITORY ?= $(REGISTRY)/eformat/pet-battle-api

IMG := $(REPOSITORY):latest

# clean compile
compile:
	mvn clean package -Pnative -DskipTests

# Podman Login
podman-login:
	@podman login -u $(DOCKER_USER) -p $(DOCKER_PASSWORD) $(REGISTRY)

# Build the oci image no compile
podman-build-nocompile:
	podman build --no-cache . -t ${IMG} -f Dockerfile.native

# Build the oci image
podman-build: compile
	podman build . -t ${IMG} -f Dockerfile.native

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
	podman run -d --pod new:pb -p 27017:27017 -p8080:8080 -p8443:8443 --name mongo mongo:latest
	podman run -d --pod pb -e quarkus.mongodb.connection-string=mongodb://localhost:27017 --name pet-battle-api quay.io/eformat/pet-battle-api:latest

podman-stop:
	podman pod stop pb
	podman pod rm pb
