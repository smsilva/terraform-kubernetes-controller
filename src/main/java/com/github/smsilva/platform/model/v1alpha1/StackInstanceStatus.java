package com.github.smsilva.platform.model.v1alpha1;

public class StackInstanceStatus {

    private String message;

    public StackInstanceStatus() {
        this.message = "false";
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "StackInstanceStatus{" +
                "message=" + message +
                '}';
    }

}
