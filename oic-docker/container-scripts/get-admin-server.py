help("modules")

import os, socket, urllib
import simplejson as json



# find admin server with the same tenantId and get its pod name
#KUBE_TOKEN=$(</var/run/secrets/kubernetes.io/serviceaccount/token)
#curl -sSk -H "Authorization: Bearer $KUBE_TOKEN" https://$KUBERNETES_SERVICE_HOST:$KUBERNETES_PORT_443_TCP_PORT/api/v1/namespaces/wls1/pods/$HOSTNAME
#curl -sSk -H "Authorization: Bearer $KUBE_TOKEN" https://$KUBERNETES_SERVICE_HOST:$KUBERNETES_PORT_443_TCP_PORT/api/v1/namespaces/wls1/pods?labelSelector=tenant%3Dt1&fieldSelector=status

host = os.environ.get('KUBERNETES_SERVICE_HOST', 'AdminServer')
port = os.environ.get('KUBERNETES_PORT_443_TCP_PORT', '443')
mypod = os.environ.get('HOSTNAME', 'localhost')

#get current pod's tenant
req = 'https://' + host + ':' + port + '/api/v1/namespaces/wls1/pods/' + mypod

#get auth token
tokenFile = open('/var/run/secrets/kubernetes.io/serviceaccount/token', 'r')
token = tokenfile.read()
tokenFile.close()

print('token     : [%s]' % token);
headers = { 'Authorization' : 'Bearer ' + token }

#create no-op ssl context
#context = ssl._create_unverified_context()
req = urllib.request.Request(url, headers=headers)
f = urllib.urlopen(req)
values = json.load(f)
f.close()
tenant = values['metadata']['labels']['tenant']
print('tenant     : [%s]' % tenant)

#find out admin server for this tenant
req = 'https://' + host + ':' + port + '/api/v1/namespaces/wls1/pods?' + 'labelSelector=tenant%3D' + tenant + ',app%3Dwls-admin'
req = urllib.request.Request(url, headers=headers)
f = urllib.urlopen(req)
values = json.load(f)
f.close()
items = values['items']
adminServer = ''
for item in items:
    adminServer = item['metadata']['name']
print('adminServer     : [%s]' % adminServer)
#set it as env variable
os.environ['ADMIN_HOST'] = adminServer

# Exit
# =========
exit()