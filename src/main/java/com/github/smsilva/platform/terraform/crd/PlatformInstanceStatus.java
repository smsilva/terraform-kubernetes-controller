package com.github.smsilva.platform.terraform.crd;

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
