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
```
