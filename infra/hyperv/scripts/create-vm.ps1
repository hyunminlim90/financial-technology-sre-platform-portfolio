# Hyper-V VM creation reference script
# 현재 VM 5대가 이미 생성되어 있으므로 이 스크립트는 재현용 문서 역할을 한다.

$SwitchName = "ExternalSwitch"

$VMs = @(
    @{ Name = "gateway"; IP = "172.30.1.105"; Role = "Nginx, React, Jenkins or GoCD, Webhook" },
    @{ Name = "app-node-1"; IP = "172.30.1.106"; Role = "Kubernetes Worker, Spring Boot WebFlux, Spring Batch" },
    @{ Name = "data-node"; IP = "172.30.1.107"; Role = "MySQL, Oracle XE, Redis, Kafka, Elasticsearch" },
    @{ Name = "observability-node"; IP = "172.30.1.108"; Role = "Prometheus, Grafana, ELK, Loki, Alertmanager" },
    @{ Name = "platform-node"; IP = "172.30.1.109"; Role = "Kubernetes Control Plane, ArgoCD, Istio, SRE Agent, RAG, LLM Gateway" }
)

foreach ($vm in $VMs) {
    Write-Host "VM: $($vm.Name)"
    Write-Host "IP: $($vm.IP)"
    Write-Host "Role: $($vm.Role)"
    Write-Host "Switch: $SwitchName"
    Write-Host "---------------------------"
}

# 실제 VM 생성 예시
# New-VM -Name "gateway" -MemoryStartupBytes 4GB -Generation 2 -SwitchName $SwitchName
# New-VHD -Path "D:\HyperV\gateway\gateway.vhdx" -SizeBytes 60GB -Dynamic
# Add-VMHardDiskDrive -VMName "gateway" -Path "D:\HyperV\gateway\gateway.vhdx"