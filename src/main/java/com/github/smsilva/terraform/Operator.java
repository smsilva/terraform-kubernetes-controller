package com.github.smsilva.terraform;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.*;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Operator {

    private static final Logger logger = LoggerFactory.getLogger(Operator.class);

    public static void main(String[] args) {
        logger.info("Starting...");

        Config config = new ConfigBuilder().build();
        DefaultKubernetesClient client = new DefaultKubernetesClient(config);

        VersionInfo version = client.getVersion();

        logger.info("Version details of this Kubernetes cluster: ");
        logger.info("Major        : {}", version.getMajor());
        logger.info("Minor        : {}", version.getMinor());

        List<Pod> podList = client.pods().list().getItems();

        logger.info("Found " + podList.size() + " Pods:");

        for (Pod pod : podList) {
            logger.info(String.format(" * %s (namespace %s)",
                    pod.getMetadata().getName(),
                    pod.getMetadata().getNamespace()));
        }

        String namespace = client.getNamespace();

        KubernetesDeserializer.registerCustomKind(HasMetadata.getApiVersion(PayaraDomainResource.class), "Domain", PayaraDomainResource.class);
        client.customResources(PayaraDomainResource.class, PayaraDomainResourceList.class).inNamespace(namespace);

        client.pods().watch(new Watcher<Pod>() {
            @Override
            public void eventReceived(Action action, Pod pod) {
                logger.info("event received " + action + " for pod " + pod.
                        getMetadata().getName());
            }

            @Override
            public void onClose(WatcherException e) {
                logger.error(e.getMessage());
            }
        });
    }

}
