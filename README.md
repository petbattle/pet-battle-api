# mongodb-panache-quickstart

```bash
oc new-build --name=cats quay.io/quarkus/ubi-quarkus-native-s2i:19.3.1-java8~https://github.com/eformat/mongodb-panache-quickstart
oc new-app imagestream.image.openshift.io/cats
oc new-app cats
oc expose svc cats
oc new-app mongodb-persistent -p MONGODB_DATABASE=cats 
oc set env --from=secret/mongodb dc/cats
```

If local nexus deployed to OpenShift
```
oc set env bc/cats MAVEN_MIRROR_URL=http://nexus.nexus.svc.cluster.local:8081/repository/maven-public/
```
