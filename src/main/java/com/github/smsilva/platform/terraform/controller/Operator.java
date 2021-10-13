package com.github.smsilva.platform.terraform.controller;

import com.github.smsilva.platform.terraform.crd.PlatformInstance;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class Operator {

    private static final Logger logger = LoggerFactory.getLogger(Operator.class);

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
                    if (theyAreDifferent(oldResource, newResource)) {
                        onUpdatePlatformInstance(oldResource, newResource);
                    }
                }

                @Override
                public void onDelete(PlatformInstance platformInstance, boolean b) {
                    onDeletePlatformInstance(platformInstance, client);
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
        logger.info("    {}", platformInstance);

        Resource<ConfigMap> configMapResource = client
                .configMaps()
                .inNamespace(platformInstance.getMetadata().getNamespace())
                .withName(platformInstance.getMetadata().getName());

        ConfigMap configMap = configMapResource.createOrReplace(new ConfigMapBuilder().
                withNewMetadata().withName(platformInstance.getMetadata().getName()).endMetadata().
                build());

        logger.info("Upserted ConfigMap at {} data {}", configMap.getMetadata().getSelfLink(), configMap.getData());
    }

    private static boolean theyAreDifferent(PlatformInstance oldResource, PlatformInstance newResource) {
        return !oldResource.getMetadata().getResourceVersion().equals(newResource.getMetadata().getResourceVersion());
    }

    private static void onUpdatePlatformInstance(PlatformInstance oldResource, PlatformInstance newResource) {
        logger.info("UPDATE {} - {}/{}",
                oldResource.getMetadata().getSelfLink(),
                oldResource.getMetadata().getResourceVersion(),
                newResource.getMetadata().getResourceVersion());
    }

    private static void onDeletePlatformInstance(PlatformInstance platformInstance, KubernetesClient client) {
        logger.info("DELETE {}", platformInstance.getMetadata().getSelfLink());

        Resource<ConfigMap> configMapResource = client
                .configMaps()
                .inNamespace(platformInstance.getMetadata().getNamespace())
                .withName(platformInstance.getMetadata().getName());

        Boolean deleted = configMapResource.delete();

        logger.info("ConfigMap {} exclusion : {}", platformInstance.getMetadata().getName(), deleted);
    }

}
