#!/bin/bash
#
#Copyright (c) 2014-2018 Oracle and/or its affiliates. All rights reserved.
#
#Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
#
# This script will wait until Admin Server is available.
# There is no timeout!
#
echo "Waiting for WebLogic Admin Server on $ADMIN_HOST:$ADMIN_PORT to become available..."
while :
do
  echo "** Waiting for Admin Server to Start **"
  result=$( curl -s --noproxy $ADMIN_HOST -w "%{http_code}" http://$ADMIN_HOST:$ADMIN_PORT/ -o /dev/null )
  if [[ $result -eq '404' ]]; then
    break
  fi
  sleep 20
done
