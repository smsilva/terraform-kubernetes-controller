package com.github.smsilva.platform.terraform.crd;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

@Version(PlatformInstance.VERSION)
@Group(PlatformInstance.GROUP)
public class PlatformInstance extends CustomResource<PlatformInstanceSpec, PlatformInstanceStatus> implements Namespaced {

    public static final String GROUP = "silvios.me";
    public static final String VERSION = "v1";

    public PlatformInstance() {
        this.status = new PlatformInstanceStatus();
    }

    @Override
    public String toString() {
        return "PlatformInstance{" +
                "spec=" + spec + ", " +
                "status=" + status +
                '}';
    }
}
