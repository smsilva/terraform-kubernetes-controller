package com.github.smsilva.terraform;

import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.util.CallGeneratorParams;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.Watch;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesApi;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesObject;
import okhttp3.Call;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;

public class ChangeArbitraryKubernetesObject {

    private static final Logger logger = LoggerFactory.getLogger(ChangeArbitraryKubernetesObject.class);

    public static void main(String[] args) throws IOException, ApiException {
//        namespaceExample();
//        stackExample();
//        watchNamespaceExample();
        watchStackExample();
    }

    @NotNull
    private static void watchNamespaceExample() throws IOException, ApiException {
        ApiClient client = ClientBuilder.standard().build();
        Configuration.setDefaultApiClient(client);

        CoreV1Api api = new CoreV1Api();

        Watch<V1Namespace> watch = Watch.createWatch(
                client,
                api.listNamespaceCall(null,
                        null,
                        null,
                        null,
                        null,
                        5,
                        null,
                        null,
                        null,
                        Boolean.TRUE,
                        null),
                new TypeToken<Watch.Response<V1Namespace>>(){}.getType());

        for (Watch.Response<V1Namespace> item : watch) {
            System.out.printf("%s : %s%n", item.type, item.object.getMetadata().getName());
        }
    }

    @NotNull
    private static void watchStackExample() throws IOException, ApiException {
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
