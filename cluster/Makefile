ROOT_DIR:=$(shell dirname $(realpath $(lastword $(MAKEFILE_LIST))))
PYTHON3=python3
VENV_DIR=$(ROOT_DIR)/venv3
NAME=$(shell basename $(ROOT_DIR))
ECHO=@echo
RM=rm -rf
TERRAFORM_VERSION=0.12.5

.PHONY: help

help:
	$(ECHO) "$(NAME)"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-15s\033[0m=> %s\n", $$1, $$2}'

$(VENV_DIR):
	$(PYTHON3) -m venv $(VENV_DIR)
	$(VENV_DIR)/bin/pip install --upgrade pip
	$(VENV_DIR)/bin/pip install -Ur requirements.txt

install: $(VENV_DIR) ## install dependencies
	ls $(ROOT_DIR)/terraform.zip || wget https://releases.hashicorp.com/terraform/$(TERRAFORM_VERSION)/terraform_$(TERRAFORM_VERSION)_linux_amd64.zip -O $(ROOT_DIR)/terraform.zip
	ls $(ROOT_DIR)/terraform || unzip -ou $(ROOT_DIR)/terraform.zip
 
tf-output:  ## Terraform output
	. $(ROOT_DIR)/venv3/bin/activate && PATH="`pwd`:$$PATH" terraform output

cluster-create: install  ## create cluster
	. $(ROOT_DIR)/venv3/bin/activate && PATH="`pwd`:$$PATH" terraform init
	. $(ROOT_DIR)/venv3/bin/activate && PATH="`pwd`:$$PATH" terraform apply -auto-approve
	. $(ROOT_DIR)/venv3/bin/activate && PATH="`pwd`:$$PATH" ansible-playbook --verbose --verbose --extra-vars @vars.yml main.yml
	$(MAKE) stop-mon

cluster-stop: ## delete worker nodes only
	. $(ROOT_DIR)/venv3/bin/activate && PATH="`pwd`:$$PATH" $(ROOT_DIR)/terraform destroy -auto-approve -target openstack_compute_instance_v2.workers

cluster-destroy: cluster-stop ## delete all nodes
	. $(ROOT_DIR)/venv3/bin/activate && PATH="`pwd`:$$PATH" $(ROOT_DIR)/terraform destroy -auto-approve

cluster-reset: cluster-stop ## reinstall cluster keeping the same public ip
	. $(ROOT_DIR)/venv3/bin/activate && PATH="`pwd`:$$PATH" $(ROOT_DIR)/terraform destroy -auto-approve -target openstack_compute_instance_v2.master
	$(MAKE) cluster-create

stop-mon: ## stop monitoring
	. $(ROOT_DIR)/venv3/bin/activate && PATH="`pwd`:$$PATH" ansible-playbook --extra-vars @vars.yml --extra-vars 'state=stopped' monitoring.yml

start-mon: tf-output ## start monitoring
	. $(ROOT_DIR)/venv3/bin/activate && PATH="`pwd`:$$PATH" ansible-playbook --extra-vars @vars.yml --extra-vars 'state=restarted' monitoring.yml
