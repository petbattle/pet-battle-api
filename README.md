# pet-battle-api

A reactive back end for [pet battle](http://petbattle.app) based on the quarkus reactive mutiny framework with mongodb and panache. 

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
mvn compile quarkus:dev
```
OR
```
java -Dquarkus-profile=dev -jar ./target/pet-battle-api-1.0-SNAPSHOT-runner
```
OR
```
podman run -e QUARKUS_PROFILE=dev -e quarkus.mongodb.connection-string=mongodb://localhost:27017 quay.io/eformat/pet-battle-api:latest
```

See Makefile for container targets:
```
make podman-build
make podman-run
make podman-stop
make podman-push
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
helm repo add https://eformat.github.io/helm-charts
helm install pet-battle-api https://eformat.github.io/helm-charts/pet-battle-api-0.0.2.tgz 
```

`With mongodb replicaset` (TODO: replicaset commented out for now but works ok)

Get SCC UID for project
```bash
oc new-project cats
SCC=$(oc get project cats -o jsonpath='{.metadata.annotations.openshift\.io/sa\.scc\.uid-range}')
SCCUID=${SCC%%/*}

helm template foobar -f chart/values.yaml --set mongodb-replicaset.securityContext.fsGroup=$SCCUID --set mongodb-replicaset.securityContext.runAsUser=$SCCUID --set mongodb-replicaset.persistentVolume.storageClass=gp2 --set mongodb-replicaset.persistentVolume.size=1Gi chart | oc apply -f-
```

### Deploy mongodb and prebuilt application on OpenShift
```bash
oc new-project cats
oc new-app mongodb-persistent -p MONGODB_DATABASE=cats
oc apply -f src/main/kubernetes/install.yaml
```

### Build and Deploy on OpenShift using s2i.
```bash
oc new-project cats
oc new-app mongodb-persistent -p MONGODB_DATABASE=cats
oc new-build --name=cats quay.io/quarkus/ubi-quarkus-native-s2i:20.0.0-java8~https://github.com/eformat/pet-battle-api
oc new-app cats
oc expose svc cats
oc set env --from=secret/mongodb dc/cats
```

Note: the latest quarkus nightly build is available here https://oss.sonatype.org/content/repositories/snapshots - you may want to use a released version
```bash
<quarkus.version>999-SNAPSHOT</quarkus.version>
```

If local nexus deployed to OpenShift
```bash
oc set env bc/cats MAVEN_MIRROR_URL=http://nexus.nexus.svc.cluster.local:8081/repository/maven-public/
```

### Swagger available at
```bash
http://cats-cats.apps.<cluster-domain>/swagger-ui
```

Test
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
helm uninstall pet-battle-api
```

