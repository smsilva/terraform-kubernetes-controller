package com.github.smsilva.platform.example.crd;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

@Version(Dummy.VERSION)
@Group(Dummy.GROUP)
public class Dummy extends CustomResource<DummySpec, Void> implements Namespaced {
    public static final String GROUP = "demo.fabric8.io";
    public static final String VERSION = "v1";
}
