const tls = require('tls');

console.log("Testing IPv6 connection to smtp.gmail.com [2404:6800:4013:813::6d]:465...");
const socket = tls.connect({
    host: '2404:6800:4013:813::6d',
    port: 465,
    servername: 'smtp.gmail.com',
    rejectUnauthorized: false
}, () => {
    console.log(">>> IPv6 TLS CONNECTED SUCCESSFULLY! <<<");
    socket.setEncoding('utf8');
    socket.on('data', (data) => {
        console.log("Received data:", data);
        socket.end();
    });
});

socket.on('error', (err) => {
    console.error("IPv6 Error:", err.message);
});

setTimeout(() => {
    socket.destroy();
}, 5000);
