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
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class StackInstanceController {

    public static final String STACK_INSTANCE_NAME = "stack-instance-name";
    private static final Logger logger = LoggerFactory.getLogger(StackInstanceController.class);

    public static void main(String[] args) throws Exception {
        logger.info("Starting...");

        while (true) {
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
                            onUpdateHandle(oldResource, newResource, client);
                        }
                    }

                    @Override
                    public void onDelete(StackInstance StackInstance, boolean b) {
                        onDeleteHandle(StackInstance, client);
                    }
                });

                sharedInformerFactory.startAllRegisteredInformers();

                logger.info("I'll sleep for 10 minute");

                TimeUnit.MINUTES.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void onAddHandle(StackInstance stackInstance, KubernetesClient client) {
        logger.info("ADD {}", stackInstance.getMetadata().getSelfLink());
        logger.info("    {}", stackInstance);

        reconcile(stackInstance, client);
    }

    private static void reconcile(StackInstance StackInstance, KubernetesClient client)  {
        try {
            ConfigMap configMap = createConfigMap(StackInstance, client);

            createJob(StackInstance, client, configMap);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    private static void createJob(StackInstance stackInstance, KubernetesClient client, ConfigMap configMap) throws Exception {
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
                            .withArgs("apply", "-auto-approve", "-no-color", "-input=false")
                            .withEnvFrom(envFromSourceSecret, envFromSourceConfigMap)
                        .endContainer()
                        .withRestartPolicy("Never")
                    .endSpec()
                .endTemplate()
                .withBackoffLimit(2)
            .endSpec()
            .build();

        logger.info("Creating JOB {}", jobName);

        client.batch().v1()
                .jobs()
                .inNamespace(namespace)
                .createOrReplace(job);

        Thread.sleep(10000);

        try {
            logger.info("Getting POD List from JOB: {}", job.getMetadata().getName());

            PodList podList = client
                    .pods()
                    .inNamespace(namespace)
                    .withLabel("job-name", job.getMetadata().getName())
                    .list();

            logger.info("POD List from JOB {}: {}", job.getMetadata().getName(), podList.getItems().size());

            if (podList.getItems().size() > 0) {
                logger.info("Waiting for Succeeded execution: {}", job.getMetadata().getName());

                try {
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
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
            }
        } finally {
            logger.info("Deleting job: {}", job.getMetadata().getSelfLink());

//            client.batch().v1()
//                .jobs()
//                .inNamespace(namespace)
//                .delete(job);
        }
    }

    private static ConfigMap createConfigMap(StackInstance stackInstance, KubernetesClient client) {
        Resource<ConfigMap> configMapResource = client
                .configMaps()
                .inNamespace(stackInstance.getMetadata().getNamespace())
                .withName(stackInstance.getMetadata().getName());

        ConfigMapBuilder configMapBuilder = new ConfigMapBuilder()
                .withNewMetadata()
                .withName(stackInstance.getName())
                .endMetadata();

        for (Map.Entry<String, String> variable : stackInstance.getVariablesAsEntrySet()) {
            configMapBuilder.addToData("TF_VAR_" + variable.getKey(), variable.getValue());
        }

        configMapBuilder.
            addToData("STACK_INSTANCE_NAME", stackInstance.getName()).
            addToData("DEBUG", "0");

        ConfigMap configMap = configMapResource.createOrReplace(configMapBuilder.build());

        logger.info("Upserted ConfigMap at {} data {}", configMap.getMetadata().getSelfLink(), configMap.getData());

        return configMap;
    }

    private static boolean theyAreDifferent(StackInstance oldResource, StackInstance newResource) {
        return !oldResource.getMetadata().getResourceVersion().equals(newResource.getMetadata().getResourceVersion());
    }

    private static void onUpdateHandle(StackInstance oldResource, StackInstance newResource, KubernetesClient client) {
        logger.info("UPDATE {} - {}/{}",
                oldResource.getMetadata().getSelfLink(),
                oldResource.getMetadata().getResourceVersion(),
                newResource.getMetadata().getResourceVersion());
        logger.info("    [{} {}] {}", oldResource.getMetadata().getUid(), oldResource.getMetadata().getResourceVersion(), oldResource);
        logger.info("    [{} {}] {}", newResource.getMetadata().getUid(), newResource.getMetadata().getResourceVersion(), newResource);

        reconcile(newResource, client);
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
