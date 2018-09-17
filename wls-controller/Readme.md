##Summary
This is metacontroller lambda function to keep admin server in sync with managed server pods.

Currently it supports the following -
Upon creation of new managed pod, it gets registered to admin server and brought up.
Registration involves -
1. Locating admin server
2. Creating a machine
3. Configure node manager

managed server pod takes care of bringing up node manager.

##TODO:
1. On managed server deletion, remove it from admin server domain.


##References
WLS REST APIs
https://docs.oracle.com/middleware/1221/wls/WLRUR/examples.htm#WLRUR197
