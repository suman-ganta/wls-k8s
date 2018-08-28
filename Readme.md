WLS Deployments:
  oic-wls-admin
    oic-image - startWeblogic.sh
    labels: tenant=t1, type=admin
  oic-wls-managed
    oic-image - /bin/bash -c "trap : TERM INT; sleep infinity & wait"
    labels: tenant=t1, type=managed

wls-controller

Config:
   tenantId -> adminUrl, user, password

Tasks:
On managed pod creation:
  1. Wait for oic wls pod with a specific labels tenant=t1, type=managed
  2. Create 
      a) machine for this pod on domain
      b) managed server on wls admin server for this domain
  3. start node manager
  4. start the server
  5. create ingress definition for this tenant

On managed pod deletion:
  1. Delete the server on admin server
  2. Delete machine
  3. delete ingress definition for this tenant

On Admin pod termination:
  ?? any house keeping? Otherwise, no-op

On admin pod creation:
  1. check if another admin pod exists with same tenant Label, if so ignore this pod
  2. Look-up all managed servers with label tenant=t1 and 
     a) create machine for each of them
     b) register them
  3. issue start on each of them
  4. Update adminUrl in the config.


WLS REST APIs
https://docs.oracle.com/middleware/1221/wls/WLRUR/examples.htm#WLRUR197

#start session
curl -u weblogic:weblogic1 -H X-Requested-By:MyClient -H Accept:application/json -H Content-Type:application/json -d "{}" \
-X POST http://localhost:7001/management/weblogic/latest/edit/changeManager/startEdit


---
Instructions
1. start admin pod
kubectl apply -f wls-test.yaml

Above command starts admin server on 7001

2. Update ms1.yaml with admin pod's ip address
3. start managed server pod
kubectl apply -f ms1.yaml

Above command connects to admin server and configures machine, managed server and starts the managed server

#Check the status of managed server
http://localhost:7001/management/weblogic/12.2.1.3.0/domainRuntime/serverRuntimes/wls-ms1 -u weblogic:welcome1

TODO:
1. Locate admin server pod ip based on pod labels
2. Push machine and managed server creation logic to wls-controller
3. Figure login problem via ingress/proxy
http://localhost:8001/api/v1/namespaces/wls1/services/http:wls-admin:/proxy/console
http://localhost:8001/api/v1/namespaces/wls1/services/http:wls-admin:/proxy/management/weblogic/12.2.1.3.0/domainRuntime/serverRuntimes/wls-ms1
curl https://www.sumanganta.com/wls-admin/management/weblogic/12.2.1.3.0/domainRuntime/serverRuntimes/wls-ms1 -v -u weblogic:welcome1 (works)
4. Create domain in the image using oic template
5. Add ingress route to the managed server


KUBE_TOKEN=$(</var/run/secrets/kubernetes.io/serviceaccount/token)
curl -sSk -H "Authorization: Bearer $KUBE_TOKEN" https://$KUBERNETES_SERVICE_HOST:$KUBERNETES_PORT_443_TCP_PORT/api/v1/namespaces/wls1/pods/$HOSTNAME
curl -sSk -H "Authorization: Bearer $KUBE_TOKEN" https://$KUBERNETES_SERVICE_HOST:$KUBERNETES_PORT_443_TCP_PORT/api/v1/namespaces/wls1/pods?labelSelector=tenant%3Dt1&fieldSelector=status

----

Demo
----
#docker image details
#manifests - images, labels

#scale
kubectl -n wls1 get pods
kubectl scale deployment wls-ms1 -n wls1 --replicas=3

#servers
curl https://www.sumanganta.com/wls-admin/management/weblogic/12.2.1.3.0/domainConfig/servers -u weblogic:welcome1 | jq .items[].identity

#machines
curl https://www.sumanganta.com/wls-admin/management/weblogic/12.2.1.3.0/domainConfig/servers -u weblogic:welcome1 | jq .items[].machine

#states
curl https://www.sumanganta.com/wls-admin/management/weblogic/12.2.1.3.0/domainRuntime/serverRuntimes/wls-ms1-8499c74977-j8q77 -u weblogic:welcome1 | jq .state

