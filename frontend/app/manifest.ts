import type { MetadataRoute } from "next";

export default function manifest(): MetadataRoute.Manifest {
  return {
    name: "Privac Secure Chat",
    short_name: "Privac",
    description: "Secure temporary chats with encrypted favorite conversations.",
    start_url: "/",
    display: "standalone",
    background_color: "#101623",
    theme_color: "#14b8a6",
    icons: [
      {
        src: "/icon.svg",
        sizes: "any",
        type: "image/svg+xml"
      }
    ]
  };
}

