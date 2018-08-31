##Installation Instructions
1. Install metacontroller
kubectl create clusterrolebinding <user>-cluster-admin-binding --clusterrole=cluster-admin --user=<user>@<domain>
# Create 'metacontroller' namespace, service account, and role/binding.
kubectl apply -f https://raw.githubusercontent.com/GoogleCloudPlatform/metacontroller/master/manifests/metacontroller-rbac.yaml
# Create CRDs for Metacontroller APIs, and the Metacontroller StatefulSet.
kubectl apply -f https://raw.githubusercontent.com/GoogleCloudPlatform/metacontroller/master/manifests/metacontroller.yaml
2. deploy wls-controller
kubectl apply -f wls-k8s/wls-controller/src/main/resources/wls-controller.yaml
3. deploy wls-admin
kubectl apply -f wls-k8s/oic-docker/k8s/wls-admin.yaml
4. deploy managed server
kubectl apply -f wls-k8s/oic-docker/k8s/ms.yaml
5. scale mananged servers
kubectl scale deployment wls-ms -n wls --replicas=2

##Defaults
1. Admin server creds weblogic:welcome1
2. Admin server port - 7001
3. Cluster: DockerCluster
4. Managed server port - 8001

#Check the status of managed server
http://localhost:7001/management/weblogic/latest/domainRuntime/serverRuntimes/wls-ms -u weblogic:welcome1

curl https://www.sumanganta.com/wls-admin/management/weblogic/latest/domainRuntime/serverRuntimes/wls-ms -v -u weblogic:welcome1

##TODO
Figure login problem via ingress/proxy

##servers
curl https://www.sumanganta.com/wls-admin/management/weblogic/12.2.1.3.0/domainConfig/servers -u weblogic:welcome1 | jq .items[].identity

##machines
curl https://www.sumanganta.com/wls-admin/management/weblogic/12.2.1.3.0/domainConfig/servers -u weblogic:welcome1 | jq .items[].machine

##states
curl https://www.sumanganta.com/wls-admin/management/weblogic/12.2.1.3.0/domainRuntime/serverRuntimes/wls-ms1-8499c74977-j8q77 -u weblogic:welcome1 | jq .state

Dev Notes
1. There are two images, wls-domain:12.2.1.3 and wls-controller:latest
2. first one is under oic-docker. Build it with build.sh
3. Next one is a maven module. build it with mvn clean install -Pdocker
4. Delete the pods to recreate with latest images. Delete deployments to restart.
5. Enable add-machine.py and add-server.py to revert to controller less coordination