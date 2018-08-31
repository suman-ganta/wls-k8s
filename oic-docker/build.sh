#!/bin/sh
#
#Copyright (c) 2014-2018 Oracle and/or its affiliates. All rights reserved.
#
#Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
#
docker build --no-cache -t 12213-domain .

docker tag 12213-domain:latest phx.ocir.io/oicpaas1/sumagant/wls-domain:12.2.1.3
