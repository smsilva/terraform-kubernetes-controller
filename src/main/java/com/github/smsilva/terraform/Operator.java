package com.github.smsilva.terraform;


import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Lister;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.CallGeneratorParams;
import okhttp3.OkHttpClient;

import java.util.concurrent.TimeUnit;

public class Operator {

    public static void main(String[] args) throws Exception {
        CoreV1Api coreV1Api = new CoreV1Api();
        ApiClient apiClient = coreV1Api.getApiClient();

        OkHttpClient httpClient = apiClient
            .getHttpClient()
            .newBuilder()
            .readTimeout(0, TimeUnit.SECONDS)
            .build();

        apiClient.setHttpClient(httpClient);

        SharedInformerFactory factory = new SharedInformerFactory();

        SharedIndexInformer<V1Pod> podInformer =
                factory.sharedIndexInformerFor(
                        (CallGeneratorParams params) -> {
                            return coreV1Api.listNamespacedPodCall(
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    0,
                                    params.resourceVersion,
                                    null,
                                    params.timeoutSeconds,
                                    params.watch,
                                    null);
                        },
                        V1Pod.class,
                        V1PodList.class);

        podInformer.addEventHandler(
                new ResourceEventHandler<V1Pod>() {
                    @Override
                    public void onAdd(V1Pod v1Pod) {
                        System.out.println("onAdd");
                    }

                    @Override
                    public void onUpdate(V1Pod v1Pod, V1Pod apiType1) {
                        System.out.println("onUpdate");
                    }

                    @Override
                    public void onDelete(V1Pod v1Pod, boolean b) {
                        System.out.println("onDelete");
                    }
                });

        factory.startAllRegisteredInformers();

//        V1Node nodeToCreate = new V1Node();
//        V1ObjectMeta metadata = new V1ObjectMeta();
//        metadata.setName("noxu");
//        nodeToCreate.setMetadata(metadata);
//        V1Node createdNode = coreV1Api.createNode(nodeToCreate, null, null, null);
//        Thread.sleep(3000);
//
//        Lister<V1Node> nodeLister = new Lister<V1Node>(nodeInformer.getIndexer());
//        V1Node node = nodeLister.get("noxu");
//        System.out.printf("noxu created! %s\n", node.getMetadata().getCreationTimestamp());
//        factory.stopAllRegisteredInformers();
//        Thread.sleep(3000);
//        System.out.println("informer stopped..");
    }
}