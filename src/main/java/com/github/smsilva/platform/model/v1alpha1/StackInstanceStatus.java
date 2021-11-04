package com.github.smsilva.platform.model.v1alpha1;

public class StackInstanceStatus {

    private String ready;

    public StackInstanceStatus() {
        this.ready = "false";
    }

    public String getReady() {
        return ready;
    }

    public void setReady(String ready) {
        this.ready = ready;
    }

    @Override
    public String toString() {
        return "StackInstanceStatus{" +
                "ready=" + ready +
                '}';
    }

}
