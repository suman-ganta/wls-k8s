# Installation Instructions
* Install metacontroller
`kubectl create clusterrolebinding <user>-cluster-admin-binding --clusterrole=cluster-admin --user=<user>@<domain>`
* Create 'metacontroller' namespace, service account, and role/binding.
`kubectl apply -f https://raw.githubusercontent.com/GoogleCloudPlatform/metacontroller/master/manifests/metacontroller-rbac.yaml`
* Create CRDs for Metacontroller APIs, and the Metacontroller StatefulSet.
`kubectl apply -f https://raw.githubusercontent.com/GoogleCloudPlatform/metacontroller/master/manifests/metacontroller.yaml`
* deploy wls-controller
`kubectl apply -f wls-k8s/wls-controller/src/main/resources/wls-controller.yaml`
* deploy wls-admin
`kubectl apply -f wls-k8s/oic-docker/k8s/wls-admin.yaml`
* deploy managed server
`kubectl apply -f wls-k8s/oic-docker/k8s/ms.yaml`
* scale mananged servers
`kubectl scale deployment wls-ms -n wls --replicas=2`

## Defaults
1. Admin server creds weblogic:welcome1
2. Admin server port - 7001
3. Cluster: DockerCluster
4. Managed server port - 8001

## Servers and Machines
`curl -k -u weblogic:welcome1 'https://www.sumanganta.com/wls-admin/management/weblogic/latest/domainConfig/servers?links=none&fields=machine,identity' | jq`

## States
`curl -k -u weblogic:welcome1 'https://www.sumanganta.com/wls-admin/management/weblogic/latest/domainRuntime/serverLifeCycleRuntimes?links=none&fields=name,state' | jq`

## TODO
Figure login problem via ingress/proxy

### Dev Notes
1. There are two images, wls-domain:12.2.1.3 and wls-controller:latest
2. first one is under oic-docker. Build it with build.sh
3. Next one is a maven module. build it with mvn clean install -Pdocker
4. Delete the pods to recreate with latest images. Delete deployments to restart.
5. Enable add-machine.py and add-server.py to revert to controller less coordination
