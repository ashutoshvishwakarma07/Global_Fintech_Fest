$hostName = 'smtp.gmail.com'
@('smtp.gmail.com', 'gmail.com', 'google.com', 'mail.google.com') | ForEach-Object {
    $sni = $_
    try {
        $tcp = New-Object System.Net.Sockets.TcpClient($hostName, 587)
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
        $ssl.AuthenticateAsClient($sni)
        Write-Host "SNI '$sni' -> SUCCESS!"
        $tcp.Close()
    } catch {
        Write-Host "SNI '$sni' -> FAILED: $($_.Exception.Message)"
    }
}
