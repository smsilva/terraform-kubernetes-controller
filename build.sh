#!/bin/bash
docker build --rm -t silviosilva/terraform-operator:latest .
docker push silviosilva/terraform-operator:latest
