## 0.6.1 (2022-02-21)

### Fix

- **outputs**: deal with outputs as strings

## 0.6.0 (2022-02-04)

### Feat

- **controller**: accept variables as they are

## 0.5.3 (2022-02-03)

### Fix

- **controller**: create terraform tfvars using variables input

## 0.5.2 (2022-02-03)

### Fix

- **crds**: update stackinstance crd

## 0.5.1 (2022-02-01)

### Fix

- **pom.xml**: rollback client version

## 0.5.0 (2022-02-01)

### Feat

- **pom.xml**: update dependencies

## 0.4.4 (2021-11-17)

### Fix

- **secret**: remove secret after stack instance is deleted

## 0.4.3 (2021-11-17)

### Fix

- **charts**: helm chart updated with the latest version[

## 0.4.2 (2021-11-16)

### Fix

- **delete**: not save outputs when deleting a stack instance

## 0.4.1 (2021-11-16)

### Refactor

- **outputs**: Refactor outputs to ConfigMap and Secret

## 0.4.0 (2021-11-16)

### Feat

- **secrets**: create secret for sensitive output

## 0.3.2 (2021-11-11)

### Refactor

- **helm-chart**: Update Helm Chart for terraform-controller to use values.image.tag as latest

## 0.3.1 (2021-11-04)

### Fix

- **crd**: Scripts was moved to development folder. StackInstance version was fixed to Kubernetes pattern v1alpha1

## 0.3.0 (2021-11-02)

### Feat

- **stack-instance-delete**: Stack Instance Exclusion now is replicated by controller

## 0.2.1 (2021-11-02)

### Fix

- **controller**: Fix log message to show when the controller becomes ready

## 0.2.0 (2021-11-02)

### Feat

- **helm-chart**: Adding a Helm Chart to Install the Controller

## 0.1.0 (2021-11-01)

### Feat

- **stack-instance-controller**: First Version of StackInstance Controller that is capable to Create and Update a Stack Instance using Terraform Stack Images

## 0.0.2 (2021-10-10)

### Fix

- **maven**: Added Maven Support
