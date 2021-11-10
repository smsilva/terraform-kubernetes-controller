[![build](https://github.com/smsilva/terraform-kubernetes-controller/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/smsilva/terraform-kubernetes-controller/actions/workflows/ci.yml)

# Terraform Kubernetes Custom Controller

## Setup

```bash
# Terminal [1]
scripts/development/create_azure_secret &&
scripts/development/create_google_secret && \
kubectl apply -f manifests/charts/terraform-controller/crds/stackinstance.yaml && \
sleep 2 && \
watch -n 3 scripts/development/show_stack_instances_information

# Terminal [2]
helm template manifests/charts/google-bucket | kubectl apply -f -
helm template manifests/charts/azure-bucket | kubectl apply -f -
```

### From this repository

```bash
# Terminal [1]: Watch Stack Instance Information
watch -n 3 scripts/development/show_stack_instances_information

# Terminal [2]: Deploy the Stack Instance Again
scripts/development/install_crd_and_create_a_new_stack_instance_object

# Retrieve apply and output logs
kubectl get ConfigMap generic-bucket-1 -o json | jq '.data."apply.log"'  -r
kubectl get ConfigMap generic-bucket-1 -o json | jq '.data."output.log"' -r

# Tests using Helm
helm template manifests/charts/google-bucket | kubectl apply -f -

# Check Stack Instances Created
kubectl get StackInstances
```

## Cleanup

```bash
scripts/development/delete_kind_cluster
```

## References

Title                                                                                                                 | Location   | Type                                                                                                | Author       | Twitter                                           | Linkedin                                                                      | Github                                         
--------------------------------------------------------------------------------------------------------------------- | ---------- | --------------------------------------------------------------------------------------------------- | ------------ | ------------------------------------------------- | ----------------------------------------------------------------------------- | -----------------------------------------------
Inside of **Kubernetes Controller**                                                                                   | Slide Deck | [Presentation](https://speakerdeck.com/govargo/inside-of-kubernetes-controller?slide=42)            | Kenta Iso    | [@go_vargo](https://twitter.com/go_vargo)         |                                                                               | 
From Zero to **Kubernetes Operator**                                                                                  | Medium     | [Post](https://medium.com/@victorpaulo/from-zero-to-kubernetes-operator-dd06436b9d89)               | Victor Paulo | [@victorpaulo](https://twitter.com/victorpaulo)   | [victorpaulo](https://www.linkedin.com/in/victorpaulo/detail/contact-info/)   |
Creating a **Secret** using Environment Variables                                                                     | Github     | [Shell Script](https://github.com/smsilva/terraform-packager/blob/main/kubernetes/create-secret.sh) | Silvio Silva | [@silvio_silva](https://twitter.com/silvio_silva) | [silviomsilva](https://www.linkedin.com/in/silviomsilva/detail/contact-info/) | 
Terraform **Kubernetes Controller** using **Fabric8** Java Client                                                     | Github     | [Project](https://github.com/smsilva/terraform-kubernetes-controller)                               | Silvio Silva | [@silvio_silva](https://twitter.com/silvio_silva) | [silviomsilva](https://www.linkedin.com/in/silviomsilva/detail/contact-info/) | [smsilva](https://github.com/smsilva)
Simple **Kubernetes Operator** demonstrating Library Management using **Quarkus Fabric8 Kubernetes Client Extension** | Github     | [Project](https://github.com/rohanKanojia/librarybookoperatorinjava)                                | Rohan Kumar  | [@r0hankanojia](https://twitter.com/r0hankanojia) |                                                                               | [rohanKanojia](https://github.com/rohanKanojia)
