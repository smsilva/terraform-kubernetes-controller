# Terraform Kubernetes Custom Controller

## Setup

```bash
# Create a Kind Cluster (it should take less than 2 minutes)
scripts/create-kind-cluster
```

## Create Terraform Providers Secrets manually

```bash
scripts/create-google-secret
```

## Install Stack Instance CRD and Deploy a New Stack Instance Object

### Using Helm

```bash
helm repo add terraform-controller https://smsilva.github.io/helm/

helm repo list

helm repo update

helm install terraform-controller terraform-controller/terraform-controller

kubectl wait \
  deployment terraform-controller \
  --for=condition=Available \
  --timeout=360s

helm list


```

### From this repository

```bash
# Terminal [1]: Watch Stack Instance Information
watch -n 3 scripts/show_stack_instances_information

# Terminal [2]: Deploy the Stack Instance Again
scripts/install-crd-and-create-a-new-stack-instance-object

# Retrieve apply and output logs
kubectl get ConfigMap generic-bucket-1 -o json | jq '.data."apply.log"'  -r
kubectl get ConfigMap generic-bucket-1 -o json | jq '.data."output.log"' -r

# Tests using Helm
helm template src/main/resources/examples/google/helm-chart | kubectl apply -f -

# Check Stack Instances Created
kubectl get StackInstances
```

## Build and Install terraform-controller

```bash
# Terminal [1]: Watch terraform-controller Deployment
watch -n 3 scripts/show_terraform_controller_and_stack_instances_information

# Terminal [2]: Build and Deploy terraform-controller
scripts/build_and_install_terraform_controller
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
