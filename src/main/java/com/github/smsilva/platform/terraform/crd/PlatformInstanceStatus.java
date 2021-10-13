package com.github.smsilva.platform.terraform.crd;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(
        using = JsonDeserializer.None.class
)
public class PlatformInstanceStatus {

    private Boolean ready;

    public PlatformInstanceStatus() {
        this.ready = false;
    }

    public Boolean getReady() {
        return ready;
    }

    public void setReady(Boolean ready) {
        this.ready = ready;
    }

    @Override
    public String toString() {
        return "PlatformInstanceStatus{" +
                "ready=" + ready +
                '}';
    }

}
