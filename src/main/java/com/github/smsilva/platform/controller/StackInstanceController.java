package com.github.smsilva.platform.controller;

import com.github.smsilva.platform.model.v1.StackInstance;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class StackInstanceController {

    public static final String STACK_INSTANCE_NAME = "stack-instance-name";
    private static final Logger logger = LoggerFactory.getLogger(StackInstanceController.class);

    public static void main(String[] args) throws Exception {
        try (KubernetesClient client = new DefaultKubernetesClient()) {

            SharedInformerFactory sharedInformerFactory = client.informers();

            SharedIndexInformer<StackInstance> sharedIndexInformer = sharedInformerFactory
                    .sharedIndexInformerFor(StackInstance.class, 30 * 1000L);

            sharedIndexInformer.addEventHandler(new ResourceEventHandler<StackInstance>() {
                @Override
                public void onAdd(StackInstance instance) {
                    onAddHandle(instance, client);
                }

                @Override
                public void onUpdate(StackInstance oldResource, StackInstance newResource) {
                    if (theyAreDifferent(oldResource, newResource)) {
                        onUpdateHandle(oldResource, newResource);
                    }
                }

                @Override
                public void onDelete(StackInstance StackInstance, boolean b) {
                    onDeleteHandle(StackInstance, client);
                }
            });

            sharedInformerFactory.startAllRegisteredInformers();

            TimeUnit.MINUTES.sleep(3);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
    }

    private static void onAddHandle(StackInstance StackInstance, KubernetesClient client) {
        logger.info("ADD {}", StackInstance.getMetadata().getSelfLink());
        logger.info("    {}", StackInstance);

        ConfigMap configMap = createConfigMap(StackInstance, client);
        logger.info("Upserted ConfigMap at {} data {}", configMap.getMetadata().getSelfLink(), configMap.getData());

        createJob(StackInstance, client, configMap);
    }

    private static void createJob(StackInstance stackInstance, KubernetesClient client, ConfigMap configMap) {
        String stackInstanceName = stackInstance.getName();
        String jobName = stackInstanceName;
        String namespace = stackInstance.getNamespace();

        String provider = stackInstance.getProvider();
        String image = stackInstance.getImage();

        EnvFromSource envFromSourceConfigMap = new EnvFromSourceBuilder()
                .withNewConfigMapRef()
                .withName(configMap.getMetadata().getName())
                .endConfigMapRef()
                .build();

        String secretNameCredentials = provider + "-" + "credentials";

        EnvFromSource envFromSourceSecret = new EnvFromSourceBuilder()
                .withNewSecretRef()
                .withName(secretNameCredentials)
                .endSecretRef()
                .build();

        final Job job = new JobBuilder()
            .withApiVersion("batch/v1")
            .withNewMetadata()
                .withName(jobName)
                .withLabels(Collections.singletonMap(STACK_INSTANCE_NAME, stackInstanceName))
                .withAnnotations(Collections.singletonMap(STACK_INSTANCE_NAME, stackInstanceName))
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

        client.batch().v1()
                .jobs().inNamespace(namespace).createOrReplace(job);

        logger.info("Getting POD List from job: {}", job.getMetadata().getName());

        PodList podList = client.pods()
                .inNamespace(namespace)
                .withLabel("job-name", job.getMetadata().getName()).list();

        logger.info("Waiting for Succeeded execution: {}", job.getMetadata().getName());

        client.pods()
                .inNamespace(namespace)
                .withName(podList.getItems().get(0).getMetadata().getName())
                .waitUntilCondition(pod -> pod.getStatus().getPhase().equals("Succeeded"), 1, TimeUnit.MINUTES);

        String joblog = client.batch().v1()
                .jobs()
                .inNamespace(namespace)
                .withName(jobName)
                .getLog();

        logger.info(joblog);
    }

    private static ConfigMap createConfigMap(StackInstance stackInstance, KubernetesClient client) {
        Resource<ConfigMap> configMapResource = client
                .configMaps()
                .inNamespace(stackInstance.getMetadata().getNamespace())
                .withName(stackInstance.getMetadata().getName());

        String region = stackInstance.getSpec().getVars().get("region");

        return configMapResource.createOrReplace(new ConfigMapBuilder()
                .withNewMetadata().withName(stackInstance.getName()).endMetadata().
                addToData("TF_VAR_region", region). // Will change here to pass all variables
                addToData("STACK_INSTANCE_NAME", stackInstance.getName()).
                addToData("DEBUG", "0").
                build());
    }

    private static boolean theyAreDifferent(StackInstance oldResource, StackInstance newResource) {
        return !oldResource.getMetadata().getResourceVersion().equals(newResource.getMetadata().getResourceVersion());
    }

    private static void onUpdateHandle(StackInstance oldResource, StackInstance newResource) {
        logger.info("UPDATE {} - {}/{}",
                oldResource.getMetadata().getSelfLink(),
                oldResource.getMetadata().getResourceVersion(),
                newResource.getMetadata().getResourceVersion());
        logger.info("    [{}] {}", oldResource.getMetadata().getResourceVersion(), oldResource);
        logger.info("    [{}] {}", newResource.getMetadata().getResourceVersion(), newResource);
    }

    private static void onDeleteHandle(StackInstance stackInstance, KubernetesClient client) {
        logger.info("DELETE {}", stackInstance.getMetadata().getSelfLink());

        String namespace = stackInstance.getMetadata().getNamespace();
        String instanceName = stackInstance.getMetadata().getName();

        Boolean jobDeleted = deleteJob(client, instanceName, namespace);
        logger.info("Job {} exclusion : {}", instanceName, jobDeleted);

        Boolean configMapDeleted = deleteConfigMap(client, instanceName, namespace);
        logger.info("ConfigMap {} exclusion : {}", stackInstance.getMetadata().getName(), configMapDeleted);
    }

    private static Boolean deleteConfigMap(KubernetesClient client, String name, String namespace) {
        return client
                .configMaps()
                .inNamespace(namespace)
                .withName(name)
                .delete();
    }

    private static Boolean deleteJob(KubernetesClient client, String name, String namespace) {
        return client
                .batch()
                .v1()
                .jobs()
                .inNamespace(namespace)
                .withName(name)
                .delete();
    }

}
