const net = require('net');

const client = net.connect(25, 'mx-biz.mail.am0.yahoodns.net', () => {
    console.log("Connected to mx-biz.mail.am0.yahoodns.net:25");
});

function send(cmd) {
    console.log("C:", cmd);
    client.write(cmd + "\r\n");
}

let step = 0;
client.on('data', (d) => {
    const resp = d.toString().trim();
    console.log("S:", resp);
    
    if (resp.startsWith("220") && step === 0) {
        step = 1;
        send("HELO qualtechedge.com");
    } else if (resp.startsWith("250") && step === 1) {
        step = 2;
        send("MAIL FROM:<nonisonani16@gmail.com>");
    } else if (resp.startsWith("250") && step === 2) {
        step = 3;
        send("RCPT TO:<jyoti.sonani@qualtechedge.com>");
    } else if (resp.startsWith("250") && step === 3) {
        console.log(">>> RECIPIENT ACCEPTED BY QUALTECH MX! <<<");
        send("QUIT");
    } else {
        console.log("Other response or reject:", resp);
        client.end();
    }
});

client.on('error', (err) => {
    console.error("Socket error:", err.message);
});
