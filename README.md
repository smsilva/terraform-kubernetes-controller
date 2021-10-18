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
export ARM_CLIENT_ID="4ae86bb7-2cec-4489-a4f7-b7782c36a29f"
export ARM_CLIENT_SECRET="SERVICE_PRINCIPAL_CLIENT_HERE"
export ARM_SUBSCRIPTION_ID="855d4c74-bd8b-4124-bddd-472f0cc51dc2"
export ARM_TENANT_ID="14d80b68-d8b4-4bec-9997-f4f4c1c49977"

# https://www.terraform.io/docs/language/settings/backends/azurerm.html
export ARM_STORAGE_ACCOUNT_NAME="silvios"
export ARM_STORAGE_ACCOUNT_CONTAINER_NAME="terraform"
export ARM_SAS_TOKEN="AZURE_STORAGE_ACCOUNT_SAS_TOKEN_HERE"

BASE64ENCODED_ARM_SUBSCRIPTION_ID=$(               echo -n "${ARM_SUBSCRIPTION_ID?}"                | base64 | tr -d "\n") && \
BASE64ENCODED_ARM_TENANT_ID=$(                     echo -n "${ARM_TENANT_ID?}"                      | base64 | tr -d "\n") && \
BASE64ENCODED_ARM_CLIENT_ID=$(                     echo -n "${ARM_CLIENT_ID?}"                      | base64 | tr -d "\n") && \
BASE64ENCODED_ARM_CLIENT_SECRET=$(                 echo -n "${ARM_CLIENT_SECRET?}"                  | base64 | tr -d "\n") && \
BASE64ENCODED_ARM_STORAGE_ACCOUNT_NAME=$(          echo -n "${ARM_STORAGE_ACCOUNT_NAME?}"           | base64 | tr -d "\n") && \
BASE64ENCODED_ARM_STORAGE_ACCOUNT_CONTAINER_NAME=$(echo -n "${ARM_STORAGE_ACCOUNT_CONTAINER_NAME?}" | base64 | tr -d "\n") && \
BASE64ENCODED_ARM_SAS_TOKEN=$(                     echo -n "${ARM_SAS_TOKEN?}"                      | base64 | tr -d "\n") && \
kubectl apply -f - <<EOF
---
apiVersion: v1
kind: Secret
metadata:
  name: azure-credentials
type: Opaque
data:
  ARM_SUBSCRIPTION_ID:                ${BASE64ENCODED_ARM_SUBSCRIPTION_ID}
  ARM_TENANT_ID:                      ${BASE64ENCODED_ARM_TENANT_ID}
  ARM_CLIENT_ID:                      ${BASE64ENCODED_ARM_CLIENT_ID}
  ARM_CLIENT_SECRET:                  ${BASE64ENCODED_ARM_CLIENT_SECRET}
  ARM_STORAGE_ACCOUNT_NAME:           ${BASE64ENCODED_ARM_STORAGE_ACCOUNT_NAME}
  ARM_STORAGE_ACCOUNT_CONTAINER_NAME: ${BASE64ENCODED_ARM_STORAGE_ACCOUNT_CONTAINER_NAME}
  ARM_SAS_TOKEN:                      ${BASE64ENCODED_ARM_SAS_TOKEN}
EOF
```

## Build and Install terraform-controller

```bash
./build.sh
```

## Tests

Execute the `run.sh` script to test the **Controller** after **Composition** and/or **CRD** update.

```bash
./crd.sh
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
