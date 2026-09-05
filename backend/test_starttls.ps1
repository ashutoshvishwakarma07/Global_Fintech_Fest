$tcp = New-Object System.Net.Sockets.TcpClient('smtp.gmail.com', 587)
$stream = $tcp.GetStream()
$reader = New-Object System.IO.StreamReader($stream)
$writer = New-Object System.IO.StreamWriter($stream)
$writer.AutoFlush = $true

Write-Host "Banner: " $reader.ReadLine()

$writer.WriteLine("EHLO localhost")
while ($line = $reader.ReadLine()) {
    Write-Host "EHLO resp: $line"
    if ($line -match "^\d{3}\s") { break }
}

$writer.WriteLine("STARTTLS")
$tlsResp = $reader.ReadLine()
Write-Host "STARTTLS resp: $tlsResp"

if ($tlsResp -like "220*") {
    Write-Host "Starting SSL handshake..."
    $ssl = New-Object System.Net.Security.SslStream($stream, $false)
    try {
        $ssl.AuthenticateAsClient('smtp.gmail.com')
        Write-Host "SSL Handshake on Port 587 Succeeded!"
        $sslReader = New-Object System.IO.StreamReader($ssl)
        $sslWriter = New-Object System.IO.StreamWriter($ssl)
        $sslWriter.AutoFlush = $true
        $sslWriter.WriteLine("EHLO localhost")
        while ($line = $sslReader.ReadLine()) {
            Write-Host "TLS EHLO resp: $line"
            if ($line -match "^\d{3}\s") { break }
        }
    } catch {
        Write-Host "SSL Handshake Error: " $_.Exception.ToString()
    }
}
$tcp.Close()
