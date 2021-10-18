#!/bin/bash
kubectl apply -f src/main/resources/crds/stack-instance.yaml && \
sleep 1 && \
clear && \
kubectl explain StackInstance.spec && \
kubectl delete StackInstance --all && \
echo "" && \
kubectl apply -f src/main/resources/crds/examples/stack-instance.yaml && \
echo "" && \
kubectl get StackInstance wasp-blue -o yaml | kubectl neat
