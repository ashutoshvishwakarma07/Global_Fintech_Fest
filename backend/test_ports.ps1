@('smtp.gmail.com', 'smtp.office365.com') | ForEach-Object {
    $hostName = $_
    @(25, 465, 587, 2525) | ForEach-Object {
        $port = $_
        try {
            $t = New-Object System.Net.Sockets.TcpClient
            $async = $t.BeginConnect($hostName, $port, $null, $null)
            $success = $async.AsyncWaitHandle.WaitOne(2000, $false)
            if ($success -and $t.Connected) {
                Write-Host "$hostName : $port -> CONNECTED"
                $stream = $t.GetStream()
                $reader = New-Object System.IO.StreamReader($stream)
                $stream.ReadTimeout = 2000
                try {
                    $banner = $reader.ReadLine()
                    Write-Host "    Banner: $banner"
                } catch {
                    Write-Host "    Read error: $($_.Exception.Message)"
                }
            } else {
                Write-Host "$hostName : $port -> TIMEOUT/REFUSED"
            }
            $t.Close()
        } catch {
            Write-Host "$hostName : $port -> ERROR: $($_.Exception.Message)"
        }
    }
}
