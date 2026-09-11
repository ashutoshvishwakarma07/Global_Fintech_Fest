$reportFile = "reports\Today_OCR_Report_2026-09-09.xlsx"
if (Test-Path $reportFile) {
    $excelBytes = [System.IO.File]::ReadAllBytes((Resolve-Path $reportFile))
} else {
    $excelBytes = [System.Text.Encoding]::UTF8.GetBytes("GFF Visiting Card Sample Report")
}

$boundary = "----=_Part_" + [System.Guid]::NewGuid().ToString("N")
$dateStr = (Get-Date).ToString("yyyy-MM-dd")

$htmlContent = @"
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <style>
    body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 20px; }
    .container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 8px; overflow: hidden; border: 1px solid #e2e8f0; }
    .header { background: #1e3a8a; color: #ffffff; padding: 20px; }
    .header h2 { margin: 0; font-size: 18px; }
    .header p { margin: 4px 0 0 0; font-size: 12px; opacity: 0.8; }
    .content { padding: 20px; }
    .user-table { width: 100%; border-collapse: collapse; margin: 16px 0; border: 1px solid #e2e8f0; border-radius: 6px; overflow: hidden; }
    .user-table th { background: #f1f5f9; padding: 10px 14px; font-size: 12px; font-weight: bold; color: #475569; text-transform: uppercase; border-bottom: 1px solid #e2e8f0; }
    .user-table tfoot td { background: #f8fafc; padding: 10px 14px; font-size: 13px; font-weight: bold; color: #0f172a; border-top: 2px solid #cbd5e1; }
    .cta-box { background: #eff6ff; border-left: 4px solid #2563eb; padding: 12px 16px; border-radius: 4px; margin-top: 15px; }
    .footer { text-align: center; padding: 15px; font-size: 11px; color: #94a3b8; border-top: 1px solid #f1f5f9; }
  </style>
</head>
<body>
  <div class="container">
    <div class="header">
      <h2>Global Fintech Fest (GFF) — Today's Report</h2>
      <p>Date: $dateStr | Report Generated Automatically</p>
    </div>
    <div class="content">
      <p style="color: #334155; font-size: 14px; margin: 0 0 10px 0;">Hi,</p>
      <p style="color: #64748b; font-size: 13px; line-height: 1.5; margin: 0 0 15px 0;">
        Please find below the visiting card OCR upload and processing summary:
      </p>

      <table class="user-table">
        <thead>
          <tr>
            <th style="text-align: left;">User / Uploader</th>
            <th style="text-align: right;">Cards Uploaded</th>
            <th style="text-align: right;">Share</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td colspan="3" style="padding: 12px 14px; text-align: center; color: #94a3b8;">No uploader data available</td>
          </tr>
        </tbody>
        <tfoot>
          <tr>
            <td style="text-align: left;">Total</td>
            <td style="text-align: right; color: #2563eb;">0</td>
            <td style="text-align: right;">100.0%</td>
          </tr>
        </tfoot>
      </table>

      <div class="cta-box">
        <p style="margin: 0; font-size: 13px; font-weight: bold; color: #1e40af;">📎 Detailed Report Attached</p>
        <p style="margin: 4px 0 0 0; font-size: 12px; color: #3b82f6;">
          All extracted contact fields are compiled in the attached <strong>Today_OCR_Report_$dateStr.xlsx</strong> spreadsheet.
        </p>
      </div>
    </div>
    <div class="footer">
      Generated automatically by GFF Backend Service.<br/>
      Confidential &copy; 2026 Global Fintech Fest.
    </div>
  </div>
</body>
</html>
"@

$base64Excel = [Convert]::ToBase64String($excelBytes)

$mimeData = @"
From: visiting.cardapp@qualtechedge.in
To: jyoti.sonani@qualtechedge.com, ashutosh.vishwakarma@qualtechedge.com
Subject: [Today's Report] OCR Processing & Upload Summary - $dateStr
MIME-Version: 1.0
Content-Type: multipart/mixed; boundary="$boundary"

--$boundary
Content-Type: text/html; charset=UTF-8
Content-Transfer-Encoding: 7bit

$htmlContent

--$boundary
Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet; name="Today_OCR_Report_$dateStr.xlsx"
Content-Disposition: attachment; filename="Today_OCR_Report_$dateStr.xlsx"
Content-Transfer-Encoding: base64

$base64Excel

--$boundary--
"@

Write-Host "Connecting to smtp.gmail.com:587..."
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

$callback = { param($s, $c, $ch, $e) return $true }
$ssl = New-Object System.Net.Security.SslStream($stream, $false, $callback)
$ssl.AuthenticateAsClient('smtp.gmail.com')

$sslReader = New-Object System.IO.StreamReader($ssl)
$sslWriter = New-Object System.IO.StreamWriter($ssl)
$sslWriter.AutoFlush = $true

$sslWriter.WriteLine("EHLO localhost")
while ($line = $sslReader.ReadLine()) {
    if ($line -match "^\d{3}\s") { break }
}

$sslWriter.WriteLine("AUTH LOGIN")
$sslReader.ReadLine()
$sslWriter.WriteLine([Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes('visiting.cardapp@qualtechedge.in')))
$sslReader.ReadLine()
$sslWriter.WriteLine([Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes('seto dqat wfoi jovo')))
$auth = $sslReader.ReadLine()
Write-Host "Auth result: $auth"

$sslWriter.WriteLine("MAIL FROM:<visiting.cardapp@qualtechedge.in>")
$sslReader.ReadLine()
# Active Recipients:
$sslWriter.WriteLine("RCPT TO:<jyoti.sonani@qualtechedge.com>")
$sslReader.ReadLine()
$sslWriter.WriteLine("RCPT TO:<ashutosh.vishwakarma@qualtechedge.com>")
$sslReader.ReadLine()
$sslWriter.WriteLine("DATA")
$sslReader.ReadLine()
$sslWriter.WriteLine($mimeData)
$sslWriter.WriteLine(".")
$res = $sslReader.ReadLine()
Write-Host "DELIVERY RESULT: $res"

$sslWriter.WriteLine("QUIT")
$tcp.Close()
