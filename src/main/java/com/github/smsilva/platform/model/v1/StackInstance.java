package com.github.smsilva.platform.model.v1;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

import java.util.Map;

@Version(StackInstance.VERSION)
@Group(StackInstance.GROUP)
public class StackInstance extends CustomResource<StackInstanceSpec, StackInstanceStatus> implements Namespaced {

    public static final String GROUP = "terraform.silvios.me";
    public static final String VERSION = "v1";

    @Override
    public String toString() {
        return "PlatformInstance{" +
                "spec=" + spec + ", " +
                "status=" + status +
                '}';
    }

    public String getName() {
        return this.getMetadata().getName();
    }

    public String getImage() {
        String registry = this.getSpec().getStack().getRegistry();
        String provider = this.getSpec().getStack().getProvider();
        String image = this.getSpec().getStack().getImage();
        String version = this.getSpec().getStack().getVersion();

        return registry + "/" + provider + "-" + image + ":" + version;
    }

    public String getProvider() {
        return this.getSpec().getStack().getProvider();
    }

    public String getNamespace() {
        return this.getMetadata().getNamespace();
    }

    public Iterable<? extends Map.Entry<String, String>> getVariablesAsEntrySet() {
        return this.getSpec().getVars().entrySet();
    }

    public String getSecretName() {
        return this.getProvider() + "-" + "credentials";
    }

}
