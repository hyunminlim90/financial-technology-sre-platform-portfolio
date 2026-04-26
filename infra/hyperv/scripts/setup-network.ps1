# Hyper-V ExternalSwitch 확인 및 네트워크 설계 기록용 스크립트

$SwitchName = "ExternalSwitch"

Write-Host "Check Hyper-V switch"
Get-VMSwitch | Where-Object { $_.Name -eq $SwitchName }

Write-Host ""
Write-Host "Expected VM IP allocation"
Write-Host "gateway            172.30.1.105"
Write-Host "app-node-1         172.30.1.106"
Write-Host "data-node          172.30.1.107"
Write-Host "observability-node 172.30.1.108"
Write-Host "platform-node      172.30.1.109"

Write-Host ""
Write-Host "Kubernetes Control Plane"
Write-Host "172.30.1.109:6443"

Write-Host ""
Write-Host "Pod CIDR"
Write-Host "192.168.0.0/16"