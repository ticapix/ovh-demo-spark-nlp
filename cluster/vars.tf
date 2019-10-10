provider "openstack" {
  domain_name = "default" # Domain Name - Always "default" for OVH
  alias = "ovh" # An alias
}

variable "keypair" {
  default = "spark-cluster"
}

variable "worker_num" {
  default = 12
}

variable "flavor" {
  default = "c2-60"
}

variable "username" {
  default = "spark"
}