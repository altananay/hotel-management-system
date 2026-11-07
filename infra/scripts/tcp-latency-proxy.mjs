import net from "node:net";

const listenPort = Number(process.env.LISTEN_PORT ?? 9100);
const targetHost = process.env.TARGET_HOST ?? "127.0.0.1";
const targetPort = Number(process.env.TARGET_PORT ?? 9000);
const delayMs = Number(process.env.DELAY_MS ?? 100);

if (!Number.isInteger(listenPort) || !Number.isInteger(targetPort) || !Number.isInteger(delayMs)) {
  console.error("LISTEN_PORT, TARGET_PORT and DELAY_MS must be integers.");
  process.exit(1);
}

function forward(source, destination) {
  let queue = Promise.resolve();
  source.on("data", (chunk) => {
    queue = queue.then(
      () =>
        new Promise((resolve) => {
          setTimeout(() => {
            if (!destination.destroyed) {
              destination.write(chunk);
            }
            resolve();
          }, delayMs);
        }),
    );
  });
}

const server = net.createServer((client) => {
  const upstream = net.connect(targetPort, targetHost);
  forward(client, upstream);
  forward(upstream, client);
  const close = () => {
    client.destroy();
    upstream.destroy();
  };
  client.on("error", close);
  upstream.on("error", close);
  client.on("close", close);
  upstream.on("close", close);
});

server.listen(listenPort, "127.0.0.1", () => {
  console.log(
    `TCP latency proxy listening on 127.0.0.1:${listenPort} -> ${targetHost}:${targetPort} (+${delayMs}ms per chunk)`,
  );
});
