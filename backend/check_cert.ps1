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

# Validation callback that logs cert info
$callback = {
    param($sender, $certificate, $chain, $sslPolicyErrors)
    Write-Host "Cert Subject: $($certificate.Subject)"
    Write-Host "Cert Issuer: $($certificate.Issuer)"
    Write-Host "SSL Policy Errors: $sslPolicyErrors"
    return $true
}

$ssl = New-Object System.Net.Security.SslStream($stream, $false, $callback)
try {
    $ssl.AuthenticateAsClient('smtp.gmail.com')
    Write-Host ">>> SSL AUTHENTICATE SUCCESS! <<<"
} catch {
    Write-Host "Caught error: $_"
}
$tcp.Close()
