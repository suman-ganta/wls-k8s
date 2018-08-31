# Pull base image
# ---------------
FROM ${docker.repository}/jdk:8

# Maintainer
# ----------
MAINTAINER Suman Ganta <suman.g.ganta@oracle.com>


# Environment variables required for this build (do NOT change)
# -------------------------------------------------------------

ENV ORACLE_HOME=/oic/wls-controller \
    PATH=$PATH:/usr/java/default/bin:/oic/wls-controller \
    LANG="en_US.UTF-8" \
    LC_ALL="en_US.UTF-8"

# Configure controller Jars
# ------------------------------------------------------------
COPY lib/* /oic/wls-controller/lib/

# Setup filesystem and oracle user
# ------------------------------------------------------------
RUN chmod a+xr /oic/wls-controller && \
    useradd -b /oic/wls-controller -m -s /bin/bash oracle && \
    echo oracle:oracle | chpasswd

RUN chown oracle:oracle -R /oic

USER oracle

WORKDIR $ORACLE_HOME

# Define default command to start bash.
CMD ["java", "-cp", "lib/*", "oracle.oic.wlscontroller.Main"]
