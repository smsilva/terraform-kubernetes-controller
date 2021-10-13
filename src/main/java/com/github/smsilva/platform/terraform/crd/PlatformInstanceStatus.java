package com.github.smsilva.platform.terraform.crd;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(
        using = JsonDeserializer.None.class
)
public class PlatformInstanceStatus {

    private Boolean ready;
    private String message;

    public PlatformInstanceStatus() {
        this.ready = false;
        this.message = "";
    }

    public Boolean getReady() {
        return ready;
    }

    public void setReady(Boolean ready) {
        this.ready = ready;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
