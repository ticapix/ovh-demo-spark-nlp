# OVH Labs - Automated monitored Spark cluster deployment for NLP processing

# Setup your VM

```shell
sudo apt-get install --yes python3-venv unzip jq
```

# Install Java-8 on Debian 10

From https://stackoverflow.com/questions/57031649/how-to-install-openjdk-8-jdk-on-debian-10-buster

```shell
echo 'APT::Default-Release "stable";' | sudo tee -a /etc/apt/apt.conf.d/99defaultrelease
```

```shell
cat <<EOF | sudo tee -a /etc/apt/sources.list.d/91-debian-unstable.list
deb http://deb.debian.org/debian sid main
deb-src http://deb.debian.org/debian sid main
EOF
```

```shell
sudo apt-get update
apt-cache policy openjdk-8-jdk
sudo apt-get install --yes -t unstable openjdk-8-jdk
```

To automate the last step and prevent the dialog box about auto-restarting service after the install

```shell
echo '* libraries/restart-without-asking boolean true' | sudo debconf-set-selections
```

And finally, configure your java environment (You don't need to do it if you haven't installed another java version)

```shell
sudo update-alternatives --config java
```

# Gather your access token

You need:

- an OpenStack user credential
- an OVH Metrics WRITE token
- an OVH Metrics READ token
- an OVH Logs WRITE token
- an OVH Logs token

Create a file `credentials.sh` based on the template `credentials_editme.sh` and source it `. ./credentials.sh`

# Compile spark

We are going to compile spark for Hadoop 3.2

```shell
make spark
```

# Start the cluster

```shell
make -C cluster cluster-create
```
