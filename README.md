# Terraform Kubernetes Custom Controller

## Setup

```bash
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
