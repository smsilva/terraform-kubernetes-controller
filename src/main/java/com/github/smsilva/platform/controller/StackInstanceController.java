package com.github.smsilva.platform.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.smsilva.platform.model.v1.StackInstance;
import com.github.smsilva.platform.model.v1.StackInstanceList;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
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

                logger.info("I'll sleep for 10 minutes (?)");

                TimeUnit.MINUTES.sleep(10);
            } catch (Exception e) {
                logger.error(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static void onAddHandle(StackInstance stackInstance, KubernetesClient client) {
        logger.info("ADD {}", stackInstance.getMetadata().getSelfLink());
        logger.info("    {}", stackInstance);

        reconcile(stackInstance, client);
    }

    private static void reconcile(StackInstance stackInstance, KubernetesClient client)  {
        try {
            ConfigMap configMap = createConfigMap(stackInstance, client);

            createPod(stackInstance, client, configMap);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    private static void createPod(StackInstance stackInstance, KubernetesClient client, ConfigMap configMap) throws Exception {
        String namespace = stackInstance.getNamespace();

        EnvFromSource envFromSourceConfigMap = new EnvFromSourceBuilder()
            .withNewConfigMapRef()
            .withName(configMap.getMetadata().getName())
            .endConfigMapRef()
            .build();

        EnvFromSource envFromSourceSecret = new EnvFromSourceBuilder()
            .withNewSecretRef()
            .withName(stackInstance.getSecretName())
            .endSecretRef()
            .build();

        String podName = stackInstance.getName() + stackInstance.getVersion();

        logger.info("podName: {}", podName);

        client.pods()
                .inNamespace(namespace)
                .withLabels(Collections.singletonMap(STACK_INSTANCE_NAME, stackInstance.getName()))
                .delete();

        Pod pod = new PodBuilder()
            .withNewMetadata()
                .withName(podName)
                .withLabels(Collections.singletonMap(STACK_INSTANCE_NAME, stackInstance.getName()))
            .endMetadata()
            .withNewSpec()
                .withRestartPolicy("OnFailure")
                .addNewContainer()
                    .withName("output")
                    .withImage(stackInstance.getImage())
                    .withArgs("output", "-json")
                    .withEnvFrom(envFromSourceSecret, envFromSourceConfigMap)
                .endContainer()
                .addNewInitContainer()
                    .withName("apply")
                    .withImage(stackInstance.getImage())
                    .withArgs("apply", "-auto-approve", "-input=false", "-no-color")
                    .withEnvFrom(envFromSourceSecret, envFromSourceConfigMap)
                .endInitContainer()
            .endSpec()
            .build();

        client.pods()
            .inNamespace(namespace)
            .createOrReplace(pod);

        try {
            logger.info("Waiting for POD Status becomes Completed");

            client.pods()
                .inNamespace(namespace)
                .withName(pod.getMetadata().getName())
                .waitUntilCondition(StackInstanceController::isCompleted, 1, TimeUnit.MINUTES);

            String applyLog = client.pods()
                    .inNamespace(namespace)
                    .withName(podName)
                    .inContainer("apply")
                    .getLog();

            logger.info("I'll try to retrieve Logs from output container at POD {}", pod.getMetadata().getName());

            String outputLog = client.pods()
                .inNamespace(namespace)
                .withName(podName)
                .inContainer("output")
                .getLog();

            ConfigMapBuilder configMapBuilder = new ConfigMapBuilder()
                    .withNewMetadata()
                        .withName(stackInstance.getName())
                    .endMetadata()
                    .addToData("apply.log", applyLog)
                    .addToData("output.log", outputLog);

            try {
                ObjectMapper mapper = new ObjectMapper();

                JsonNode jsonObject = mapper.readTree(outputLog);

                for (String key : stackInstance.getSpec().getOutputs()) {
                    JsonNode jsonObjectOutput = jsonObject.get(key);
                    JsonNode value = jsonObjectOutput.get("value");
                    logger.info("  output_{}={}", key, value);
                    configMapBuilder.addToData("output_" + key, value.textValue());
                }

            } catch (Exception e) {
                logger.error(e.getMessage());
            }

            client
                .configMaps()
                .inNamespace(stackInstance.getNamespace())
                .withName(stackInstance.getName())
                .createOrReplace(configMapBuilder.build());

            logger.info("Request POD {} exclusion", pod.getFullResourceName());

            client.pods()
                    .inNamespace(namespace)
                    .withName(podName)
                    .delete();

            createEvent(stackInstance, client);

            logger.info("Done");
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    private static void createEvent(StackInstance stackInstance, KubernetesClient client) {
        logger.info("stackInstance.getMetadata().getUid(): {}", stackInstance.getMetadata().getUid());

        ObjectReference objectReference = new ObjectReferenceBuilder()
                .withUid(stackInstance.getMetadata().getUid())
                .withApiVersion(stackInstance.getApiVersion())
                .withKind(stackInstance.getKind())
                .withNamespace(stackInstance.getNamespace())
                .withName(stackInstance.getName())
                .build();

        logger.info("objectReference: {}", objectReference);

        try {
            String eventNameWithUUID = stackInstance.getName() + "_" +  UUID.randomUUID();

            logger.info("Creating Event {}", eventNameWithUUID);

            ZonedDateTime zonedDateTime = ZonedDateTime.now();
            LocalDateTime localDateTime = LocalDateTime.now();

            logger.info("zonedDateTime: {}", zonedDateTime);
            logger.info("localDateTime: {}", localDateTime);

            final DateTimeFormatter microTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'.'SSSSSSXXX");

            MicroTime microTime = new MicroTimeBuilder()
                    .withTime(microTimeFormatter.format(zonedDateTime))
                    .build();

            logger.info("microTime: {}", microTime);

            Event reconcileEvent = new EventBuilder()
                    .withInvolvedObject(objectReference)
                    .withNewMetadata()
                        .withName(eventNameWithUUID)
                        .withNamespace(stackInstance.getNamespace())
                    .endMetadata()
                    .withEventTime(microTime)
                    .withReportingInstance("ReportInstance")
                    .withReportingComponent("ComponentReporting")
                    .withAction("Update")
                    .withType("Normal")
                    .withReason("Reconciled")
                    .withMessage("This is the event message: " + eventNameWithUUID)
                    .build();

            Event createdEvent = client.v1()
                    .events()
                    .createOrReplace(reconcileEvent);

            logger.info("createdEvent: {}", createdEvent);
        } catch (Exception e) {
            logger.error("Error creating event {}", e.getMessage());
            e.printStackTrace();
        }
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

        Boolean podDeleted = deletePod(client, stackInstance);
        logger.info("POD {} exclusion : {}", stackInstance.getName(), podDeleted);

        Boolean configMapDeleted = deleteConfigMap(client, stackInstance);
        logger.info("ConfigMap {} exclusion : {}", stackInstance.getName(), configMapDeleted);
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
                .withLabels(Collections.singletonMap(STACK_INSTANCE_NAME, stackInstance.getName()))
                .delete();
    }

}
