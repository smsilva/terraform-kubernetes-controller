package com.github.smsilva.platform.controller;

import com.github.smsilva.platform.model.v1.PlatformInstance;
import com.github.smsilva.platform.model.v1.PlatformInstanceList;
import com.github.smsilva.platform.model.v1.PlatformInstanceStatus;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class PlatformInstanceController {

    public static final String LABEL_PLATFORM_INSTANCE_NAME = "platform-instance-name";
    private static final Logger logger = LoggerFactory.getLogger(PlatformInstanceController.class);

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

        ConfigMap configMap = createConfigMap(platformInstance, client);
        logger.info("Upserted ConfigMap at {} data {}", configMap.getMetadata().getSelfLink(), configMap.getData());

        createJob(platformInstance, client, configMap);
    }

    private static void createJob(PlatformInstance platformInstance, KubernetesClient client, ConfigMap configMap) {
        String image = "silviosilva/" + platformInstance.getSpec().getProvider() + "-dummy-stack:latest";
        String jobName = platformInstance.getMetadata().getName();
        String namespace = platformInstance.getMetadata().getNamespace();

        EnvFromSource envFromSourceConfigMap = new EnvFromSourceBuilder()
                .withNewConfigMapRef()
                .withName(configMap.getMetadata().getName())
                .endConfigMapRef()
                .build();

        EnvFromSource envFromSourceSecret = new EnvFromSourceBuilder()
                .withNewSecretRef()
                .withName(platformInstance.getSpec().getProvider() + "-credentials")
                .endSecretRef()
                .build();

        final Job job = new JobBuilder()
            .withApiVersion("batch/v1")
            .withNewMetadata()
                .withName(jobName)
                .withLabels(Collections.singletonMap(LABEL_PLATFORM_INSTANCE_NAME, platformInstance.getMetadata().getName()))
                .withAnnotations(Collections.singletonMap(LABEL_PLATFORM_INSTANCE_NAME, platformInstance.getMetadata().getName()))
            .endMetadata()
            .withNewSpec()
                .withNewTemplate()
                    .withNewSpec()
                        .addNewContainer()
                            .withName("apply")
                            .withImage(image)
                            .withArgs("apply", "-auto-approve")
                            .withEnvFrom(envFromSourceSecret, envFromSourceConfigMap)
                        .endContainer()
                        .withRestartPolicy("Never")
                    .endSpec()
                .endTemplate()
            .endSpec()
            .build();

        logger.info("Creating job {}", jobName);

        client.batch().v1().jobs().inNamespace(namespace).createOrReplace(job);

        logger.info("Getting POD List from job: {}", job.getMetadata().getName());

        PodList podList = client.pods()
                .inNamespace(namespace)
                .withLabel("job-name", job.getMetadata().getName()).list();

        logger.info("Waiting for Succeeded execution: {}", job.getMetadata().getName());

        client.pods()
                .inNamespace(namespace)
                .withName(podList.getItems().get(0).getMetadata().getName())
                .waitUntilCondition(pod -> pod.getStatus().getPhase().equals("Succeeded"), 1, TimeUnit.MINUTES);

        String joblog = client.batch().v1().jobs().inNamespace(namespace).withName(jobName).getLog();

        logger.info(joblog);

        PlatformInstanceStatus platformInstanceStatus = new PlatformInstanceStatus();
        platformInstanceStatus.setReady(true);
        platformInstance.setStatus(platformInstanceStatus);

        MixedOperation<PlatformInstance, PlatformInstanceList, Resource<PlatformInstance>> resources = client.resources(PlatformInstance.class, PlatformInstanceList.class);

        resources.createOrReplace(platformInstance);

        logger.info("Done");
    }

    private static ConfigMap createConfigMap(PlatformInstance platformInstance, KubernetesClient client) {
        Resource<ConfigMap> configMapResource = client
                .configMaps()
                .inNamespace(platformInstance.getMetadata().getNamespace())
                .withName(platformInstance.getMetadata().getName());

        return configMapResource.createOrReplace(new ConfigMapBuilder().
                withNewMetadata().withName(platformInstance.getMetadata().getName()).endMetadata().
                addToData("STACK_INSTANCE_NAME", platformInstance.getMetadata().getName()).
                addToData("DEBUG", "0").
                build());
    }

    private static boolean theyAreDifferent(PlatformInstance oldResource, PlatformInstance newResource) {
        return !oldResource.getMetadata().getResourceVersion().equals(newResource.getMetadata().getResourceVersion());
    }

    private static void onUpdatePlatformInstance(PlatformInstance oldResource, PlatformInstance newResource) {
        logger.info("UPDATE {} - {}/{}",
                oldResource.getMetadata().getSelfLink(),
                oldResource.getMetadata().getResourceVersion(),
                newResource.getMetadata().getResourceVersion());
        logger.info("    [{}] {}", oldResource.getMetadata().getResourceVersion(), oldResource);
        logger.info("    [{}] {}", newResource.getMetadata().getResourceVersion(), newResource);
    }

    private static void onDeletePlatformInstance(PlatformInstance platformInstance, KubernetesClient client) {
        logger.info("DELETE {}", platformInstance.getMetadata().getSelfLink());

        String namespace = platformInstance.getMetadata().getNamespace();
        String instanceName = platformInstance.getMetadata().getName();

        Boolean jobDeleted = deleteJob(client, instanceName, namespace);
        logger.info("Job {} exclusion : {}", instanceName, jobDeleted);

        Boolean configMapDeleted = deleteConfigMap(client, instanceName, namespace);
        logger.info("ConfigMap {} exclusion : {}", platformInstance.getMetadata().getName(), configMapDeleted);
    }

    private static Boolean deleteConfigMap(KubernetesClient client, String instanceName, String namespace) {
        return client
                .configMaps()
                .inNamespace(namespace)
                .withName(instanceName)
                .delete();
    }

    private static Boolean deleteJob(KubernetesClient client, String instanceName, String namespace) {
        return client
                .batch()
                .v1()
                .jobs()
                .inNamespace(namespace)
                .withName(instanceName)
                .delete();
    }

}
