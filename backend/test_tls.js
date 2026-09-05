const tls = require('tls');

console.log("Testing TLS connection to smtp.gmail.com:465 via Node.js OpenSSL...");
const socket = tls.connect({
    host: 'smtp.gmail.com',
    port: 465,
    rejectUnauthorized: false
}, () => {
    console.log(">>> Node.js TLS CONNECTED SUCCESSFULLY! <<<");
    socket.setEncoding('utf8');
    socket.on('data', (data) => {
        console.log("Received data:", data);
        socket.end();
    });
});

socket.on('error', (err) => {
    console.error("Node.js TLS Error:", err.message);
});

setTimeout(() => {
    socket.destroy();
}, 6000);
