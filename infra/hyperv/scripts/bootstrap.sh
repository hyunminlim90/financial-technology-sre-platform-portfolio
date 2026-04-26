#!/usr/bin/env bash

set -euo pipefail

echo "[1/8] Update packages"
sudo yum update -y

echo "[2/8] Install common packages"
sudo yum install -y \
  git \
  curl \
  wget \
  vim \
  htop \
  net-tools \
  telnet \
  unzip \
  jq \
  yum-utils \
  device-mapper-persistent-data \
  lvm2 \
  conntrack \
  socat \
  iproute-tc

echo "[3/8] Install Docker"
if command -v amazon-linux-extras >/dev/null 2>&1; then
  sudo amazon-linux-extras install docker -y || true
else
  sudo yum install -y docker || true
fi

sudo systemctl enable docker
sudo systemctl start docker
sudo usermod -aG docker ec2-user || true

echo "[4/8] Configure Docker daemon"
sudo mkdir -p /etc/docker

cat <<EOF | sudo tee /etc/docker/daemon.json
{
  "exec-opts": ["native.cgroupdriver=systemd"],
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "100m"
  },
  "storage-driver": "overlay2"
}
EOF

sudo systemctl restart docker

echo "[5/8] Disable swap permanently"
sudo swapoff -a
sudo sed -i '/ swap / s/^/#/' /etc/fstab

echo "[6/8] Configure kernel modules for Kubernetes"
cat <<EOF | sudo tee /etc/modules-load.d/k8s.conf
br_netfilter
overlay
EOF

sudo modprobe br_netfilter
sudo modprobe overlay

echo "[7/8] Configure sysctl for Kubernetes networking"
cat <<EOF | sudo tee /etc/sysctl.d/k8s.conf
net.bridge.bridge-nf-call-iptables = 1
net.bridge.bridge-nf-call-ip6tables = 1
net.ipv4.ip_forward = 1
EOF

sudo sysctl --system

echo "[8/8] Verify"
docker --version || true
systemctl status docker --no-pager || true

echo "Bootstrap completed. Re-login is required for docker group permission."