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

spark: ## compile spark
	$(ROOT_DIR)/_compile_spark.sh

spark-clean: ## clean spark generated files and folders
	$(RM) spark*
	$(RM) sbt*
