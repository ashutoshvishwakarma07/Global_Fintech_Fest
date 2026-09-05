const net = require('net');

const servers = [
    { host: 'smtp.mail.yahoo.com', port: 587 },
    { host: 'smtp.office365.com', port: 587 },
    { host: 'smtp.zoho.com', port: 587 },
    { host: 'smtp.zoho.in', port: 587 },
    { host: 'mail.qualtechedge.com', port: 25 },
    { host: 'mail.qualtechedge.com', port: 587 },
    { host: '18.60.179.46', port: 25 },
    { host: '18.60.179.46', port: 587 },
    { host: '18.60.179.46', port: 8080 }
];

servers.forEach(s => {
    const sock = net.connect(s.port, s.host, () => {
        console.log(`CONNECTED: ${s.host}:${s.port}`);
    });
    sock.setTimeout(3000);
    sock.on('data', d => {
        console.log(`DATA from ${s.host}:${s.port} -> ${d.toString().trim().split('\n')[0]}`);
        sock.end();
    });
    sock.on('timeout', () => {
        console.log(`TIMEOUT: ${s.host}:${s.port}`);
        sock.destroy();
    });
    sock.on('error', e => {
        console.log(`ERROR ${s.host}:${s.port} -> ${e.message}`);
    });
});
