package com.github.smsilva.platform.example.controller;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class Informer {

    private static final Logger logger = LoggerFactory.getLogger(Informer.class);

    public static void main(String[] args) throws Exception {
        try (KubernetesClient client = new DefaultKubernetesClient()) {

            SharedInformerFactory informerFactory = client.informers();

            CustomResourceDefinitionContext context = new CustomResourceDefinitionContext.Builder()
                    .withGroup("demo.fabric8.io")
                    .withVersion("v1")
                    .withPlural("dummies")
                    .withScope("Namespaced")
                    .build();

            SharedIndexInformer<GenericKubernetesResource> informer = informerFactory
                    .sharedIndexInformerForCustomResource(context, 30 * 1000L);

            informer.addEventHandler(new ResourceEventHandler<GenericKubernetesResource>() {
                @Override
                public void onAdd(GenericKubernetesResource resource) {
                    logger.info("{} add", resource.getMetadata().getName());
                }

                @Override
                public void onUpdate(GenericKubernetesResource oldResource, GenericKubernetesResource newResource) {
                    if (!oldResource.getMetadata().getResourceVersion().equals(newResource.getMetadata().getResourceVersion())) {
                        logger.info("{} update {}/{}",
                                oldResource.getMetadata().getName(),
                                oldResource.getMetadata().getResourceVersion(),
                                newResource.getMetadata().getResourceVersion());
                    }
                }

                @Override
                public void onDelete(GenericKubernetesResource resource, boolean b) {
                    logger.info("{} delete", resource.getMetadata().getName());
                }
            });

            informerFactory.startAllRegisteredInformers();

            TimeUnit.MINUTES.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
    }
}
