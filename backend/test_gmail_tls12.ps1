$tcp = New-Object System.Net.Sockets.TcpClient('smtp.gmail.com', 587)
$stream = $tcp.GetStream()
$reader = New-Object System.IO.StreamReader($stream)
$writer = New-Object System.IO.StreamWriter($stream)
$writer.AutoFlush = $true
$reader.ReadLine()
$writer.WriteLine("EHLO localhost")
while ($line = $reader.ReadLine()) {
    if ($line -match "^\d{3}\s") { break }
}
$writer.WriteLine("STARTTLS")
$reader.ReadLine()
$ssl = New-Object System.Net.Security.SslStream($stream, $false)
try {
    # Force TLS 1.2
    $ssl.AuthenticateAsClient('smtp.gmail.com', $null, [System.Security.Authentication.SslProtocols]::Tls12, $false)
    Write-Host "Gmail TLS 1.2 SUCCESS!"
} catch {
    Write-Host "Gmail TLS 1.2 FAILED: " $_.Exception.ToString()
}
$tcp.Close()
