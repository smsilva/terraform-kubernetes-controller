package com.github.smsilva.terraform;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesApi;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesObject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;

public class Operator {

    private static final Logger logger = LoggerFactory.getLogger(Operator.class);

    public static void main(String[] args) throws IOException, ApiException {
        namespaceExample();
        stackExample();
    }

    @NotNull
    private static void namespaceExample() throws IOException, ApiException {
        ApiClient apiClient = ClientBuilder.standard().build();

        DynamicKubernetesApi dynamicApi = new DynamicKubernetesApi("", "v1", "namespaces", apiClient);

        DynamicKubernetesObject defaultNamespace =
                dynamicApi.get("default").throwsApiException().getObject();

        defaultNamespace.setMetadata(defaultNamespace.getMetadata().putLabelsItem("foo", "bar"));

        DynamicKubernetesObject updatedDefaultNamespace =
                dynamicApi.update(defaultNamespace).throwsApiException().getObject();

        logger.info("Namespace: {}", defaultNamespace.getMetadata().getName());

        apiClient.getHttpClient().connectionPool().evictAll();
    }

    @NotNull
    private static void stackExample() throws IOException, ApiException {
        ApiClient apiClient = ClientBuilder.standard().build();

        DynamicKubernetesApi dynamicApi = new DynamicKubernetesApi("poc.silvios.me", "v1alpha", "stacks", apiClient);

        DynamicKubernetesObject stackDummyInstance =
                dynamicApi.get("default", "dummy").throwsApiException().getObject();

        UUID uuid = UUID.randomUUID();

        stackDummyInstance.setMetadata(stackDummyInstance.getMetadata().putLabelsItem("foo", uuid.toString()));

        dynamicApi.update(stackDummyInstance).throwsApiException();

        logger.info("Stack [{}]: {}", uuid.toString(), stackDummyInstance.getMetadata().getName());

        apiClient.getHttpClient().connectionPool().evictAll();
    }
}
