$tcp = New-Object System.Net.Sockets.TcpClient('smtp.mail.yahoo.com', 587)
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
    $ssl.AuthenticateAsClient('smtp.mail.yahoo.com')
    Write-Host "Yahoo STARTTLS SUCCESS!"
} catch {
    Write-Host "Yahoo STARTTLS FAILED: " $_.Exception.Message
}
$tcp.Close()
