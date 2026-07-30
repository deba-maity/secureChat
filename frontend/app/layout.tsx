import type { Metadata, Viewport } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Privac Secure Chat",
  description: "Privacy-first secure chat with temporary conversations and encrypted favorites",
  appleWebApp: {
    capable: true,
    title: "Privac"
  }
};

export const viewport: Viewport = {
  themeColor: "#129c9c",
  width: "device-width",
  initialScale: 1
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body>{children}</body>
    </html>
  );
}

