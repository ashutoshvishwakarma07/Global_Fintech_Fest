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

# Validation callback that accepts the cert
$callback = {
    param($sender, $certificate, $chain, $sslPolicyErrors)
    Write-Host "Cert Subject: $($certificate.Subject)"
    return $true
}

$ssl = New-Object System.Net.Security.SslStream($stream, $false, $callback)
try {
    $ssl.AuthenticateAsClient('google.com')
    Write-Host ">>> SSL AUTHENTICATE SUCCESS! <<<"
    $sslReader = New-Object System.IO.StreamReader($ssl)
    $sslWriter = New-Object System.IO.StreamWriter($ssl)
    $sslWriter.AutoFlush = $true
    
    $sslWriter.WriteLine("EHLO localhost")
    while ($line = $sslReader.ReadLine()) {
        Write-Host "TLS EHLO resp: $line"
        if ($line -match "^\d{3}\s") { break }
    }
    
    # Test AUTH LOGIN
    $sslWriter.WriteLine("AUTH LOGIN")
    Write-Host "AUTH LOGIN resp: $($sslReader.ReadLine())"
    
    $uBase64 = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes('jyotilakhidhar96@gmail.com'))
    $sslWriter.WriteLine($uBase64)
    Write-Host "User resp: $($sslReader.ReadLine())"
    
    # Clean app password: remove spaces
    $pBase64 = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes('niwvyxmtutzxkdsi'))
    $sslWriter.WriteLine($pBase64)
    Write-Host "Pass resp: $($sslReader.ReadLine())"
    
} catch {
    Write-Host "Caught error: $_"
}
$tcp.Close()
