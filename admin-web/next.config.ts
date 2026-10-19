import type { NextConfig } from "next";

const adminApiInternalUrl =
  process.env.ADMIN_API_INTERNAL_URL ?? "http://localhost:8082";

const nextConfig: NextConfig = {
  async rewrites() {
    return [
      {
        source: "/admin-api/:path*",
        destination: `${adminApiInternalUrl}/admin-api/:path*`,
      },
    ];
  },
};

export default nextConfig;
