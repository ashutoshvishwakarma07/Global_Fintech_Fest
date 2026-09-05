$ips = [System.Net.Dns]::GetHostAddresses('smtp.gmail.com')
foreach ($ip in $ips) {
    Write-Host "IP: $ip"
    try {
        # Add host route via Wi-Fi gateway 192.168.1.1
        route add "$($ip.IPAddressToString)" mask 255.255.255.255 192.168.1.1 metric 1 if 10
    } catch {
        Write-Host "Error adding route: $_"
    }
}
