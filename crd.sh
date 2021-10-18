#!/bin/bash
kubectl apply -f src/main/resources/crds/stack-instance.yaml && \
sleep 1 && \
clear && \
kubectl explain StackInstance.spec && \
kubectl delete StackInstance --all && \
echo "" && \
kubectl apply -f src/main/resources/examples/stack-instance.yaml && \
echo "" && \
kubectl get StackInstance africa-1 -o yaml | kubectl neat
kubectl get StackInstances
