package com.github.smsilva.platform.controller;

import com.fasterxml.jackson.annotation.JsonValue;
import io.fabric8.kubernetes.api.model.MicroTime;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/*

How to create custom event for CRD? #3178
https://github.com/fabric8io/kubernetes-client/issues/3178

 */
public class WorkaroundMicroTime extends MicroTime {

    private static final DateTimeFormatter k8sMicroTime = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'.'SSSSSS'Z'XXX");
    private final String k8sFormattedMicroTime;

    public WorkaroundMicroTime(ZonedDateTime dateTime) {
        this.k8sFormattedMicroTime = k8sMicroTime.format(dateTime);
        System.out.println("#################### k8sFormattedMicroTime: " + this.k8sFormattedMicroTime);
        this.setTime(k8sFormattedMicroTime);
    }

    @JsonValue
    public String serialise() {
        return this.k8sFormattedMicroTime;
    }

}