# mongodb-panache-quickstart

```bash
oc new-build --name=cats quay.io/eformat/quarkus-native-s2i-ubi:latest~https://github.com/eformat/mongodb-panache-quickstart
oc new-app imagestream.image.openshift.io/cats
oc new-app cats
oc expose svc cats
oc new-app mongodb-persistent -p MONGODB_DATABASE=cats 
oc set env --from=secret/mongodb dc/cats
```
