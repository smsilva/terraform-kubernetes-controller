package com.github.smsilva.platform.terraform.controller;

import com.github.smsilva.platform.terraform.crd.PlatformInstance;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Informer {

    private static final Logger logger = LoggerFactory.getLogger(Informer.class);

    public static void main(String[] args) throws Exception {
        try (KubernetesClient client = new DefaultKubernetesClient()) {

            SharedInformerFactory sharedInformerFactory = client.informers();

            SharedIndexInformer<PlatformInstance> sharedIndexInformer = sharedInformerFactory
                    .sharedIndexInformerFor(PlatformInstance.class, 30 * 1000L);

            sharedIndexInformer.addEventHandler(new ResourceEventHandler<PlatformInstance>() {
                @Override
                public void onAdd(PlatformInstance platformInstance) {
                    onAddPlatformInstance(platformInstance, client);
                }

                @Override
                public void onUpdate(PlatformInstance oldResource, PlatformInstance newResource) {
                    if (!oldResource.getMetadata().getResourceVersion().equals(newResource.getMetadata().getResourceVersion())) {
                        logger.info("UPDATE {} - {}/{}",
                                oldResource.getMetadata().getSelfLink(),
                                oldResource.getMetadata().getResourceVersion(),
                                newResource.getMetadata().getResourceVersion());
                    }
                }

                @Override
                public void onDelete(PlatformInstance resource, boolean b) {
                    logger.info("DELETE {}", resource.getMetadata().getSelfLink());
                }
            });

            sharedInformerFactory.startAllRegisteredInformers();

            TimeUnit.MINUTES.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
    }

    private static void onAddPlatformInstance(PlatformInstance platformInstance, KubernetesClient client) {
        logger.info("ADD {}", platformInstance.getMetadata().getSelfLink());

        String platformInstanceName = platformInstance.getMetadata().getName();

        Resource<ConfigMap> configMapResource = client
                .configMaps()
                .inNamespace(platformInstance.getMetadata().getNamespace())
                .withName(platformInstanceName);

        ConfigMap configMap = configMapResource.createOrReplace(new ConfigMapBuilder().
                withNewMetadata().withName(platformInstanceName).endMetadata().
                addToData("foo", UUID.randomUUID().toString()).
                addToData("bar", "beer").
                build());

        logger.info("Upserted ConfigMap at {} data {}", configMap.getMetadata().getSelfLink(), configMap.getData());

        platformInstance.getStatus().setMessage("Updated status message");
        platformInstance.getStatus().setReady(true);
    }

}
