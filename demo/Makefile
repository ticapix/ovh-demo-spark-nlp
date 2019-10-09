ROOT_DIR:=$(shell dirname $(realpath $(lastword $(MAKEFILE_LIST))))
NAME=$(shell basename $(ROOT_DIR))
SBT=sbt
SPARK_SUBMIT=spark-submit
TIMEIT=time -o /tmp/time.log
ASSEMBLY=$(ROOT_DIR)/target/scala-2.11/DemoWiki-assembly-1.0.jar
SPARK_MASTER=$(shell cd ../cluster/ && ./terraform output master)

NUM_WORKER=8
NUM_CORE_PER_WORKER=12

.PHONY: help

help:
	@echo "$(NAME)"
	@grep -E '^[a-zA-Z_0-9-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-15s\033[0m=> %s\n", $$1, $$2}'

deps:
#	cd $(ROOT_DIR) && wget --continue https://s3.amazonaws.com/auxdata.johnsnowlabs.com/public/models/pos_anc_en_2.0.2_2.4_1556659930154.zip
#	cd $(ROOT_DIR) && wget --continue https://s3.amazonaws.com/auxdata.johnsnowlabs.com/public/models/lemma_antbnc_en_2.0.2_2.4_1556480454569.zip
#	swift post -r '.r:*,.rlistings' wiki

assembly: deps
	eval "echo \"`cat $(ROOT_DIR)/application.properties`\"" > $(ROOT_DIR)/src/main/resources/application.properties
	JAVA_OPTS='-Xmx2048m' $(SBT) assembly | tee assembly.log

hello-world: ## Hello World (setup check)
	$(MAKE) CLASS=com.ovh.pgronlie.HelloWorld _run

compute-schema-local: ## Compute XML schema from small dump
	$(MAKE) CLASS=com.ovh.pgronlie.ComputeSchema _run-local

preprocessing: ## extract stats from dumps
	$(MAKE) CLASS=com.ovh.pgronlie.PreProcessing _run

demo: ## run demo on extracted texts
	$(MAKE) CLASS=com.ovh.pgronlie.DemoWiki _run

demo-local: ## run demo on extracted texts
	$(MAKE) CLASS=com.ovh.pgronlie.DemoWiki _run-local

_run-local:
	$(TIMEIT) $(SPARK_SUBMIT) \
		--class $(CLASS) $(ASSEMBLY) swift://wiki.OVH/enwiki-20190801-pages-articles-multistream2.xml-p30304p88444 | tee run.log

#		--files pos_anc_en_2.0.2_2.4_1556659930154.zip,lemma_antbnc_en_2.0.2_2.4_1556480454569.zip \


_run:
	$(MAKE) -C ../cluster start-mon
	$(TIMEIT) $(SPARK_SUBMIT) \
	    --master spark://$(SPARK_MASTER):7077 \
	    --class $(CLASS) $(ASSEMBLY) swift://wiki.OVH/enwiki-20190801-pages-articles-multistream-500MB.xml || true
	$(MAKE) -C ../cluster stop-mon
#	    --executor-cores $(NUM_CORE_PER_WORKER) --total-executor-cores $$(( $(NUM_CORE_PER_WORKER) * $(NUM_WORKER))) \

clean:
	$(SBT) clean
	rm -rf target

