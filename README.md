# Terraform Kubernetes Custom Controller

## Setup

```bash
# Create a Kind Cluster (it should take less than 2 minutes)
scripts/create-kind-cluster
```

## Create ARM Secret manually

```bash
# Configuring the Service Principal in Terraform
# https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs/guides/service_principal_client_secret#configuring-the-service-principal-in-terraform
scripts/create-azure-secret
scripts/create-google-secret
```

## Install Stack Instance CRD and Deploy a New Stack Instance Object

```bash
# Terminal [1]: Watch Stack Instance Information
watch -n 3 scripts/show_stack_instances_information

# Terminal [2]: Deploy the Stack Instance Again
scripts/install-crd-and-create-a-new-stack-instance-object

# Retrieve apply and output logs
kubectl get cm africa-1 -o json | jq '.data."apply.log"' -r
kubectl get cm africa-1 -o json | jq '.data."output.log"' -r

# Test using Helm
helm template src/main/resources/examples/stack-instances | kubectl apply -f -
```

## Build and Install terraform-controller

```bash
# Terminal [1]: Watch terraform-controller Deployment
watch -n 3 scripts/show_terraform_controller_and_stack_instances_information

# Terminal [2]: Build and Deploy terraform-controller
./build-and-install-terraform-controller
```

## Scenario

```bash
wasp platform instance create \
  --name "africa-1" \
  --region "southafrica" \
  --provider "azure"

wasp platform instance list

wasp cluster create \
  --platform-instance "africa-1" \
  --ingress-cname "wasp-services"
  --name "k8s-green" \
  --version "1.20.7" \

```

## Cleanup

```bash
scripts/delete-kind-cluster
```

## References

Title                                                                                                                 | Location   | Type                                                                                                | Author       | Twitter                                           | Linkedin                                                                      | Github                                         
--------------------------------------------------------------------------------------------------------------------- | ---------- | --------------------------------------------------------------------------------------------------- | ------------ | ------------------------------------------------- | ----------------------------------------------------------------------------- | -----------------------------------------------
Inside of **Kubernetes Controller**                                                                                   | Slide Deck | [Presentation](https://speakerdeck.com/govargo/inside-of-kubernetes-controller?slide=42)            | Kenta Iso    | [@go_vargo](https://twitter.com/go_vargo)         |                                                                               | 
From Zero to **Kubernetes Operator**                                                                                  | Medium     | [Post](https://medium.com/@victorpaulo/from-zero-to-kubernetes-operator-dd06436b9d89)               | Victor Paulo | [@victorpaulo](https://twitter.com/victorpaulo)   | [victorpaulo](https://www.linkedin.com/in/victorpaulo/detail/contact-info/)   |
Creating a **Secret** using Environment Variables                                                                     | Github     | [Shell Script](https://github.com/smsilva/terraform-packager/blob/main/kubernetes/create-secret.sh) | Silvio Silva | [@silvio_silva](https://twitter.com/silvio_silva) | [silviomsilva](https://www.linkedin.com/in/silviomsilva/detail/contact-info/) | 
Terraform **Kubernetes Controller** using **Fabric8** Java Client                                                     | Github     | [Project](https://github.com/smsilva/terraform-kubernetes-controller)                               | Silvio Silva | [@silvio_silva](https://twitter.com/silvio_silva) | [silviomsilva](https://www.linkedin.com/in/silviomsilva/detail/contact-info/) | [smsilva](https://github.com/smsilva)
Simple **Kubernetes Operator** demonstrating Library Management using **Quarkus Fabric8 Kubernetes Client Extension** | Github     | [Project](https://github.com/rohanKanojia/librarybookoperatorinjava)                                | Rohan Kumar  | [@r0hankanojia](https://twitter.com/r0hankanojia) |                                                                               | [rohanKanojia](https://github.com/rohanKanojia)
