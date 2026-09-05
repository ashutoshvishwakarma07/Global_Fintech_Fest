$tcp = New-Object System.Net.Sockets.TcpClient('smtp.gmail.com', 465)
$ssl = New-Object System.Net.Security.SslStream($tcp.GetStream(), $false)
try {
    $ssl.AuthenticateAsClient('smtp.gmail.com')
    Write-Host "SSL Handshake Succeeded!"
    $reader = New-Object System.IO.StreamReader($ssl)
    Write-Host "Banner: " $reader.ReadLine()
} catch {
    Write-Host "Error: " $_.Exception.Message
} finally {
    $tcp.Close()
}
