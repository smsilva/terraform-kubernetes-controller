package com.github.smsilva.platform.terraform.crd;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;

@JsonDeserialize(
        using = JsonDeserializer.None.class
)
public class PlatformInstanceSpec implements KubernetesResource {

    private String region;
    private String provider;

    @Override
    public String toString() {
        return "PlatformInstanceSpec{" +
                "region='" + region + '\'' + "," +
                "provider='" + provider + '\'' +
                '}';
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

}