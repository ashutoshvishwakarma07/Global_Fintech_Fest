@('smtp-relay.gmail.com', 'aspmx.l.google.com') | ForEach-Object {
    $h = $_
    try {
        $tcp = New-Object System.Net.Sockets.TcpClient($h, 587)
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
        $ssl.AuthenticateAsClient($h)
        Write-Host "$h STARTTLS SUCCESS!"
        $tcp.Close()
    } catch {
        Write-Host "$h FAILED: $($_.Exception.Message)"
    }
}
