#
# NETWORK
#

data "openstack_networking_network_v2" "ext-network" {
  name = "Ext-Net"
}

#
# KEYPAIR
#

resource "openstack_compute_keypair_v2" "keypair" {
  name = "${var.keypair}"
}

#
# CLOUDINIT CONFIGS
#

data "template_file" "cloudinit" {
  template      = "${file("${path.module}/cloudinit.cfg")}"
  vars = {
    username    = "${var.username}"
    keypair     = "${openstack_compute_keypair_v2.keypair.public_key}"
  }
}

data "template_cloudinit_config" "cloudinit_config" {
  # gzip          = true
  # base64_encode = true
  part {
    filename     = "init.cfg"
    content_type = "text/cloud-config"
    content      = "${data.template_file.cloudinit.rendered}"
  }
}

#
# MASTER NODE
#

resource "openstack_networking_port_v2" "port_master_ext" {
  network_id     = "${data.openstack_networking_network_v2.ext-network.id}"
  admin_state_up = "true"
}

resource "openstack_compute_instance_v2" "master" {
  name = "spark-master" # Instance's name
  provider = "openstack.ovh" # Provider's name
  image_name = "Ubuntu 19.04" # Image's name
  flavor_name = "b2-15" # Flavor's name
  key_pair ="${var.keypair}"
  network {port = "${openstack_networking_port_v2.port_master_ext.id}"}
  user_data = "${data.template_cloudinit_config.cloudinit_config.rendered}"
#   depends_on = ["openstack_networking_subnet_v2.subnet"]
}

#
# WORKER NODES
#

resource "openstack_compute_instance_v2" "workers" {
  count = "${var.worker_num}"
  name = "spark-worker-${count.index}" # Instance's name
  provider = "openstack.ovh" # Provider's name
  image_name = "Ubuntu 19.04" # Image's name
  flavor_name = "${var.flavor}" # Flavor's name
  key_pair ="${var.keypair}"
  network {uuid = "${data.openstack_networking_network_v2.ext-network.id}"}
  user_data = "${data.template_cloudinit_config.cloudinit_config.rendered}"
}
#
# OUTPUT
#

output "username" {
  value = "${var.username}"
}

output "master" {
  value = "${openstack_compute_instance_v2.master.network.0.fixed_ip_v4}"
}

output "workers" {
  value = "${openstack_compute_instance_v2.workers.*.network.0.fixed_ip_v4}"
}

output "key" {
  value = "${
    map(
      "public", openstack_compute_keypair_v2.keypair.public_key,
      "private", openstack_compute_keypair_v2.keypair.private_key
    )
    }"
}