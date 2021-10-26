package com.github.smsilva.platform.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.smsilva.platform.model.v1.StackInstance;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.singletonMap;

public class StackInstanceController {

    public static final String STACK_INSTANCE_NAME = "stack-instance-name";
    private static final Logger logger = LoggerFactory.getLogger(StackInstanceController.class);

    public static void main(String[] args) throws Exception {
        logger.info("Starting...");

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

            TimeUnit.DAYS.sleep(1000);
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
    }

    private static void onAddHandle(StackInstance stackInstance, KubernetesClient client) {
        reconcile(stackInstance, client, "Creation");
    }

    private static void onUpdateHandle(StackInstance oldResource, StackInstance newResource, KubernetesClient client) {
        logger.info("UPDATE {} - {}/{}",
                oldResource.getMetadata().getSelfLink(),
                oldResource.getMetadata().getResourceVersion(),
                newResource.getMetadata().getResourceVersion());

        logger.info("    [{} {}] {}", oldResource.getMetadata().getUid(), oldResource.getMetadata().getResourceVersion(), oldResource);
        logger.info("    [{} {}] {}", newResource.getMetadata().getUid(), newResource.getMetadata().getResourceVersion(), newResource);

        reconcile(newResource, client, "Update");
    }

    private static void onDeleteHandle(StackInstance stackInstance, KubernetesClient client) {
        reconcile(stackInstance, client, "Delete");

        Boolean podDeleted = deletePod(client, stackInstance);
        logger.info("POD {} exclusion: {}", stackInstance.getName(), podDeleted);

        Boolean configMapDeleted = deleteConfigMap(client, stackInstance);
        logger.info("ConfigMap {} exclusion: {}", stackInstance.getName(), configMapDeleted);
    }

    private static void reconcile(StackInstance stackInstance, KubernetesClient client, String reason) {
        logger.info("reconcile :: reason = {} :: {}", reason, stackInstance);
        try {
            createEvent(stackInstance, client, "ReconciliationStart", reason, "Terraform reconciliation started");

            ConfigMap configMap = createOrReplace(stackInstance, client);

            createEvent(stackInstance, client, "ConfigMapCreation", reason, "ConfigMap created: " + configMap.getMetadata().getName());

            Pod pod = createOrReplace(stackInstance, client, configMap);

            createEvent(stackInstance, client, "PodExecutuonStarted", reason, "Pod: " + pod.getMetadata().getName() + " created. Waiting for completion.");

            waitForComplete(client, pod);

            createEvent(stackInstance, client, "PodExecutuonCompleted", reason, "Pod: " + pod.getMetadata().getName() + " has completed execution.");

            updateConfigMap(stackInstance, client, pod);

            createEvent(stackInstance, client, "PodExclusionRequested", reason, "Pod: " + pod.getMetadata().getName() + " deleted.");

            delete(client, pod);

            createEvent(stackInstance, client, "ReconciliationDone", reason, "Creation completed.");
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    private static Pod createOrReplace(StackInstance stackInstance, KubernetesClient client, ConfigMap configMap) throws Exception {
        String podName = stackInstance.getName() + stackInstance.getVersion();

        client.pods()
            .inNamespace(stackInstance.getNamespace())
            .withLabels(singletonMap(STACK_INSTANCE_NAME, stackInstance.getName()))
            .delete();

        Pod pod = new PodBuilder()
            .withNewMetadata()
                .withName(podName)
                .withLabels(singletonMap(STACK_INSTANCE_NAME, stackInstance.getName()))
            .endMetadata()
            .withNewSpec()
                .withRestartPolicy("OnFailure")
                .addNewContainer()
                    .withName("output")
                    .withImage(stackInstance.getImage())
                    .withArgs("output", "-json")
                    .withEnvFrom(getEnvFromSource(stackInstance), getEnvFromSource(configMap))
                .endContainer()
                .addNewInitContainer()
                    .withName("apply")
                    .withImage(stackInstance.getImage())
                    .withArgs("apply", "-auto-approve", "-input=false", "-no-color")
                    .withEnvFrom(getEnvFromSource(stackInstance), getEnvFromSource(configMap))
                .endInitContainer()
            .endSpec()
            .build();


        Pod createdPod = client.pods()
                .inNamespace(stackInstance.getNamespace())
                .createOrReplace(pod);

        logger.info("POD {} created.", createdPod.getMetadata().getName());

        return createdPod;
    }

    private static void delete(KubernetesClient client, Pod pod) {
        logger.info("Request POD {} exclusion", pod.getMetadata().getName());

        client.pods()
            .inNamespace(pod.getMetadata().getNamespace())
            .withName(pod.getMetadata().getName())
            .delete();
    }

    private static void waitForComplete(KubernetesClient client, Pod pod) {
        logger.info("Waiting for POD Status becomes Completed: {}", pod.getFullResourceName());

        client.pods()
            .inNamespace(pod.getMetadata().getNamespace())
            .withName(pod.getMetadata().getName())
            .waitUntilCondition(StackInstanceController::isCompleted, 1, TimeUnit.MINUTES);
    }

    private static void updateConfigMap(StackInstance stackInstance, KubernetesClient client, Pod createdPod) throws Exception {
        String applyLog = getLog(client, createdPod, "apply");
        String outputLog = getLog(client, createdPod, "output");

        ConfigMapBuilder configMapBuilder = new ConfigMapBuilder()
            .withNewMetadata()
                .withName(stackInstance.getName())
            .endMetadata()
            .addToData("apply.log", applyLog)
            .addToData("output.log", outputLog);

        for (String key : stackInstance.getSpec().getOutputs()) {
            JsonNode value = new ObjectMapper()
                    .readTree(outputLog)
                    .get(key)
                    .get("value");

            configMapBuilder.addToData("output_" + key, value.textValue());

            logger.info("output :: {}={}", key, value);
        }

        client.configMaps()
            .inNamespace(stackInstance.getNamespace())
            .withName(stackInstance.getName())
            .createOrReplace(configMapBuilder.build());
    }

    private static EnvFromSource getEnvFromSource(StackInstance stackInstance) {
        return new EnvFromSourceBuilder()
            .withNewSecretRef()
            .withName(stackInstance.getSecretName())
            .endSecretRef()
            .build();
    }

    private static EnvFromSource getEnvFromSource(ConfigMap configMap) {
        return new EnvFromSourceBuilder()
            .withNewConfigMapRef()
            .withName(configMap.getMetadata().getName())
            .endConfigMapRef()
            .build();
    }

    private static String getLog(KubernetesClient client, Pod pod, String containerName) {
        return client.pods()
            .inNamespace(pod.getMetadata().getNamespace())
            .withName(pod.getMetadata().getName())
            .inContainer(containerName)
            .getLog();
    }

    private static Event createEvent(StackInstance stackInstance, KubernetesClient client, String name, String reason, String message) throws Exception {
        ObjectReference objectReference = new ObjectReferenceBuilder()
            .withUid(stackInstance.getMetadata().getUid())
            .withApiVersion(stackInstance.getApiVersion())
            .withKind(stackInstance.getKind())
            .withNamespace(stackInstance.getNamespace())
            .withName(stackInstance.getName())
            .build();

        String eventName = stackInstance.getName() + "-" + name + "-" + UUID.randomUUID();

        final DateTimeFormatter microTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'.'SSSSSSXXX");

        MicroTime microTime = new MicroTimeBuilder()
            .withTime(microTimeFormatter.format(ZonedDateTime.now()))
            .build();

        Event event = new EventBuilder()
            .withInvolvedObject(objectReference)
            .withNewMetadata()
                .withName(eventName)
                .withNamespace(stackInstance.getNamespace())
                .withLabels(singletonMap(STACK_INSTANCE_NAME, stackInstance.getName()))
            .endMetadata()
            .withEventTime(microTime)
            .withFirstTimestamp(microTimeFormatter.format(ZonedDateTime.now()))
            .withLastTimestamp(microTimeFormatter.format(ZonedDateTime.now()))
            .withReportingInstance("stack-instance-operator")
            .withReportingComponent("operator")
            .withAction("Update")
            .withType("Normal")
            .withReason(reason)
            .withMessage(message)
            .build();

        Event createdEvent = client.v1()
            .events()
            .createOrReplace(event);

        logger.info("Event {} created", createdEvent.getMetadata().getName());

        TimeUnit.SECONDS.sleep(1);

        return createdEvent;
    }

    private static boolean isCompleted(Pod pod) {
        List<ContainerStatus> containerStatuses = pod
                .getStatus()
                .getContainerStatuses();

        for (ContainerStatus containerStatus : containerStatuses) {
            logger.info("{} :: state :: {}",
                    containerStatus.getName(),
                    containerStatus.getState());

            if (containerStatus.getState().getTerminated() != null) {
                logger.info("{} :: state :: TERMINATED :: reason: {}",
                        containerStatus.getName(),
                        containerStatus.getState().getTerminated().getReason());

                if (containerStatus.getState().getTerminated().getReason().equals("Completed")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static ConfigMap createOrReplace(StackInstance stackInstance, KubernetesClient client) {
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

    private static Boolean deleteConfigMap(KubernetesClient client, StackInstance stackInstance) {
        return client
                .configMaps()
                .inNamespace(stackInstance.getNamespace())
                .withName(stackInstance.getName())
                .delete();
    }

    private static Boolean deletePod(KubernetesClient client, StackInstance stackInstance) {
        return client.pods()
                .inNamespace(stackInstance.getNamespace())
                .withLabels(singletonMap(STACK_INSTANCE_NAME, stackInstance.getName()))
                .delete();
    }

}
