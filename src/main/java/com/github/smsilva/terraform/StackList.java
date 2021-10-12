package com.github.smsilva.terraform;

import io.kubernetes.client.common.KubernetesListObject;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1ListMeta;
import io.kubernetes.client.openapi.models.V1ObjectMeta;

import java.util.List;

public class StackList implements KubernetesListObject {

    private V1ListMeta metadata;
    private List<? extends KubernetesObject> items;
    private String apiVersion;

    public void setMetadata(V1ListMeta metadata) {
        this.metadata = metadata;
    }

    public void setItems(List<? extends KubernetesObject> items) {
        this.items = items;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    @Override
    public V1ListMeta getMetadata() {
        return metadata;
    }

    @Override
    public List<? extends KubernetesObject> getItems() {
        return items;
    }

    @Override
    public String getApiVersion() {
        return apiVersion;
    }

    @Override
    public String getKind() {
        return null;
    }
    
}
