This is metacontroller lambda function to keep admin server in sync with managed server pods.

Currently it supports the following -
Upon creation of new managed pod, it gets registered to admin server and brought up.
Registration involves -
1. Locating admin server
2. Creating a machine
3. Configure node manager

managed server pod takes care of bringing up node manager.

TODO:
1. Wait for admin server to come up
2. If admin pod is not found, return
3. If admin server is created, locate all managed servers and register them
4. On managed server deletion, remove it from admin server domain.
