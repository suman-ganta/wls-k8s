# HOW TO BUILD THIS IMAGE
# -----------------------
# Put all downloaded files in the same directory as this Dockerfile
# Pull base image
# ---------------
FROM store/oracle/weblogic:12.2.1.3

# Maintainer
# ----------
MAINTAINER Suman Ganta <suman.g.ganta@oracle.com>

# WLS Configuration
# ---------------------------
ENV ADMIN_HOST="wlsadmin" \
    NM_PORT="5556" \
    MS_PORT="8001" \
    DEBUG_PORT="8453" \
    ORACLE_HOME=/u01/oracle \
    SCRIPT_FILE=/u01/oracle/createAndStartWLSDomain.sh \
    CONFIG_JVM_ARGS="-Dweblogic.security.SSL.ignoreHostnameVerification=true"  \
    PATH=$PATH:/u01/oracle/oracle_common/common/bin:/u01/oracle/wlserver/common/bin:/u01/oracle/user_projects/domains/${DOMAIN_NAME:-base_domain}/bin:/u01/oracle

# Domain and Server environment variables
# ------------------------------------------------------------
ENV DOMAIN_NAME="${DOMAIN_NAME:-base_domain}" \
    PRE_DOMAIN_HOME=/u01/oracle/user_projects \
    ADMIN_PORT="${ADMIN_PORT:-7001}"  \
    ADMIN_USERNAME="${ADMIN_USERNAME:-weblogic}" \
    ADMIN_NAME="${ADMIN_NAME:-AdminServer}" \
    MS_NAME="${MS_NAME:-""}" \
    NM_NAME="${NM_NAME:-""}" \
    ADMIN_PASSWORD="${ADMIN_PASSWORD:-"welcome1"}" \
    CLUSTER_NAME="${CLUSTER_NAME:-DockerCluster}" \
    DEBUG_FLAG=true \
    PRODUCTION_MODE=dev

# Add files required to build this image
COPY container-scripts/* /u01/oracle/

#Create directory where domain will be written to
USER root
RUN chmod +xw /u01/oracle/*.sh && \
    chmod +xw /u01/oracle/*.py && \
    mkdir -p $PRE_DOMAIN_HOME && \
    chmod a+xr $PRE_DOMAIN_HOME && \
    chown -R oracle:oracle $PRE_DOMAIN_HOME

USER oracle

# Configuration of WLS Domain
RUN sed -i -e "s|ADMIN_PASSWORD|$ADMIN_PASSWORD|g" /u01/oracle/create-wls-domain.py && \
    /u01/oracle/wlst -skipWLSModuleScanning /u01/oracle/create-wls-domain.py && \
    mkdir -p /u01/oracle/user_projects/domains/$DOMAIN_NAME/servers/AdminServer/security && \
    echo "username=weblogic" > /u01/oracle/user_projects/domains/$DOMAIN_NAME/servers/AdminServer/security/boot.properties && \
    echo "password=$ADMIN_PASSWORD" >> /u01/oracle/user_projects/domains/$DOMAIN_NAME/servers/AdminServer/security/boot.properties && \
    echo ". /u01/oracle/user_projects/domains/$DOMAIN_NAME/bin/setDomainEnv.sh" >> /u01/oracle/.bashrc

# Expose Node Manager default port, and also default for admin and managed server
EXPOSE $NM_PORT $ADMIN_PORT $MS_PORT $DEBUG_PORT
WORKDIR $ORACLE_HOME

# Define default command to start bash.
CMD ["startWebLogic.sh"]
