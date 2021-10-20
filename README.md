# Terraform Kubernetes Custom Controller

## Setup

```bash
# Check Kubernetes Contexts
kubectl config get-contexts

# Create a Kind Cluster (it should take less than 2 minutes)
create_kind_cluster() {
  KIND_CLUSTER_NAME="trash" && \
  kind create cluster \
    --config "kind-cluster-config.yaml" \
    --name "${KIND_CLUSTER_NAME?}" && \
  for NODE in $(kubectl get nodes --output name); do
    kubectl wait ${NODE} \
      --for condition=Ready \
      --timeout=360s
  done && \
  kubectl config get-contexts
}
time create_kind_cluster

# External Secrets Install
install_external_secrets() {
  helm repo add external-secrets https://external-secrets.github.io/kubernetes-external-secrets/ && \
  HELM_CHART_NEWEST_VERSION=$(helm search repo external-secrets -l | sed 1d | awk '{ print $2}' | sort --version-sort | tail --lines 1) && \
  echo "${HELM_CHART_NEWEST_VERSION}" && \
  helm install external-secrets external-secrets/kubernetes-external-secrets \
  --create-namespace \
  --namespace external-secrets \
  --version "${HELM_CHART_NEWEST_VERSION?}" \
  --wait
}
time install_external_secrets
```

## Create ARM Secret manually

```bash
# Configuring the Service Principal in Terraform
# https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/guides/service_principal_client_secret#configuring-the-service-principal-in-terraform

./create-azure-secret
```

## Build and Install terraform-controller

```bash
# Terminal [1]: Watch terraform-controller Deployment
watch -n 3 ./show_terraform_controller_and_stack_instances_information

# Terminal [2]: Build and Deploy terraform-controller
./build-and-install-terraform-controller
```

## Install Stack Instance CRD and Deploy a New Stack Instance Object

```bash
# Terminal [1]: Watch Stack Instance Information
watch -n 3 ./show_stack_instances_information

# Terminal [2]: Deploy the Stack Instance Again
./install-crd-and-create-a-new-stack-instance-object
```

## Cleanup

```bash
# Create a Kind Cluster (it should take less than 2 minutes)
if grep --quiet kind-trash <<< $(kubectl config get-contexts); then
  kind delete cluster --name trash
  kubectl config get-contexts
else
  echo "Kind trash cluser not found. Nothing to do."
fi
```

## References

Title                                                                                                                 | Location   | Type                                                                                                | Author       | Twitter                                           | Linkedin                                                                      | Github                                         
--------------------------------------------------------------------------------------------------------------------- | ---------- | --------------------------------------------------------------------------------------------------- | ------------ | ------------------------------------------------- | ----------------------------------------------------------------------------- | -----------------------------------------------
Inside of **Kubernetes Controller**                                                                                   | Slide Deck | [Presentation](https://speakerdeck.com/govargo/inside-of-kubernetes-controller?slide=42)            | Kenta Iso    | [@go_vargo](https://twitter.com/go_vargo)         |                                                                               | 
From Zero to **Kubernetes Operator**                                                                                  | Medium     | [Post](https://medium.com/@victorpaulo/from-zero-to-kubernetes-operator-dd06436b9d89)               | Victor Paulo | [@victorpaulo](https://twitter.com/victorpaulo)   | [victorpaulo](https://www.linkedin.com/in/victorpaulo/detail/contact-info/)   |
Creating a **Secret** using Environment Variables                                                                     | Github     | [Shell Script](https://github.com/smsilva/terraform-packager/blob/main/kubernetes/create-secret.sh) | Silvio Silva | [@silvio_silva](https://twitter.com/silvio_silva) | [silviomsilva](https://www.linkedin.com/in/silviomsilva/detail/contact-info/) | 
Terraform **Kubernetes Controller** using **Fabric8** Java Client                                                     | Github     | [Project](https://github.com/smsilva/terraform-kubernetes-controller)                               | Silvio Silva | [@silvio_silva](https://twitter.com/silvio_silva) | [silviomsilva](https://www.linkedin.com/in/silviomsilva/detail/contact-info/) | [smsilva](https://github.com/smsilva)
Simple **Kubernetes Operator** demonstrating Library Management using **Quarkus Fabric8 Kubernetes Client Extension** | Github     | [Project](https://github.com/rohanKanojia/librarybookoperatorinjava)                                | Rohan Kumar  | [@r0hankanojia](https://twitter.com/r0hankanojia) |                                                                               | [rohanKanojia](https://github.com/rohanKanojia)
