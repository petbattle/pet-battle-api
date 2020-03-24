# pet-battle-api

```bash
oc new-project cats
oc new-app mongodb-persistent -p MONGODB_DATABASE=cats
oc new-build --name=cats quay.io/quarkus/ubi-quarkus-native-s2i:20.0.0-java8~https://github.com/eformat/pet-battle-api
oc new-app cats
oc expose svc cats
oc set env --from=secret/mongodb dc/cats
```

If local nexus deployed to OpenShift
```
oc set env bc/cats MAVEN_MIRROR_URL=http://nexus.nexus.svc.cluster.local:8081/repository/maven-public/
```

Swagger available at
```
http://cats-cats.apps.<cluster-domain>/swagger-ui
```

Test
```
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
curl -s -s -H "Content-Type: application/json" -X GET "http://${HOST}/cats/datatable?draw=1&start=0&length=10&search\[value\]=" | jq
```

Prometheus & Grafana metrics endpoint
```git exclude
curl http://${HOST}/metrics/application
```

```
oc create configmap prom --from-file=prometheus.yml=src/main/kubernetes/prometheus.yml
oc new-app prom/prometheus && oc expose svc/prometheus
oc set volume dc/prometheus --add -t configmap --configmap-name=prom -m /etc/prometheus/prometheus.yml --sub-path=prometheus.yml
oc rollout status -w dc/prometheus
oc new-app grafana/grafana && oc expose svc/grafana
oc rollout status -w dc/grafana
```
