[![Build containers](https://github.com/petbattle/pet-battle-api/actions/workflows/build_container.yaml/badge.svg?branch=master)](https://github.com/petbattle/pet-battle-api/actions/workflows/build_container.yaml)
[![Code scanning - action](https://github.com/petbattle/pet-battle-api/actions/workflows/codeql.yml/badge.svg?branch=master)](https://github.com/petbattle/pet-battle-api/actions/workflows/codeql.yml)

[![button](https://raw.githubusercontent.com/eformat/launch-service/master/static/img/launchbutton_light.svg)](https://github.com/petbattle/pet-battle-api/actions/workflows/oc_deploy.yaml)

# pet-battle-api

A reactive back end for [pet battle](https://github.com/petbattle/pet-battle) based on the quarkus reactive mutiny framework with mongodb and panache. 

### Develop

#### for the impatient

```
make podman-run
```
 
#### local commands

Database:
```
podman run --name mongo -p 27017:27017 mongo:latest
```

Application:
```bash
mvn -s settings.xml quarkus:dev
```
OR
```
java -Dquarkus-profile=dev -jar ./target/pet-battle-api-1.0-SNAPSHOT-runner
```
OR
```
podman run -e QUARKUS_PROFILE=dev -e quarkus.mongodb.connection-string=mongodb://localhost:27017 quay.io/petbattle/pet-battle-api:latest
```

OR we can also run a podman `pod` as opposed to single containers:
```
podman run -d --pod new:pb -p 27017:27017 -p8080:8080 -p8443:8443 mongo:latest
podman run -d --pod pb -e quarkus.mongodb.connection-string=mongodb://localhost:27017 quay.io/eformat/pet-battle-api:latest
```

See Makefile for container targets:
```
make podman-build
make podman-run
make podman-stop
make podman-push
```

#### Jkube remote-dev OpenShift

Create a project

```bash
oc new-project cats 
```

Deploy MongoDB

```bash
oc apply -f mongodb-persistent.yml
oc new-app mongodb-persistent -p MONGODB_DATABASE=cats -p MONGODB_USER=catuser -p MONGODB_PASSWORD=password -p MONGODB_ADMIN_PASSWORD=password 
```

Build and deploy application on OpenShift using Jkube-s2i

```bash
mvn clean package oc:build oc:resource oc:apply
```

Start the jkube remote service in OpenShift (this will port-forward to localhost)

```bash
mvn oc:remote-dev
```

Start developing locally (but connected to remote environment!) we use a profile to setup DB connection as if it were local

```bash
mvn quarkus:dev -Dquarkus.profile=jkube
```

### Helm3

New project
```
oc new-project cats
```

`Using the mongo-persistent template` on OpenShift
```
helm template --dependency-update cats -f chart/values.yaml chart | oc apply -f-
```

OR from deployed chart
```
helm repo add https://petbattle.github.io/helm-charts
helm install pet-battle-api https://petbattle.github.io/helm-charts/pet-battle-api-1.1.0.tgz 
```

`With mongodb replicaset` (TODO: replicaset commented out for now but works ok)

Get SCC UID for project
```bash
oc new-project cats
SCC=$(oc get project cats -o jsonpath='{.metadata.annotations.openshift\.io/sa\.scc\.uid-range}')
SCCUID=${SCC%%/*}

helm template foobar -f chart/values.yaml --set mongodb-replicaset.securityContext.fsGroup=$SCCUID --set mongodb-replicaset.securityContext.runAsUser=$SCCUID --set mongodb-replicaset.persistentVolume.storageClass=gp2 --set mongodb-replicaset.persistentVolume.size=1Gi chart | oc apply -f-
```

### Deploy to OpenShift using a JAR

OpenShift has a cool feature where you can easily deploy an appl in development using [a JAR file](http://openshift.github.io/openshift-origin-design/designs/developer/4.8/upload-jar-file/)

There are two artifacts you will need in the [Release](https://github.com/petbattle/pet-battle-api/releases) folder

1. From `Developer` perspective `Add` YAML file - `mongodb-persistent.yml` - drag-n-drop this to create the MongoDB template.
2. Instantiate the mongo database instance, use `catuser`, `password` and `cats` as the database parameters:
![images/drag-n-drop-mongo.png](images/drag-n-drop-mongo.png)
3. Import JDK 17 image builder if it does not exist:
```bash
oc -n openshift import-image java:openjdk-17 --from=registry.access.redhat.com/ubi8/openjdk-17:latest --confirm
oc -n openshift annotate istag java:openjdk-17 supports='java:17,java' tags='builder,java,openjdk'
```
4. From `Topology` view drag-n-drop the `pet-battle-api-<version>-runner.jar` to build and create the app deployment. Use the `openjdk-17` Builder image version drop down imported in (3)
![images/drag-n-drop-app.png](images/drag-n-drop-app.png)
5. Wait for application to deploy and try it out.

### Build and Deploy on OpenShift using s2i.

Deploy Mongo
```bash
oc new-project cats
oc apply -n openshift -f mongodb-persistent.yml
oc new-app mongodb-persistent -p MONGODB_DATABASE=cats
```

We are going to use a 2-step build process - see [here](https://eformat.github.io/ubi-mvn-builder) for more details.

Create the builder image to build our code.
```bash
oc new-build --name=cats-build \
  quay.io/eformat/ubi-mvn-builder:latest~https://github.com/petbattle/pet-battle-api \
  -e MAVEN_BUILD_OPTS="-Dquarkus.package.type=fast-jar -DskipTests" \
  -e MAVEN_CLEAR_REPO="true"
```

(optional) If local nexus deployed to OpenShift, use this env.var
```bash
oc set env bc/cats-build MAVEN_MIRROR_URL=http://nexus.nexus.svc.cluster.local:8081/repository/maven-public/
```

Once complete, create the runtime image.
```bash
oc new-build --name=cats \
  --build-arg BUILD_IMAGE=image-registry.openshift-image-registry.svc:5000/$(oc project -q)/cats-build:latest \
  --strategy docker --dockerfile - < Dockerfile.s2i
```

Set triggers in case we change the build image:
```bash
oc set triggers bc/cats --from-image=$(oc project -q)/cats-build:latest
```

Deploy the app:
```bash
oc new-app cats \
  -e DATABASE_SERVICE_HOST=mongodb \
  -e DATABASE_SERVICE_PORT=27017 \
  -e DATABASE_NAME=cats
oc set env --from=secret/mongodb deployments/cats
oc expose svc/cats
oc patch route/cats \
      --type=json -p '[{"op":"add", "path":"/spec/tls", "value":{"termination":"edge","insecureEdgeTerminationPolicy":"Redirect"}}]'
```

Swagger is available at:

```bash
http://cats-cats.apps.<cluster-domain>/swagger-ui 
```

### Testing

Run the unit tests.

```bash
mvn clean test
```

> **NOTE:** When running jmeter you need to have the pet-battle-api service is up running.

Run the unit and jmeter performance tests.

```bash
mvn clean verify
```

Run just the jmeter performance test.

```bash
mvn jmeter:jmeter
```

Run the jmeter performance test in the gui.

```bash
mvn jmeter:configure jmeter:gui -DguiTestFile=src/test/jmeter/pet-battle-api-jmeter.jmx
```

View test coverage report
```bash
xdg-open target/jacoco-report/index.html
```

Test manually
```bash
export CATID=5e69e003a765314bf6d04281
export HOST=0.0.0.0:8080

curl -s -H "Content-Type: application/json" -X GET http://${HOST}/cats | jq .
curl -s -H "Content-Type: application/json" -X GET http://${HOST}/cats/ids | jq .
curl -s -H "Content-Type: application/json" -X GET http://${HOST}/cats/count | jq .
curl -s -H "Content-Type: application/json" -X GET http://${HOST}/cats/${CATID} | jq .
curl -s -H "Content-Type: application/json" -X GET http://${HOST}/cats/${CATID} | jq ".id"
curl -s -H "Content-Type: application/json" -X GET http://${HOST}/cats/${CATID} | jq ".count"
curl -s -H "Content-Type: application/json" -X DELETE http://${HOST}/cats/${CATID}
curl -s -H "Content-Type: application/json" -X PUT http://${HOST}/cats/${CATID}
curl -s -H "Content-Type: application/json" -X GET http://${HOST}/cats/${CATID} | jq ".image" | sed -e 's|"||g' | base64 -d > ~/Pictures/foo.png
curl -s -H "Content-Type: application/json" -X GET http://${HOST}/cats/topcats | jq ".[].count"
curl -s -H "Content-Type: application/json" -X DELETE http://${HOST}/cats/kittykiller
curl -s -H "Content-Type: application/json" -X GET "http://${HOST}/cats/datatable?draw=1&start=0&length=10&search\[value\]=" | jq
curl -s -H "Content-Type: application/json" -X GET http://${HOST}/cats/loadlitter
```

### Prometheus & Grafana metrics endpoint
```bash
curl http://${HOST}/metrics/application
```

```bash
oc create configmap prom --from-file=prometheus.yml=src/main/kubernetes/prometheus.yml
oc apply -f src/main/kubernetes/prom-rbac.yaml
oc new-app prom/prometheus && oc expose svc/prometheus
oc set volume dc/prometheus --add -t configmap --configmap-name=prom -m /etc/prometheus/prometheus.yml --sub-path=prometheus.yml
oc rollout status -w dc/prometheus
oc new-app grafana/grafana && oc expose svc/grafana
oc rollout status -w dc/grafana
```

### Delete application (not mongodb)
```bash
oc delete dc,svc,route,is -lapp=cats
```
OR
```git exclude
helm delete pet-battle-api
```

### Signature

The public key of [pet-battle-api images](https://quay.io/repository/petbattle/pet-battle-api)

[Cosign](https://github.com/sigstore/cosign) public key:

```shell
-----BEGIN PUBLIC KEY-----
MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEpQLELVwfM8hcPxqY5xBk5sGdjFDi
dFFy7WrlLfd+YG0NzA/RM3D9uQrUYEjPcU5hH8cBoe7AjVg/k/qO58+Qjg==
-----END PUBLIC KEY-----
```

The public key is also available online: <https://raw.githubusercontent.com/petbattle/pet-battle-api/master/cosign.pub>

To verify an image:

```shell
curl --progress-bar -o cosign.pub https://raw.githubusercontent.com/petbattle/pet-battle-api/master/cosign.pub
cosign verify --key cosign.pub quay.io/petbattle/pet-battle-api:latest
```

SBOM generated using [syft](https://github.com/anchore/syft). 

Verify signed SBOM:

```bash
cosign verify --key cosign.pub --attachment sbom quay.io/petbattle/pet-battle-api:latest
```

Verify SBOM attestation:

```bash
cosign verify-attestation --key cosign.pub quay.io/petbattle/pet-battle-api:latest
```

You may also like to use:

```bash
cosign tree quay.io/petbattle/pet-battle-api:latest
📦 Supply Chain Security Related artifacts for an image: quay.io/petbattle/pet-battle-api:latest
└── 💾 Attestations for an image tag: quay.io/petbattle/pet-battle-api:sha256-ce4f9101b2bf09e89605012c3eb6a4bbeaaee82cd51c1af55a4b4f622b11d504.att
   └── 🍒 sha256:c61c9c15f782670c6e521a4f09482ced2854387d6c5fa39378c37d296ff3a181
└── 🔐 Signatures for an image tag: quay.io/petbattle/pet-battle-api:sha256-ce4f9101b2bf09e89605012c3eb6a4bbeaaee82cd51c1af55a4b4f622b11d504.sig
   └── 🍒 sha256:d45e1aa311163335366032bf901eea430c72eb870e386c1359e4ae17bfc4af3d
└── 📦 SBOMs for an image tag: quay.io/petbattle/pet-battle-api:sha256-ce4f9101b2bf09e89605012c3eb6a4bbeaaee82cd51c1af55a4b4f622b11d504.sbom
   └── 🍒 sha256:3cd8b8f7f181414447e1035ee681aebd41a1d3608e8eaecf2c516d36ee4df45c
```
