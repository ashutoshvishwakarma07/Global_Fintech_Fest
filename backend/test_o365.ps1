$tcp = New-Object System.Net.Sockets.TcpClient('smtp.office365.com', 587)
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
    $ssl.AuthenticateAsClient('smtp.office365.com')
    Write-Host "Office365 STARTTLS SUCCESS!"
} catch {
    Write-Host "Office365 STARTTLS FAILED: " $_.Exception.Message
}
$tcp.Close()
