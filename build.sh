#!/bin/bash
mvn clean package

docker build --rm -t silviosilva/terraform-kubernetes-controller:latest .

docker push silviosilva/terraform-kubernetes-controller:latest

kubectl delete -f src/main/resources/deployment/terraform-controller &> /dev/null

kubectl apply -f src/main/resources/deployment/terraform-controller

sleep 10

kubectl logs -f -l app=terraform-operator

