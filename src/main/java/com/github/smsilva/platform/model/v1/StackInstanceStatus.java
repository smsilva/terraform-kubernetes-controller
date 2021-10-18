package com.github.smsilva.platform.model.v1;

public class StackInstanceStatus {

    private Boolean ready;

    public StackInstanceStatus() {
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
        return "StackInstanceStatus{" +
                "ready=" + ready +
                '}';
    }

}
