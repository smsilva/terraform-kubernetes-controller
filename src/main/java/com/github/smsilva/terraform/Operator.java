package com.github.smsilva.terraform;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.VersionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Operator {

    private static final Logger logger = LoggerFactory.getLogger(Operator.class);

    public static void main(String[] args) {
        logger.info("Example log from {}", Operator.class.getSimpleName());
        logger.debug("Example log from {}", Operator.class.getSimpleName());
        logger.warn("Example log from {}", Operator.class.getSimpleName());
        logger.error("Example log from {}", Operator.class.getSimpleName());

        Config config = new ConfigBuilder().build();
        DefaultKubernetesClient client = new DefaultKubernetesClient(config);

        VersionInfo version = client.getVersion();

        logger.info("Version details of this Kubernetes cluster: ");
        logger.info("Major        : {}", version.getMajor());
        logger.info("Minor        : {}", version.getMinor());
    }

}
