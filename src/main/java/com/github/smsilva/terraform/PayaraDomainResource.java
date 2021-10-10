package com.github.smsilva.terraform;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Plural;
import io.fabric8.kubernetes.model.annotation.Singular;
import io.fabric8.kubernetes.model.annotation.Version;

@Plural("domains")
@Singular("domains")
@Group("poc.payara.fish")
@Version("v1alpha")
public class PayaraDomainResource extends CustomResource<PayaraDomainSpec, Void> implements Namespaced {
}
